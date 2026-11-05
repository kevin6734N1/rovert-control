package com.rovert.rovercontrol.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Estados posibles del stream de vídeo.
 */
sealed class MjpegState {
    /** El stream está detenido. */
    object IDLE : MjpegState()

    /** Se está abriendo la conexión HTTP con la cámara. */
    object CONNECTING : MjpegState()

    /** Se están decodificando fotogramas correctamente. */
    object STREAMING : MjpegState()

    /** Error con mensaje comprensible en español. */
    data class ERROR(val message: String) : MjpegState()
}

/**
 * Vista que reproduce el stream MJPEG del ESP32 (`GET /stream`).
 *
 * El cuerpo de la respuesta es `multipart/x-mixed-replace`: cada fotograma
 * llega precedido por sus cabeceras boundary y delimitado por
 * `Content-Length` (contrato §2.1). Este parser consume el flujo de forma
 * incremental, decodifica cada JPEG fuera del hilo principal y lo dibuja
 * centrado y ajustado al formato.
 */
class MjpegStreamView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val _state = MutableStateFlow<MjpegState>(MjpegState.IDLE)

    /** Estado actual del stream. */
    val state: StateFlow<MjpegState> = _state.asStateFlow()

    private val _fps = MutableStateFlow(0f)

    /** Fotogramas por segundo medidos sobre el último segundo. */
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val running = AtomicBoolean(false)

    @Volatile
    private var call: Call? = null

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var frame: Bitmap? = null

    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8FA3B8")
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#05080C")
    }

    private var frameCounter = 0
    private var fpsWindowStart = 0L

    init {
        setBackgroundColor(Color.parseColor("#05080C"))
    }

    /**
     * Abre el stream de vídeo contra el rover indicado.
     */
    @Synchronized
    fun start(host: String, port: Int) {
        stop()
        running.set(true)
        _state.value = MjpegState.CONNECTING
        frameCounter = 0
        fpsWindowStart = System.currentTimeMillis()

        val thread = Thread({ runStream(host, port) }, "mjpeg-stream")
        thread.isDaemon = true
        worker = thread
        thread.start()
    }

    /**
     * Detiene el stream y cancela la petición HTTP en curso.
     */
    @Synchronized
    fun stop() {
        running.set(false)
        call?.cancel()
        call = null
        worker?.interrupt()
        worker = null
        if (_state.value !is MjpegState.ERROR) {
            _state.value = MjpegState.IDLE
        }
        _fps.value = 0f
    }

    /** Reinicia el stream (usado por el botón «reintentar»). */
    fun restart(host: String, port: Int) = start(host, port)

    private fun runStream(host: String, port: Int) {
        val url = "http://$host:$port/stream"
        val request = Request.Builder().url(url).build()
        try {
            val newCall = httpClient.newCall(request)
            call = newCall
            newCall.execute().use { response ->
                if (!response.isSuccessful) {
                    fail("El stream de vídeo respondió con el código ${response.code}.")
                    return
                }
                val body = response.body
                if (body == null) {
                    fail("El rover no devolvió imagen.")
                    return
                }
                val boundary = parseBoundary(response.header("Content-Type"))
                body.byteStream().use { input ->
                    parseStream(input, boundary)
                }
            }
            if (running.get()) {
                fail("El stream de vídeo se cerró inesperadamente.")
            }
        } catch (e: Exception) {
            if (running.get()) {
                fail(friendlyStreamError(e))
            }
        } finally {
            call = null
        }
    }

    /**
     * Consume el flujo multipart y entrega cada fotograma JPEG completo.
     */
    private fun parseStream(input: InputStream, boundary: String) {
        val parser = MjpegParser(boundary)
        val readBuffer = ByteArray(16 * 1024)

        while (running.get()) {
            val read = input.read(readBuffer)
            if (read < 0) break
            if (read == 0) continue

            val frames = parser.push(readBuffer, read)
            for (jpeg in frames) {
                if (!running.get()) return
                val bitmap = decodeFrame(jpeg) ?: continue
                post {
                    if (!running.get()) return@post
                    frame = bitmap
                    invalidate()
                }
                countFrame()
            }
        }
    }

    /**
     * Decodifica el JPEG fuera del hilo principal.
     */
    private fun decodeFrame(jpeg: ByteArray): Bitmap? = try {
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
    } catch (e: OutOfMemoryError) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun countFrame() {
        frameCounter++
        val now = System.currentTimeMillis()
        val elapsed = now - fpsWindowStart
        if (elapsed >= 1000L) {
            _fps.value = frameCounter * 1000f / elapsed
            frameCounter = 0
            fpsWindowStart = now
        }
    }

    private fun fail(message: String) {
        if (running.get()) {
            _state.value = MjpegState.ERROR(message)
        }
    }

    private fun friendlyStreamError(e: Exception): String = when (e) {
        is java.net.UnknownHostException -> "No se encuentra el rover en la red."
        is java.net.SocketTimeoutException -> "El rover no responde al stream de vídeo."
        is java.net.ConnectException -> "No se pudo abrir el stream de vídeo del rover."
        is java.io.IOException -> "Error de red en el stream de vídeo."
        else -> "No se pudo reproducir el stream de vídeo."
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val current = frame
        if (current == null || current.isRecycled) {
            val message = when (val s = _state.value) {
                is MjpegState.CONNECTING -> "Conectando con la cámara…"
                is MjpegState.ERROR -> s.message
                else -> "Sin señal de vídeo"
            }
            canvas.drawText(
                message,
                width / 2f,
                height / 2f,
                placeholderPaint,
            )
            return
        }

        val target = fitRect(current.width, current.height)
        canvas.drawBitmap(current, null, target, null)
    }

    /**
     * Calcula el rectángulo centrado con el formato original de la imagen.
     */
    private fun fitRect(bitmapWidth: Int, bitmapHeight: Int): RectF {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || width == 0 || height == 0) {
            return RectF(0f, 0f, width.toFloat(), height.toFloat())
        }
        val scale = minOf(width.toFloat() / bitmapWidth, height.toFloat() / bitmapHeight)
        val drawWidth = bitmapWidth * scale
        val drawHeight = bitmapHeight * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f
        return RectF(left, top, left + drawWidth, top + drawHeight)
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    companion object {
        /**
         * Extrae el valor de `boundary` de la cabecera `Content-Type`.
         */
        fun parseBoundary(contentType: String?): String {
            if (contentType.isNullOrBlank()) return "frame"
            val marker = "boundary="
            val index = contentType.indexOf(marker, ignoreCase = true)
            if (index < 0) return "frame"
            var value = contentType.substring(index + marker.length).trim()
            if (value.startsWith("\"")) value = value.removePrefix("\"")
            if (value.endsWith("\"")) value = value.removeSuffix("\"")
            return value.ifEmpty { "frame" }
        }
    }
}

/**
 * Parser incremental del cuerpo `multipart/x-mixed-replace`.
 *
 * Busca la cabecera de cada parte, lee el `Content-Length` y extrae el
 * fotograma JPEG completo. Si el firmware no envía `Content-Length`, se usa
 * la siguiente boundary como delimitador.
 */
internal class MjpegParser(boundary: String) {

    private val boundaryBytes = "--$boundary".toByteArray(Charsets.US_ASCII)
    private val headerEnd = "\r\n\r\n".toByteArray(Charsets.US_ASCII)

    private val buffer = StreamBuffer()
    private var phase = Phase.BOUNDARY
    private var contentLength = -1
    private val headers = StringBuilder()

    private enum class Phase { BOUNDARY, HEADERS, BODY, TRAILER }

    /**
     * Introduce nuevos bytes en el parser y devuelve los fotogramas completos.
     */
    fun push(data: ByteArray, length: Int): List<ByteArray> {
        buffer.append(data, length)
        val frames = mutableListOf<ByteArray>()

        var progressed = true
        while (progressed) {
            progressed = false
            when (phase) {
                Phase.BOUNDARY -> {
                    val index = buffer.indexOf(boundaryBytes)
                    if (index >= 0) {
                        buffer.consume(index + boundaryBytes.size)
                        headers.setLength(0)
                        contentLength = -1
                        phase = Phase.HEADERS
                        progressed = true
                    }
                }

                Phase.HEADERS -> {
                    val index = buffer.indexOf(headerEnd)
                    if (index >= 0) {
                        val block = String(buffer.peek(index), Charsets.ISO_8859_1)
                        headers.append(block).append("\r\n")
                        contentLength = parseContentLength(headers.toString())
                        buffer.consume(index + headerEnd.size)
                        phase = if (contentLength > 0) Phase.BODY else Phase.BOUNDARY
                        progressed = true
                    }
                }

                Phase.BODY -> {
                    if (contentLength > 0) {
                        if (buffer.available() >= contentLength) {
                            frames.add(buffer.read(contentLength))
                            phase = Phase.TRAILER
                            progressed = true
                        }
                    } else {
                        // Sin Content-Length: el fotograma termina en la
                        // siguiente boundary.
                        val index = buffer.indexOf(boundaryBytes)
                        if (index >= 0) {
                            if (index > 0) {
                                frames.add(buffer.read(index))
                            }
                            phase = Phase.TRAILER
                            progressed = true
                        }
                    }
                }

                Phase.TRAILER -> {
                    // Tras el fotograma llega la siguiente boundary.
                    val index = buffer.indexOf(boundaryBytes)
                    if (index >= 0) {
                        buffer.consume(index + boundaryBytes.size)
                        headers.setLength(0)
                        contentLength = -1
                        phase = Phase.HEADERS
                        progressed = true
                    }
                }
            }
        }
        return frames
    }

    /**
     * Extrae el valor de la cabecera `Content-Length`.
     */
    private fun parseContentLength(block: String): Int {
        for (line in block.split("\r\n")) {
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            val name = line.substring(0, separator).trim()
            if (!name.equals("Content-Length", ignoreCase = true)) continue
            return line.substring(separator + 1).trim().toIntOrNull() ?: -1
        }
        return -1
    }
}

/**
 * Buffer circular sencillo para consumir bytes de forma incremental.
 */
internal class StreamBuffer {
    private var data = ByteArray(32 * 1024)
    private var size = 0

    fun available(): Int = size

    fun append(source: ByteArray, length: Int) {
        if (length <= 0) return
        if (size + length > data.size) {
            var capacity = data.size
            while (capacity < size + length) capacity *= 2
            data = data.copyOf(capacity)
        }
        System.arraycopy(source, 0, data, size, length)
        size += length
    }

    fun peek(length: Int): ByteArray {
        val safe = length.coerceAtMost(size)
        return data.copyOfRange(0, safe)
    }

    fun read(length: Int): ByteArray {
        val safe = length.coerceIn(0, size)
        val out = data.copyOfRange(0, safe)
        consume(safe)
        return out
    }

    fun consume(length: Int) {
        val safe = length.coerceIn(0, size)
        System.arraycopy(data, safe, data, 0, size - safe)
        size -= safe
    }

    /**
     * Busca una secuencia de bytes y devuelve su posición, o -1.
     */
    fun indexOf(pattern: ByteArray): Int {
        if (pattern.isEmpty() || size < pattern.size) return -1
        val last = size - pattern.size
        for (start in 0..last) {
            var matched = true
            for (offset in pattern.indices) {
                if (data[start + offset] != pattern[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return start
        }
        return -1
    }
}
