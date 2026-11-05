package com.rovert.rovercontrol.model

/**
 * Errores de conexión que la capa de red puede comunicar a la interfaz.
 *
 * Se mantienen como datos (y no como texto) para que los mensajes en español
 * vivan en los recursos de la app y la capa de red no dependa de Android.
 */
enum class RoverError {
    /** No se resuelve el nombre o la IP del rover. */
    HOST_NOT_FOUND,

    /** El rover no ha respondido dentro del tiempo de espera. */
    TIMEOUT,

    /** No se ha podido establecer la conexión con el rover. */
    CONNECT_FAILED,

    /** Fallo de red genérico. */
    NETWORK,

    /** No se ha podido enviar la orden por el WebSocket. */
    SEND_FAILED,

    /** Error desconocido. */
    UNKNOWN,
}
