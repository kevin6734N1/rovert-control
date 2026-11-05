package com.rovert.rovercontrol.model

/**
 * Estados posibles de la conexión con el rover.
 */
enum class ConnectionState {
    /** No hay WebSocket abierto. */
    DISCONNECTED,

    /** Intentando abrir el WebSocket de control. */
    CONNECTING,

    /** WebSocket de control abierto y operativo. */
    CONNECTED,

    /** Último intento fallido; se reintenta automáticamente. */
    ERROR;

    /** `true` sólo cuando el canal de control está operativo. */
    val isConnected: Boolean
        get() = this == CONNECTED
}
