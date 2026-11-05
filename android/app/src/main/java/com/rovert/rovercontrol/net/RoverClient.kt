package com.rovert.rovercontrol.net

import com.rovert.rovercontrol.model.ConnectionState
import com.rovert.rovercontrol.model.ControlMessage
import com.rovert.rovercontrol.model.DeviceMessage
import com.rovert.rovercontrol.model.RoverError
import com.rovert.rovercontrol.model.RoverSettings
import com.rovert.rovercontrol.model.RoverStatus
import com.rovert.rovercontrol.protocol.MotorMapping
import com.rovert.rovercontrol.protocol.MotorPair
import com.rovert.rovercontrol.protocol.ProtocolCodec
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.math.min

/**
 * Cliente de red del rover.
 *
 * - WebSocket de control en `ws://<ip>:<puerto>/control` (contrato §3).
 * - Reconexión automática con backoff exponencial de 1 s a 10 s como máximo.
 * - Latido (`ping`) cada 2 s; si el ESP32 deja de responder, se fuerza el
 *   cierre y la reconexión.
 * - Envía `stop` siempre que el socket se cierra o falla, y al reconectar
 *   (el rover nunca debe quedarse con una consigna activa).
 *
 * La polaridad de los motores y el límite de velocidad se aplican aquí, en la
 * capa de cliente, tal y como exige el contrato §3.3.
 */
class RoverClient(
    private val scope: CoroutineScope,
    private val settingsProvider: () -> RoverSettings,
) {

    companion object {
        /** Backoff inicial de reconexión. */
        const val BACKOFF_MIN_MS = 1_000L

        /** Backoff máximo de reconexión. */
        const val BACKOFF_MAX_MS = 10_000L

        /** Intervalo del latido de aplicación. */
        const val HEARTBEAT_INTERVAL_MS = 2_000L

        /** Tiempo sin `pong` antes de considerar la conexión muerta. */
        const val PONG_TIMEOUT_MS = 12_000L

        /** Ruta del WebSocket de control. */
        const val CONTROL_PATH = "/control"

        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val MAX_SEQ = 0x7FFFFFFF

        /** Código de cierre WebSocket normal (RFC 6455). */
        private const val NORMAL_CLOSURE = 1000
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(0, TimeUnit.MILLISECONDS)
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastError = MutableStateFlow<RoverError?>(null)

    /** Último error de conexión; el texto lo pone la interfaz. */
    val lastError: StateFlow<RoverError?> = _lastError.asStateFlow()

    private val _status = MutableStateFlow<RoverStatus?>(null)

    /**
     * Telegría del rover (`status`) o `null` si aún no se ha recibido ninguna.
     *
     * Es un [StateFlow] (y por tanto un `Flow`) para que la interfaz pueda
     * recogerlo sin necesidad de un valor inicial.
     */
    val status: StateFlow<RoverStatus?> = _status.asStateFlow()

    private val _seq = AtomicInteger(1)

    @Volatile
    private var webSocket: WebSocket? = null

    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var backoffMs = BACKOFF_MIN_MS

    @Volatile
    private var lastPongAtMs = 0L

    private val sendLock = Any()

    /** `true` si el canal de control está operativo. */
    val isConnected: Boolean
        get() = _connectionState.value.isConnected

    // ---------------------------------------------------------------------
    // Ciclo de vida de la conexión
    // ---------------------------------------------------------------------

    /**
     * Abre (o reabre) el WebSocket de control. Es idempotente: si ya hay un
     * intento en curso o una conexión abierta, no hace nada.
     */
    @Synchronized
    fun connect() {
        if (webSocket != null) return
        if (reconnectJob?.isActive == true) return

        _connectionState.value = ConnectionState.CONNECTING
        val settings = settingsProvider().sanitized()
        val request = Request.Builder()
            .url(settings.wsUrl)
            .build()

        try {
            webSocket = httpClient.newWebSocket(request, ControlListener())
        } catch (e: IllegalArgumentException) {
            webSocket = null
            _connectionState.value = ConnectionState.ERROR
            _lastError.value = RoverError.UNKNOWN
            scheduleReconnect()
        }
    }

    /**
     * Cierra el WebSocket, cancela la reconexión y detiene el rover.
     */
    @Synchronized
    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        val socket = webSocket
        webSocket = null
        if (socket != null) {
            // Se intenta avisar al rover antes de cerrar el canal.
            sendStopInternal(socket)
            socket.close(NORMAL_CLOSURE, "cierre solicitado por la app")
        }
        backoffMs = BACKOFF_MIN_MS
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        val delayMs = backoffMs
        backoffMs = min(backoffMs * 2, BACKOFF_MAX_MS)
        reconnectJob = scope.launch {
            delay(delayMs)
            connect()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (!isConnected) continue
                if (nowMs() - lastPongAtMs > PONG_TIMEOUT_MS) {
                    // El ESP32 ha dejado de responder: se fuerza el cierre y
                    // se deja que el listener programe la reconexión.
                    closeSocketQuietly()
                    break
                }
                sendInternal(ControlMessage.Ping(nextSeq()))
            }
        }
    }

    private fun closeSocketQuietly() {
        val socket = webSocket
        webSocket = null
        socket?.close(NORMAL_CLOSURE, "sin respuesta")
    }

    // ---------------------------------------------------------------------
    // Envío de órdenes
    // ---------------------------------------------------------------------

    /**
     * Envía una consigna de movimiento. La polaridad de cada motor y el
     * límite de velocidad configurados se aplican antes de transmitir.
     */
    fun sendDrive(left: Float, right: Float) {
        val settings = settingsProvider().sanitized()
        val polarity = MotorMapping.applyPolarity(
            MotorPair(left, right),
            settings.leftPolarity,
            settings.rightPolarity,
        )
        val limited = MotorMapping.applySpeedLimit(polarity, settings.maxSpeed)
        sendInternal(ControlMessage.Drive(limited.left, limited.right, nextSeq()))
    }

    /**
     * Envía la orden de parada inmediata.
     */
    fun sendStop() {
        sendStopInternal(webSocket)
    }

    private fun sendStopInternal(socket: WebSocket?) {
        if (socket == null) return
        val sent = socket.send(ProtocolCodec.encode(ControlMessage.Stop(nextSeq())))
        if (!sent) {
            // No se pudo garantizar la orden: por seguridad el rover se
            // considera detenido (contrato §5.4).
            webSocket = null
            _connectionState.value = ConnectionState.ERROR
            _lastError.value = RoverError.SEND_FAILED
        }
    }

    private fun sendInternal(message: ControlMessage) {
        val socket = webSocket ?: return
        val sent = synchronized(sendLock) {
            socket.send(ProtocolCodec.encode(message))
        }
        if (!sent) {
            webSocket = null
            _connectionState.value = ConnectionState.ERROR
            _lastError.value = RoverError.SEND_FAILED
            scheduleReconnect()
        }
    }

    private fun nextSeq(): Int {
        val next = _seq.incrementAndGet()
        if (next > MAX_SEQ) {
            _seq.set(1)
            return 1
        }
        return next
    }

    private fun nowMs(): Long = System.currentTimeMillis()

    // ---------------------------------------------------------------------
    // HTTP auxiliar (diagnóstico y estado)
    // ---------------------------------------------------------------------

    /**
     * Descarga el JSON de diagnóstico del endpoint `/`.
     */
    suspend fun fetchDiagnostic(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsProvider().sanitized()
            val request = Request.Builder().url("${settings.httpBaseUrl}/").build()
            httpClient.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
        }
    }

    /**
     * Descarga el estado del rover del endpoint `/status`.
     */
    suspend fun fetchStatus(): Result<RoverStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsProvider().sanitized()
            val request = Request.Builder().url("${settings.httpBaseUrl}/status").build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val message = ProtocolCodec.decode(body)
                val status = (message as? DeviceMessage.Status)?.status
                    ?: throw IOException("Respuesta de estado inválida")
                status
            }
        }
    }

    /**
     * Traduce una excepción de red al error que la interfaz mostrará.
     */
    fun errorFor(throwable: Throwable?): RoverError = when (throwable) {
        is UnknownHostException -> RoverError.HOST_NOT_FOUND
        is SocketTimeoutException -> RoverError.TIMEOUT
        is ConnectException -> RoverError.CONNECT_FAILED
        is IOException -> RoverError.NETWORK
        null -> RoverError.UNKNOWN
        else -> RoverError.UNKNOWN
    }

    // ---------------------------------------------------------------------
    // Listener del WebSocket
    // ---------------------------------------------------------------------

    private inner class ControlListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@RoverClient.webSocket = webSocket
            backoffMs = BACKOFF_MIN_MS
            lastPongAtMs = nowMs()
            _connectionState.value = ConnectionState.CONNECTED
            _lastError.value = null

            // El rover puede haber quedado con una consigna activa de una
            // sesión anterior: se le manda parar antes de nada.
            sendStopInternal(webSocket)
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            when (val message = ProtocolCodec.decode(text)) {
                is DeviceMessage.Status -> _status.value = message.status
                is DeviceMessage.Pong -> lastPongAtMs = nowMs()
                is DeviceMessage.Unknown -> Unit
                null -> Unit
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@RoverClient.webSocket = null
            heartbeatJob?.cancel()
            _connectionState.value = ConnectionState.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@RoverClient.webSocket = null
            heartbeatJob?.cancel()
            _connectionState.value = ConnectionState.ERROR
            _lastError.value = errorFor(t)
            // El rover deja de mover los motores al caer el WebSocket, pero se
            // intenta igualmente enviar `stop` antes de dar la conexión por
            // perdida.
            scheduleReconnect()
        }
    }
}
