package com.rovert.rovercontrol.model

/**
 * Acciones lógicas de dirección del rover.
 *
 * Los valores por motor de cada acción están definidos en el contrato
 * (Docs/04-Protocolo-Comandos.md, §3.3) y se calculan en
 * [com.rovert.rovercontrol.protocol.MotorMapping].
 */
enum class DriveCommand {
    /** Adelante: ambos motores hacia delante. */
    FORWARD,

    /** Retroceso: ambos motors hacia atrás. */
    BACKWARD,

    /** Giro a la izquierda sobre el sitio: motor izquierdo atrás, derecho adelante. */
    LEFT,

    /** Giro a la derecha sobre el sitio: motor izquierdo adelante, derecho atrás. */
    RIGHT,

    /** Parada total. */
    STOP;

    /** `true` para cualquier orden que implique movimiento. */
    val isMovement: Boolean
        get() = this != STOP

    /**
     * `true` si [other] es la dirección opuesta a esta. Mantener dos direcciones
     * contradictorias a la vez equivale a STOP (regla de seguridad del contrato §5.2).
     */
    fun contradicts(other: DriveCommand): Boolean = when {
        this == STOP || other == STOP -> false
        this == FORWARD && other == BACKWARD -> true
        this == BACKWARD && other == FORWARD -> true
        this == LEFT && other == RIGHT -> true
        this == RIGHT && other == LEFT -> true
        else -> false
    }
}
