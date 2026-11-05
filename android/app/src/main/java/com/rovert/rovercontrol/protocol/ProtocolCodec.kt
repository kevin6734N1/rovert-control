package com.rovert.rovercontrol.protocol

import com.rovert.rovercontrol.model.ControlMessage
import com.rovert.rovercontrol.model.DeviceMessage
import com.rovert.rovercontrol.model.RoverStatus
import org.json.JSONException
import org.json.JSONObject

/**
 * Codec del canal de control WebSocket `/control`.
 *
 * Es lógica pura (sin dependencias de Android) para poder probarla en la JVM.
 *
 * La **codificación** construye un [JSONObject] con `org.json` y lo serializa
 * respetando el orden de claves del contrato (`type`, `left`, `right`, `seq`),
 * de forma que el texto generado es siempre idéntico byte a byte:
 * ```json
 * {"type":"drive","left":1.0,"right":1.0,"seq":42}
 * {"type":"stop","seq":43}
 * {"type":"ping","seq":44}
 * ```
 * (El orden de iteración de `JSONObject` no está garantizado por la
 * especificación, de ahí el serializador determinista.)
 *
 * La **decodificación** usa `org.json.JSONObject` y nunca propaga excepciones:
 * un mensaje mal formado devuelve `null` en lugar de romper la app.
 */
object ProtocolCodec {

    const val TYPE_DRIVE = "drive"
    const val TYPE_STOP = "stop"
    const val TYPE_PING = "ping"
    const val TYPE_STATUS = "status"
    const val TYPE_PONG = "pong"

    /** Orden de claves exigido por el contrato para cada tipo de mensaje. */
    private val DRIVE_KEYS = listOf("type", "left", "right", "seq")
    private val SIMPLE_KEYS = listOf("type", "seq")

    /**
     * Serializa un mensaje de la app al formato JSON del contrato.
     */
    fun encode(message: ControlMessage): String {
        val json = when (message) {
            is ControlMessage.Drive -> JSONObject()
                .put("type", TYPE_DRIVE)
                .put("left", message.left.toDouble())
                .put("right", message.right.toDouble())
                .put("seq", message.seq)

            is ControlMessage.Stop -> JSONObject()
                .put("type", TYPE_STOP)
                .put("seq", message.seq)

            is ControlMessage.Ping -> JSONObject()
                .put("type", TYPE_PING)
                .put("seq", message.seq)
        }
        val keys = when (message) {
            is ControlMessage.Drive -> DRIVE_KEYS
            else -> SIMPLE_KEYS
        }
        return render(json, keys)
    }

    /**
     * Interpreta un mensaje recibido del ESP32.
     *
     * @return el mensaje interpretado, o `null` si el texto no es JSON válido
     *         o no contiene ningún campo conocido.
     */
    fun decode(raw: String?): DeviceMessage? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        val json = try {
            JSONObject(text)
        } catch (e: JSONException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }

        return when (json.optString("type", "").lowercase()) {
            TYPE_STATUS -> DeviceMessage.Status(
                RoverStatus(
                    state = json.optString("state", RoverStatus.STATE_STOPPED),
                    left = json.optDouble("left", 0.0).toFloat(),
                    right = json.optDouble("right", 0.0).toFloat(),
                    seq = json.optInt("seq", 0),
                    uptimeMs = json.optLong("uptimeMs", 0L),
                ),
            )

            TYPE_PONG -> DeviceMessage.Pong(json.optInt("seq", 0))

            else -> DeviceMessage.Unknown(text)
        }
    }

    /**
     * Serializa un [JSONObject] respetando un orden fijo de claves.
     */
    private fun render(json: JSONObject, keys: List<String>): String {
        val sb = StringBuilder("{")
        var first = true
        for (key in keys) {
            if (!json.has(key)) continue
            val value = json.get(key)
            if (!first) sb.append(',')
            sb.append(JSONObject.quote(key)).append(':').append(renderValue(value))
            first = false
        }
        return sb.append('}').toString()
    }

    /**
     * Convierte un valor de `org.json` a su representación textual JSON.
     */
    private fun renderValue(value: Any?): String = when (value) {
        null -> "null"
        is Double -> formatNumber(value.toFloat())
        is Number -> value.toString()
        is Boolean -> value.toString()
        else -> JSONObject.quote(value.toString())
    }

    /**
     * Formatea un valor de consigna con un decimal fijo y sin notación
     * científica para los valores habituales (-1.0 … 1.0).
     */
    fun formatNumber(value: Float): String {
        if (value.isNaN() || value.isInfinite()) return "0.0"
        val rounded = Math.round(value.toDouble() * 1000.0) / 1000.0
        return rounded.toString()
    }
}
