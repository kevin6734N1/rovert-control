package com.rovert.rovercontrol.video

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas del parser incremental del cuerpo `multipart/x-mixed-replace`.
 *
 * El parser es lógica pura (no depende de Android), así que se puede
 * comprobar en la JVM alimentándolo con trozos de un cuerpo MIME sintético.
 */
class MjpegParserTest {

    private val jpegOne = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02, 0x03)
    private val jpegTwo = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x04, 0x05)

    @Test
    fun `extrae los fotogramas de un cuerpo multipart completo`() {
        val body = multipartBody(jpegOne, jpegTwo)

        val frames = MjpegParser("frame").let { parser ->
            val collected = mutableListOf<ByteArray>()
            var offset = 0
            while (offset < body.size) {
                val chunk = minOf(5, body.size - offset)
                collected += parser.push(body.copyOfRange(offset, offset + chunk), chunk)
                offset += chunk
            }
            collected
        }

        assertEquals(2, frames.size)
        assertArrayEquals(jpegOne, frames[0])
        assertArrayEquals(jpegTwo, frames[1])
    }

    @Test
    fun `extrae los fotogramas aunque lleguen byte a byte`() {
        val body = multipartBody(jpegOne, jpegTwo)

        val frames = MjpegParser("frame").let { parser ->
            val collected = mutableListOf<ByteArray>()
            for (index in body.indices) {
                collected += parser.push(byteArrayOf(body[index]), 1)
            }
            collected
        }

        assertEquals(2, frames.size)
        assertArrayEquals(jpegOne, frames[0])
        assertArrayEquals(jpegTwo, frames[1])
    }

    @Test
    fun `usa la siguiente boundary cuando no hay content length`() {
        val out = ByteArrayOutputStream()
        out.write("--frame\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
        out.write(jpegOne)
        out.write("\r\n--frame\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
        out.write(jpegTwo)
        // Un stream real siempre llega la siguiente boundary: el último
        // fotograma se entrega cuando aparece la boundary que lo cierra.
        out.write("\r\n--frame\r\n".toByteArray())
        val body = out.toByteArray()

        val frames = MjpegParser("frame").let { parser ->
            val collected = mutableListOf<ByteArray>()
            var offset = 0
            while (offset < body.size) {
                val chunk = minOf(3, body.size - offset)
                collected += parser.push(body.copyOfRange(offset, offset + chunk), chunk)
                offset += chunk
            }
            collected
        }

        assertEquals(2, frames.size)
        assertArrayEquals(jpegOne, frames[0])
        assertArrayEquals(jpegTwo, frames[1])
    }

    @Test
    fun `no entrega fotogramas a medio`() {
        val headerOnly = (
            "--frame\r\nContent-Type: image/jpeg\r\n" +
                "Content-Length: ${jpegOne.size}\r\n\r\n"
            ).toByteArray()

        val parser = MjpegParser("frame")
        val frames = mutableListOf<ByteArray>()

        // Sólo llegan las cabeceras: aún no hay un fotograma completo.
        frames += parser.push(headerOnly, headerOnly.size)
        assertEquals(0, frames.size)

        // Al llegar los bytes del JPEG se entrega el fotograma entero.
        frames += parser.push(jpegOne, jpegOne.size)
        assertEquals(1, frames.size)
        assertArrayEquals(jpegOne, frames[0])
    }

    @Test
    fun `la boundary se lee de la cabecera content type`() {
        assertEquals("frame", MjpegStreamView.parseBoundary("multipart/x-mixed-replace; boundary=frame"))
        assertEquals("frame", MjpegStreamView.parseBoundary("multipart/x-mixed-replace; boundary=\"frame\""))
        assertEquals("abc", MjpegStreamView.parseBoundary("multipart/x-mixed-replace;boundary=abc"))
        assertEquals("frame", MjpegStreamView.parseBoundary(null))
        assertEquals("frame", MjpegStreamView.parseBoundary("multipart/x-mixed-replace"))
    }

    /**
     * Construye un cuerpo `multipart/x-mixed-replace` con los fotogramas dados.
     */
    private fun multipartBody(vararg frames: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (frame in frames) {
            out.write("--frame\r\n".toByteArray())
            out.write("Content-Type: image/jpeg\r\n".toByteArray())
            out.write("Content-Length: ${frame.size}\r\n".toByteArray())
            out.write("\r\n".toByteArray())
            out.write(frame)
            out.write("\r\n".toByteArray())
        }
        return out.toByteArray()
    }
}
