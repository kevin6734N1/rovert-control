package com.rovert.rovercontrol.protocol

import com.rovert.rovercontrol.model.ControlMessage
import com.rovert.rovercontrol.model.DeviceMessage
import com.rovert.rovercontrol.model.RoverStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del codec del canal de control.
 */
class ProtocolCodecTest {

    @Test
    fun `codifica drive con el formato exacto del contrato`() {
        val json = ProtocolCodec.encode(ControlMessage.Drive(1.0f, 1.0f, 42))

        assertEquals("""{"type":"drive","left":1.0,"right":1.0,"seq":42}""", json)
    }

    @Test
    fun `codifica drive con valores negativos y decimales`() {
        val json = ProtocolCodec.encode(ControlMessage.Drive(-1.0f, 0.5f, 7))

        assertEquals("""{"type":"drive","left":-1.0,"right":0.5,"seq":7}""", json)
    }

    @Test
    fun `codifica stop sin campos de motor`() {
        val json = ProtocolCodec.encode(ControlMessage.Stop(43))

        assertEquals("""{"type":"stop","seq":43}""", json)
        assertTrue(!json.contains("left"))
        assertTrue(!json.contains("right"))
    }

    @Test
    fun `codifica ping con el formato exacto del contrato`() {
        val json = ProtocolCodec.encode(ControlMessage.Ping(44))

        assertEquals("""{"type":"ping","seq":44}""", json)
    }

    @Test
    fun `decodifica un mensaje de estado del rover`() {
        val raw = """{"type":"status","state":"stopped","left":0.0,"right":0.0,"seq":42,"uptimeMs":123456}"""

        val decoded = ProtocolCodec.decode(raw)

        assertTrue(decoded is DeviceMessage.Status)
        val status = (decoded as DeviceMessage.Status).status
        assertEquals(RoverStatus.STATE_STOPPED, status.state)
        assertEquals(0.0f, status.left, 0.0001f)
        assertEquals(0.0f, status.right, 0.0001f)
        assertEquals(42, status.seq)
        assertEquals(123456L, status.uptimeMs)
        assertTrue(!status.isDriving)
    }

    @Test
    fun `decodifica un estado de marcha`() {
        val raw = """{"type":"status","state":"driving","left":1.0,"right":-1.0,"seq":9,"uptimeMs":5000}"""

        val decoded = ProtocolCodec.decode(raw) as DeviceMessage.Status

        assertTrue(decoded.status.isDriving)
        assertEquals(1.0f, decoded.status.left, 0.0001f)
        assertEquals(-1.0f, decoded.status.right, 0.0001f)
    }

    @Test
    fun `decodifica un pong`() {
        val decoded = ProtocolCodec.decode("""{"type":"pong","seq":44}""")

        assertTrue(decoded is DeviceMessage.Pong)
        assertEquals(44, (decoded as DeviceMessage.Pong).seq)
    }

    @Test
    fun `decodifica un tipo desconocido como desconocido`() {
        val decoded = ProtocolCodec.decode("""{"type":"inventado","seq":1}""")

        assertTrue(decoded is DeviceMessage.Unknown)
    }

    @Test
    fun `el json mal formado devuelve null sin lanzar excepciones`() {
        val malformed = listOf(
            "",
            "   ",
            "no es json",
            "{",
            "{\"type\":",
            "[1,2,3]",
            "null",
            "{\"type\" \"drive\"}",
        )

        for (raw in malformed) {
            val decoded = ProtocolCodec.decode(raw)
            assertNull("La entrada '$raw' no debería decodificarse", decoded)
        }
    }

    @Test
    fun `un json con comas sobrantes no lanza excepciones`() {
        // Algunas implementaciones de JSON aceptan comas sobrantes y otras no:
        // lo importante es que la app no se caiga con ninguna de las dos.
        val decoded = ProtocolCodec.decode("""{"type":"drive",}""")

        assertTrue(decoded == null || decoded is DeviceMessage.Unknown)
    }

    @Test
    fun `decodificar null o vacio no lanza excepciones`() {
        assertNull(ProtocolCodec.decode(null))
        assertNull(ProtocolCodec.decode(""))
        assertNull(ProtocolCodec.decode("   "))
    }

    @Test
    fun `un json valido con tipos rarity no lanza excepciones`() {
        // Un "type" numérico no es un tipo conocido: se devuelve como desconocido.
        assertTrue(ProtocolCodec.decode("""{"type":123}""") is DeviceMessage.Unknown)

        // Campos de estado con tipos inválidos: se usan los valores por defecto.
        val status = ProtocolCodec.decode("""{"type":"status","left":"abc","uptimeMs":"mucho"}""")
        assertTrue(status is DeviceMessage.Status)
        val roverStatus = (status as DeviceMessage.Status).status
        assertEquals(0.0f, roverStatus.left, 0.0001f)
        assertEquals(0L, roverStatus.uptimeMs)
    }

    @Test
    fun `un mensaje de la app se decodifica como desconocido`() {
        val json = ProtocolCodec.encode(ControlMessage.Drive(0.25f, -0.75f, 11))

        val decoded = ProtocolCodec.decode(json)

        assertNotNull(decoded)
        assertTrue(decoded is DeviceMessage.Unknown)
    }

    @Test
    fun `el estado con campos ausentes usa valores por defecto`() {
        val decoded = ProtocolCodec.decode("""{"type":"status"}""") as DeviceMessage.Status

        assertEquals(RoverStatus.STATE_STOPPED, decoded.status.state)
        assertEquals(0.0f, decoded.status.left, 0.0001f)
        assertEquals(0, decoded.status.seq)
        assertEquals(0L, decoded.status.uptimeMs)
    }

    @Test
    fun `los numeros se formatean con el formato del contrato`() {
        assertEquals("0.0", ProtocolCodec.formatNumber(0.0f))
        assertEquals("1.0", ProtocolCodec.formatNumber(1.0f))
        assertEquals("-1.0", ProtocolCodec.formatNumber(-1.0f))
        assertEquals("0.5", ProtocolCodec.formatNumber(0.5f))
        assertEquals("0.25", ProtocolCodec.formatNumber(0.25f))
    }
}
