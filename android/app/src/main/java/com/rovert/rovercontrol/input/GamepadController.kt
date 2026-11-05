package com.rovert.rovercontrol.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.rovert.rovercontrol.model.DriveCommand
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Estado del mando (gamepad) conectado.
 */
sealed class GamepadState {
    /** No hay ningún mando con ejes de joystick disponible. */
    object DISCONNECTED : GamepadState()

    /** Hay un mando conectado. */
    data class CONNECTED(val name: String) : GamepadState()
}

/**
 * Valores de los ejes del stick izquierdo y de la "hat" (cruceta).
 */
data class GamepadAxes(
    val x: Float = 0f,
    val y: Float = 0f,
    val hatX: Float = 0f,
    val hatY: Float = 0f,
) {
    /**
     * Ejes efectivos a usar: el stick izquierdo tiene prioridad y la cruceta
     * actúa como respaldo cuando el stick está en la zona neutra.
     */
    fun effective(deadZone: Float): Pair<Float, Float> {
        val stickActive = abs(x) > deadZone || abs(y) > deadZone
        if (stickActive) return x to y
        return hatX to hatY
    }
}

/**
 * Lectura del mando conectado por Bluetooth (por ejemplo, un DualShock 4).
 *
 * Android entrega los eventos de joystick a la vista que tiene el foco, por
 * eso la captura se hace en [GamepadCaptureView] y aquí sólo se procesa la
 * información. No hace falta ningún permiso de Bluetooth para *leer* el
 * mando: el emparejamiento se hace en los ajustes de Android.
 *
 * Si hay varios mandos conectados, se elige el primero que declare ejes de
 * joystick.
 */
class GamepadController(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<GamepadState>(GamepadState.DISCONNECTED)

    /** Estado de conexión del mando. */
    val state: StateFlow<GamepadState> = _state.asStateFlow()

    private val _axes = MutableStateFlow(GamepadAxes())

    /** Últimos valores de los ejes del stick. */
    val axes: StateFlow<GamepadAxes> = _axes.asStateFlow()

    private val _pressedButtons = MutableStateFlow<Set<Int>>(emptySet())

    /** Teclas de cruceta que están pulsadas. */
    val pressedButtons: StateFlow<Set<Int>> = _pressedButtons.asStateFlow()

    private var pollingJob: Job? = null

    /**
     * Empieza a vigilar la lista de dispositivos de entrada para detectar
     * conexiones y desconexiones del mando.
     */
    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                refreshDevices()
                delay(DEVICE_POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Detiene la vigilancia de dispositivos.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Descarta los valores de ejes y botones memorizados.
     *
     * Se usa al pasar la app a segundo plano: el rover se detiene y no se
     * retoma el movimiento con un mando que siguiera pulsado desde antes.
     */
    fun resetAxes() {
        _axes.value = GamepadAxes()
        _pressedButtons.value = emptySet()
    }

    /**
     * Comprueba los dispositivos de entrada y actualiza el estado del mando.
     */
    fun refreshDevices() {
        val device = findGamepad()
        val current = _state.value
        if (device == null) {
            if (current !is GamepadState.DISCONNECTED) {
                _state.value = GamepadState.DISCONNECTED
                _axes.value = GamepadAxes()
                _pressedButtons.value = emptySet()
            }
        } else {
            val name = device.name?.takeIf { it.isNotBlank() } ?: DEFAULT_GAMEPAD_NAME
            if (current !is GamepadState.CONNECTED || current.name != name) {
                _state.value = GamepadState.CONNECTED(name)
            }
        }
    }

    /**
     * Traduce un evento genérico de movimiento (ejes del stick) a valores de
     * eje. Devuelve `true` si el evento procede de un mando.
     */
    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_MOVE) return false
        val device = event.device ?: return false
        if (!isGamepad(device)) return false

        _axes.value = GamepadAxes(
            x = event.getAxisValue(MotionEvent.AXIS_X),
            y = event.getAxisValue(MotionEvent.AXIS_Y),
            hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
            hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
        )
        return true
    }

    /**
     * Traduce un evento de tecla (cruceta del mando) al conjunto de botones
     * pulsados. Devuelve `true` si el evento procede de un mando.
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (!isGamepad(device)) return false

        val keyCode = event.keyCode
        if (keyCode !in DPAD_KEYS) return false

        val pressed = _pressedButtons.value.toMutableSet()
        if (event.action == KeyEvent.ACTION_DOWN) {
            pressed.add(keyCode)
        } else {
            pressed.remove(keyCode)
        }
        _pressedButtons.value = pressed
        return true
    }

    /**
     * Dirección equivalente a la cruceta que está pulsada, si la hay.
     */
    fun directionFromButtons(): DriveCommand? {
        val pressed = _pressedButtons.value
        val up = KeyEvent.KEYCODE_DPAD_UP in pressed
        val down = KeyEvent.KEYCODE_DPAD_DOWN in pressed
        val left = KeyEvent.KEYCODE_DPAD_LEFT in pressed
        val right = KeyEvent.KEYCODE_DPAD_RIGHT in pressed
        return when {
            up && down -> null
            left && right -> null
            up -> DriveCommand.FORWARD
            down -> DriveCommand.BACKWARD
            left -> DriveCommand.LEFT
            right -> DriveCommand.RIGHT
            else -> null
        }
    }

    /**
     * Busca el primer mando disponible que declare ejes de joystick.
     */
    private fun findGamepad(): InputDevice? {
        val ids = InputDevice.getDeviceIds()
        for (id in ids) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            if (!isGamepad(device)) continue
            val hasStick = device.getMotionRange(MotionEvent.AXIS_X) != null &&
                device.getMotionRange(MotionEvent.AXIS_Y) != null
            if (hasStick) return device
        }
        return null
    }

    /**
     * `true` si el dispositivo declara ser un gamepad o un joystick.
     */
    private fun isGamepad(device: InputDevice): Boolean {
        val sources = device.sources
        return sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }

    companion object {
        /** Intervalo de vigilancia de dispositivos de entrada. */
        const val DEVICE_POLL_INTERVAL_MS = 1_000L

        /** Nombre mostrado cuando el mando no informa de uno. */
        const val DEFAULT_GAMEPAD_NAME = "Mando"

        private val DPAD_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        )
    }
}
