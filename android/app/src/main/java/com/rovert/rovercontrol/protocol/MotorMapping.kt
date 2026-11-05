package com.rovert.rovercontrol.protocol

import com.rovert.rovercontrol.model.DriveCommand
import kotlin.math.abs
import kotlin.math.max

/**
 * Par de consignas normalizadas para los dos motores, en el rango -1.0 … 1.0.
 * Signo `+` = avance (contrato §3.1).
 */
data class MotorPair(val left: Float, val right: Float) {

    /** `true` cuando ambos motores están a cero. */
    val isStop: Boolean
        get() = left == 0f && right == 0f

    /** Escala ambas consignas por un factor. */
    fun scaled(factor: Float): MotorPair = MotorPair(left * factor, right * factor)

    /** Aplica la polaridad configurada para cada motor. */
    fun withSigns(leftSign: Int, rightSign: Int): MotorPair =
        MotorPair(left * sign(leftSign), right * sign(rightSign))

    companion object {
        /** Par de consignas nulo: STOP. */
        val STOP = MotorPair(0f, 0f)

        /** Convierte un valor de polaridad a +1 / -1. */
        fun sign(polarity: Int): Float = if (polarity < 0) -1f else 1f
    }
}

/**
 * Conversión de órdenes lógicas y ejes del mando a consignas de motor.
 *
 * Todas las funciones son puras y no dependen de Android, de modo que la
 * tabla del contrato (contrato §3.3) se puede verificar con tests unitarios.
 */
object MotorMapping {

    /**
     * Traduce una acción lógica a la tabla de consignas del contrato:
     *
     * | Acción      | left | right |
     * |-------------|------|-------|
     * | Adelante    | +1.0 | +1.0  |
     * | Retroceso   | -1.0 | -1.0  |
     * | Giro izq.   | -1.0 | +1.0  |
     * | Giro der.   | +1.0 | -1.0  |
     * | STOP        |  0.0 |  0.0  |
     */
    fun fromCommand(command: DriveCommand): MotorPair = when (command) {
        DriveCommand.FORWARD -> MotorPair(1f, 1f)
        DriveCommand.BACKWARD -> MotorPair(-1f, -1f)
        DriveCommand.LEFT -> MotorPair(-1f, 1f)
        DriveCommand.RIGHT -> MotorPair(1f, -1f)
        DriveCommand.STOP -> MotorPair.STOP
    }

    /**
     * Traduce un conjunto de órdenes simultáneas (varios dedos o ejes a la vez).
     *
     * - Si no hay órdenes de movimiento, devuelve STOP.
     * - Si hay direcciones contradictorias (adelante/retroceso o
     *   izquierda/derecha), devuelve STOP: nunca deben coexistir dos
     *   direcciones opuestas (contrato §5.2).
     * - Si hay dos direcciones compatibles (diagonal), promedia las
     *   consignas de cada motor.
     */
    fun fromCommands(commands: Set<DriveCommand>): MotorPair {
        val moves = commands.filter { it.isMovement }.toSet()
        if (moves.isEmpty()) return MotorPair.STOP

        val contradictory = moves.any { a ->
            moves.any { b -> b != a && a.contradicts(b) }
        }
        if (contradictory) return MotorPair.STOP

        if (moves.size == 1) return fromCommand(moves.first())

        var left = 0f
        var right = 0f
        for (command in moves) {
            val pair = fromCommand(command)
            left += pair.left
            right += pair.right
        }
        val count = moves.size.toFloat()
        return MotorPair(clamp(left / count), clamp(right / count))
    }

    /**
     * Aplica la polaridad configurada a cada motor.
     */
    fun applyPolarity(pair: MotorPair, leftSign: Int, rightSign: Int): MotorPair =
        pair.withSigns(leftSign, rightSign)

    /**
     * Aplica el límite de velocidad configurado (0.1 … 1.0).
     */
    fun applySpeedLimit(pair: MotorPair, maxSpeed: Float): MotorPair {
        val limit = maxSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
        return MotorPair(clamp(pair.left * limit), clamp(pair.right * limit))
    }

    /**
     * Convierte los ejes del stick izquierdo a consignas de motor.
     *
     * @param x eje horizontal (-1.0 a la izquierda … +1.0 a la derecha).
     * @param y eje vertical (+1.0 hacia abajo … -1.0 hacia arriba).
     * @param deadZone zona muerta; por debajo de ella el eje se considera neutro.
     *
     * El stick izquierdo se traduce con mezcla de gas y dirección:
     * `left = gas + giro`, `right = gas - giro`, normalizando para que el
     * resultado nunca supere el rango ±1.0. En las cuatro posiciones
     * cardinales el resultado coincide exactamente con la tabla del contrato.
     */
    fun fromAxes(x: Float, y: Float, deadZone: Float): MotorPair {
        val zone = deadZone.coerceIn(MIN_DEAD_ZONE, MAX_DEAD_ZONE)
        val stickX = applyDeadZone(x, zone)
        val stickY = applyDeadZone(y, zone)

        val throttle = -stickY // en Android el stick hacia arriba es negativo
        val steering = stickX

        if (throttle == 0f && steering == 0f) return MotorPair.STOP

        val rawLeft = throttle + steering
        val rawRight = throttle - steering
        val peak = max(abs(rawLeft), abs(rawRight))
        return if (peak > 1f) {
            MotorPair(clamp(rawLeft / peak), clamp(rawRight / peak))
        } else {
            MotorPair(clamp(rawLeft), clamp(rawRight))
        }
    }

    /**
     * Anula un valor de eje que esté dentro de la zona muerta.
     */
    fun applyDeadZone(value: Float, deadZone: Float): Float {
        val limited = value.coerceIn(-1f, 1f)
        val zone = deadZone.coerceIn(MIN_DEAD_ZONE, MAX_DEAD_ZONE)
        return if (abs(limited) <= zone) 0f else limited
    }

    /**
     * Recorta un valor al rango válido -1.0 … 1.0 (el firmware también recorta,
     * pero la app nunca debe enviar valores fuera de rango).
     */
    fun clamp(value: Float): Float = value.coerceIn(-1f, 1f)

    const val MIN_SPEED = 0.1f
    const val MAX_SPEED = 1.0f
    const val MIN_DEAD_ZONE = 0.0f
    const val MAX_DEAD_ZONE = 0.4f
}
