package com.rovert.rovercontrol.protocol

import com.rovert.rovercontrol.model.DriveCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la traducción de órdenes lógicas y ejes del stick a consignas
 * de motor (contrato §3.3).
 */
class MotorMappingTest {

    private val epsilon = 0.0001f

    @Test
    fun `adelante deja ambos motores a uno`() {
        val pair = MotorMapping.fromCommand(DriveCommand.FORWARD)

        assertEquals(1.0f, pair.left, epsilon)
        assertEquals(1.0f, pair.right, epsilon)
    }

    @Test
    fun `retroceso deja ambos motores a menos uno`() {
        val pair = MotorMapping.fromCommand(DriveCommand.BACKWARD)

        assertEquals(-1.0f, pair.left, epsilon)
        assertEquals(-1.0f, pair.right, epsilon)
    }

    @Test
    fun `giro a la izquierda invierte el motor izquierdo`() {
        val pair = MotorMapping.fromCommand(DriveCommand.LEFT)

        assertEquals(-1.0f, pair.left, epsilon)
        assertEquals(1.0f, pair.right, epsilon)
    }

    @Test
    fun `giro a la derecha invierte el motor derecho`() {
        val pair = MotorMapping.fromCommand(DriveCommand.RIGHT)

        assertEquals(1.0f, pair.left, epsilon)
        assertEquals(-1.0f, pair.right, epsilon)
    }

    @Test
    fun `stop deja ambos motores a cero`() {
        val pair = MotorMapping.fromCommand(DriveCommand.STOP)

        assertEquals(0.0f, pair.left, epsilon)
        assertEquals(0.0f, pair.right, epsilon)
        assertTrue(pair.isStop)
    }

    @Test
    fun `la polaridad invierte solo el motor indicado`() {
        val base = MotorMapping.fromCommand(DriveCommand.FORWARD)

        val rightInverted = MotorMapping.applyPolarity(base, leftSign = 1, rightSign = -1)
        assertEquals(1.0f, rightInverted.left, epsilon)
        assertEquals(-1.0f, rightInverted.right, epsilon)

        val leftInverted = MotorMapping.applyPolarity(base, leftSign = -1, rightSign = 1)
        assertEquals(-1.0f, leftInverted.left, epsilon)
        assertEquals(1.0f, leftInverted.right, epsilon)

        val bothInverted = MotorMapping.applyPolarity(base, leftSign = -1, rightSign = -1)
        assertEquals(-1.0f, bothInverted.left, epsilon)
        assertEquals(-1.0f, bothInverted.right, epsilon)

        val noneInverted = MotorMapping.applyPolarity(base, leftSign = 1, rightSign = 1)
        assertEquals(1.0f, noneInverted.left, epsilon)
        assertEquals(1.0f, noneInverted.right, epsilon)
    }

    @Test
    fun `la polaridad aplicada al giro a la derecha invierte el motor derecho`() {
        val base = MotorMapping.fromCommand(DriveCommand.RIGHT)

        val inverted = MotorMapping.applyPolarity(base, leftSign = 1, rightSign = -1)

        assertEquals(1.0f, inverted.left, epsilon)
        assertEquals(1.0f, inverted.right, epsilon)
    }

    @Test
    fun `el limite de velocidad escala las consignas`() {
        val base = MotorMapping.fromCommand(DriveCommand.FORWARD)

        val half = MotorMapping.applySpeedLimit(base, 0.5f)
        assertEquals(0.5f, half.left, epsilon)
        assertEquals(0.5f, half.right, epsilon)

        val full = MotorMapping.applySpeedLimit(base, 1.0f)
        assertEquals(1.0f, full.left, epsilon)
        assertEquals(1.0f, full.right, epsilon)

        val minimum = MotorMapping.applySpeedLimit(base, 0.1f)
        assertEquals(0.1f, minimum.left, epsilon)
        assertEquals(0.1f, minimum.right, epsilon)
    }

    @Test
    fun `el limite de velocidad se recorta a los rangos permitidos`() {
        val base = MotorMapping.fromCommand(DriveCommand.FORWARD)

        val tooLow = MotorMapping.applySpeedLimit(base, 0.0f)
        assertEquals(0.1f, tooLow.left, epsilon)

        val tooHigh = MotorMapping.applySpeedLimit(base, 5.0f)
        assertEquals(1.0f, tooHigh.left, epsilon)
    }

    @Test
    fun `la polaridad y la velocidad se aplican en el cliente`() {
        val base = MotorMapping.fromCommand(DriveCommand.FORWARD)

        val result = MotorMapping.applySpeedLimit(
            MotorMapping.applyPolarity(base, leftSign = 1, rightSign = -1),
            0.5f,
        )

        assertEquals(0.5f, result.left, epsilon)
        assertEquals(-0.5f, result.right, epsilon)
    }

    @Test
    fun `la zona muerta descarta valores pequenos del stick`() {
        val pair = MotorMapping.fromAxes(x = 0.1f, y = 0.1f, deadZone = 0.18f)

        assertEquals(0.0f, pair.left, epsilon)
        assertEquals(0.0f, pair.right, epsilon)
        assertTrue(pair.isStop)
    }

    @Test
    fun `la zona muerta descarta un solo eje`() {
        // El eje X está dentro de la zona neutra: sólo cuenta el gas.
        val pair = MotorMapping.fromAxes(x = 0.1f, y = -0.9f, deadZone = 0.18f)

        assertEquals(0.9f, pair.left, epsilon)
        assertEquals(0.9f, pair.right, epsilon)
    }

    @Test
    fun `los ejes del stick reproducen la tabla del contrato`() {
        // Stick arriba: adelante.
        val forward = MotorMapping.fromAxes(x = 0.0f, y = -1.0f, deadZone = 0.18f)
        assertEquals(1.0f, forward.left, epsilon)
        assertEquals(1.0f, forward.right, epsilon)

        // Stick abajo: retroceso.
        val backward = MotorMapping.fromAxes(x = 0.0f, y = 1.0f, deadZone = 0.18f)
        assertEquals(-1.0f, backward.left, epsilon)
        assertEquals(-1.0f, backward.right, epsilon)

        // Stick izquierda: giro a la izquierda sobre el sitio.
        val left = MotorMapping.fromAxes(x = -1.0f, y = 0.0f, deadZone = 0.18f)
        assertEquals(-1.0f, left.left, epsilon)
        assertEquals(1.0f, left.right, epsilon)

        // Stick derecha: giro a la derecha sobre el sitio.
        val right = MotorMapping.fromAxes(x = 1.0f, y = 0.0f, deadZone = 0.18f)
        assertEquals(1.0f, right.left, epsilon)
        assertEquals(-1.0f, right.right, epsilon)
    }

    @Test
    fun `las consignas de los ejes nunca salen del rango permitido`() {
        val diagonal = MotorMapping.fromAxes(x = 1.0f, y = -1.0f, deadZone = 0.18f)

        assertTrue(diagonal.left in -1.0f..1.0f)
        assertTrue(diagonal.right in -1.0f..1.0f)
    }

    @Test
    fun `dos direcciones contradictorias se resuelven a stop`() {
        val pair = MotorMapping.fromCommands(setOf(DriveCommand.FORWARD, DriveCommand.BACKWARD))

        assertEquals(0.0f, pair.left, epsilon)
        assertEquals(0.0f, pair.right, epsilon)
        assertTrue(pair.isStop)
    }

    @Test
    fun `girar a izquierda y a la derecha a la vez se resuelve a stop`() {
        val pair = MotorMapping.fromCommands(setOf(DriveCommand.LEFT, DriveCommand.RIGHT))

        assertTrue(pair.isStop)
    }

    @Test
    fun `un conjunto vacio se resuelve a stop`() {
        assertTrue(MotorMapping.fromCommands(emptySet()).isStop)
    }

    @Test
    fun `una diagonal combina ambas consignas`() {
        val pair = MotorMapping.fromCommands(setOf(DriveCommand.FORWARD, DriveCommand.LEFT))

        assertEquals(0.0f, pair.left, epsilon)
        assertEquals(1.0f, pair.right, epsilon)
    }
}
