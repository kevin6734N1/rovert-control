package com.rovert.rovercontrol.model

/**
 * Configuración persistente de la app.
 *
 * Todos los valores tienen un origen por defecto coherente con el contrato:
 * IP del SoftAP `192.168.4.1`, puerto 80 y red `Rovert-Cam`.
 */
data class RoverSettings(
    /** Dirección IP o hostname del rover. */
    val host: String = DEFAULT_HOST,
    /** Puerto HTTP/WS del rover. */
    val port: Int = DEFAULT_PORT,
    /** SSID de la red SoftAP del rover (solo informativo: el emparejamiento Wi-Fi es del sistema). */
    val ssidHint: String = DEFAULT_SSID,
    /** Límite de velocidad global, en el rango 0.1 … 1.0. */
    val maxSpeed: Float = DEFAULT_MAX_SPEED,
    /** Polaridad del motor izquierdo: +1 normal, -1 invertido. */
    val leftPolarity: Int = 1,
    /** Polaridad del motor derecho: +1 normal, -1 invertido. */
    val rightPolarity: Int = 1,
    /** Zona muerta del stick analógico, en el rango 0.0 … 0.4. */
    val deadZone: Float = DEFAULT_DEAD_ZONE,
    /** Si es `true`, la app intenta conectar al abrirse. */
    val autoConnect: Boolean = true,
) {

    /** URL base HTTP del rover. */
    val httpBaseUrl: String
        get() = "http://$host:$port"

    /** URL del WebSocket de control. */
    val wsUrl: String
        get() = "ws://$host:$port/control"

    /**
     * Devuelve una copia con todos los valores dentro de los rangos permitidos.
     * Se usa antes de persistir o aplicar ajustes para que ningún valor
     * corrupto pueda provocar una excepción en tiempo de ejecución.
     */
    fun sanitized(): RoverSettings = copy(
        host = host.trim().ifEmpty { DEFAULT_HOST },
        port = port.coerceIn(MIN_PORT, MAX_PORT),
        ssidHint = ssidHint.trim(),
        maxSpeed = maxSpeed.coerceIn(MIN_SPEED, MAX_SPEED),
        leftPolarity = if (leftPolarity < 0) -1 else 1,
        rightPolarity = if (rightPolarity < 0) -1 else 1,
        deadZone = deadZone.coerceIn(MIN_DEAD_ZONE, MAX_DEAD_ZONE),
    )

    companion object {
        const val DEFAULT_HOST = "192.168.4.1"
        const val DEFAULT_PORT = 80
        const val DEFAULT_SSID = "Rovert-Cam"

        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 1.0f
        const val DEFAULT_MAX_SPEED = 1.0f

        const val MIN_DEAD_ZONE = 0.0f
        const val MAX_DEAD_ZONE = 0.4f
        const val DEFAULT_DEAD_ZONE = 0.18f

        const val MIN_PORT = 1
        const val MAX_PORT = 65535
    }
}
