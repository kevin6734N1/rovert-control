package com.rovert.rovercontrol.model

/**
 * Mensajes que la app envía al ESP32 por el WebSocket `/control`.
 *
 * Formato (contrato §3.1):
 * ```json
 * {"type":"drive","left":0.0,"right":0.0,"seq":42}
 * {"type":"stop","seq":43}
 * {"type":"ping","seq":44}
 * ```
 */
sealed class ControlMessage {

    /** Consigna de movimiento para ambos motores. Valores en el rango -1.0 … 1.0. */
    data class Drive(
        val left: Float,
        val right: Float,
        val seq: Int,
    ) : ControlMessage()

    /** Parada inmediata. No lleva `left`/`right`. */
    data class Stop(val seq: Int) : ControlMessage()

    /** Latido para comprobar que la conexión sigue viva. */
    data class Ping(val seq: Int) : ControlMessage()
}

/**
 * Mensajes que el ESP32 envía a la app (contrato §3.2).
 */
sealed class DeviceMessage {

    /** Telegetría de motores y uptime. */
    data class Status(val status: RoverStatus) : DeviceMessage()

    /** Respuesta a un `ping`. */
    data class Pong(val seq: Int) : DeviceMessage()

    /** Mensaje reconocido pero no interpretado (o de una versión futura del firmware). */
    data class Unknown(val raw: String) : DeviceMessage()
}

/**
 * Estado que reporta el ESP32.
 */
data class RoverStatus(
    /** `stopped` o `driving`. */
    val state: String,
    /** Consigna real del motor izquierdo en milésimas, según el firmware. */
    val left: Float,
    /** Consigna real del motor derecho en milésimas, según el firmware. */
    val right: Float,
    /** Último número de secuencia recibido. */
    val seq: Int,
    /** Tiempo de funcionamiento del ESP32 en milisegundos. */
    val uptimeMs: Long,
) {
    /** `true` cuando el rover informa que hay una consigna de movimiento activa. */
    val isDriving: Boolean
        get() = state.equals(STATE_DRIVING, ignoreCase = true)

    /** Uptime expresado en segundos. */
    val uptimeSeconds: Long
        get() = uptimeMs / 1000L

    companion object {
        const val STATE_STOPPED = "stopped"
        const val STATE_DRIVING = "driving"
    }
}
