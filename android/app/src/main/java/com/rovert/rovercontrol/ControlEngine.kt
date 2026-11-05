package com.rovert.rovercontrol

import com.rovert.rovercontrol.data.SettingsStore
import com.rovert.rovercontrol.input.GamepadAxes
import com.rovert.rovercontrol.input.GamepadController
import com.rovert.rovercontrol.input.GamepadState
import com.rovert.rovercontrol.model.ConnectionState
import com.rovert.rovercontrol.model.DriveCommand
import com.rovert.rovercontrol.net.RoverClient
import com.rovert.rovercontrol.protocol.MotorMapping
import com.rovert.rovercontrol.protocol.MotorPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Núcleo de control: decide qué orden se envía al rover y con qué cadencia.
 *
 * Es la capa que coordina las tres fuentes de orden (táctil, mando y botón de
 * STOP) y aplica las reglas de seguridad del contrato (§5):
 *
 * 1. Hold-to-move: sólo existe orden mientras el control está pulsado.
 * 2. STOP tiene prioridad sobre cualquier orden de movimiento.
 * 3. Se manda `stop` al soltar, al cancelar el gesto, al perder la conexión,
 *    al desconectar el mando, al mandar la app a segundo plano y ante un
 *    error del stream de vídeo.
 * 4. Dos direcciones contradictorias nunca coexisten: se resuelve a STOP.
 */
class ControlEngine(
    private val scope: CoroutineScope,
    private val client: RoverClient,
    private val settingsStore: SettingsStore,
    private val gamepad: GamepadController,
) {

    /**
     * Modo de control activo shown en la interfaz.
     */
    enum class ControlMode {
        /** Control mediante la cruceta táctil. */
        TACTILE,

        /** Control mediante el mando Bluetooth. */
        GAMEPAD,
    }

    companion object {
        /** Cadencia de envío de órdenes `drive`: 10 Hz. */
        const val DRIVE_INTERVAL_MS = 100L
    }

    private val lock = Any()

    private val _mode = MutableStateFlow(ControlMode.TACTILE)

    /** Modo de control activo. */
    val mode: StateFlow<ControlMode> = _mode.asStateFlow()

    private val _activeCommand = MutableStateFlow<DriveCommand?>(null)

    /** Última orden lógica en curso, o `null` si el rover está parado. */
    val activeCommand: StateFlow<DriveCommand?> = _activeCommand.asStateFlow()

    private val _activePair = MutableStateFlow(MotorPair.STOP)

    /** Consigna normalizada que se está transmitiendo. */
    val activePair: StateFlow<MotorPair> = _activePair.asStateFlow()

    private val touchHeld = LinkedHashSet<DriveCommand>()
    private var stopLatch = false
    private var driveJob: Job? = null

    /**
     * Arranca la suscripción a los eventos de red y de mando.
     */
    fun start() {
        scope.launch {
            client.connectionState.collect { state ->
                if (state != ConnectionState.CONNECTED) {
                    synchronized(lock) {
                        touchHeld.clear()
                        setActivePair(MotorPair.STOP)
                    }
                }
            }
        }
        scope.launch {
            gamepad.axes.collect { axes ->
                onGamepadAxesChanged(axes)
            }
        }
        scope.launch {
            gamepad.pressedButtons.collect {
                onGamepadAxesChanged(gamepad.axes.value)
            }
        }
        scope.launch {
            gamepad.state.collect { state ->
                if (state is GamepadState.DISCONNECTED) {
                    synchronized(lock) {
                        setActivePair(MotorPair.STOP)
                        if (_mode.value == ControlMode.GAMEPAD) {
                            _mode.value = ControlMode.TACTILE
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Entrada táctil
    // ---------------------------------------------------------------------

    /**
     * El usuario ha pulsado una dirección de la cruceta.
     */
    fun onTouchPress(command: DriveCommand) {
        synchronized(lock) {
            if (command == DriveCommand.STOP) {
                emergencyStopLocked()
                return
            }
            _mode.value = ControlMode.TACTILE
            touchHeld.add(command)
            updateLatchLocked()
            setActivePair(MotorMapping.fromCommands(touchHeld))
        }
    }

    /**
     * El usuario ha soltado una dirección de la cruceta.
     */
    fun onTouchRelease(command: DriveCommand) {
        synchronized(lock) {
            touchHeld.remove(command)
            updateLatchLocked()
            setActivePair(currentPairLocked())
        }
    }

    /**
     * El usuario ha soltado todas las direcciones de la cruceta.
     */
    fun onTouchReleaseAll() {
        synchronized(lock) {
            touchHeld.clear()
            updateLatchLocked()
            setActivePair(currentPairLocked())
        }
    }

    // ---------------------------------------------------------------------
    // Botón de STOP
    // ---------------------------------------------------------------------

    /**
     * Parada de emergencia: detiene los motores y bloquea cualquier orden
     * pendiente hasta que el usuario suelte los controles.
     */
    fun emergencyStop() {
        synchronized(lock) {
            emergencyStopLocked()
        }
    }

    private fun emergencyStopLocked() {
        // STOP sólo bloquea el movimiento si había alguna orden en curso: si
        // el rover ya estaba parado, el siguiente toque debe poder moverlo.
        val anythingHeld = touchHeld.isNotEmpty() || !gamepadPairLocked().isStop
        touchHeld.clear()
        stopLatch = anythingHeld
        _activePair.value = MotorPair.STOP
        _activeCommand.value = null
        stopDriveLoop()
        client.sendStop()
    }

    // ---------------------------------------------------------------------
    // Ciclo de vida de la app
    // ---------------------------------------------------------------------

    /**
     * La app pasa a segundo plano o se apaga la pantalla: se para el rover y
     * se limpian las entradas pendientes.
     */
    fun onAppBackgrounded() {
        synchronized(lock) {
            touchHeld.clear()
            stopLatch = true
            gamepad.resetAxes()
            _activePair.value = MotorPair.STOP
            _activeCommand.value = null
            stopDriveLoop()
            client.sendStop()
        }
    }

    /**
     * La app vuelve a primer plano.
     */
    fun onAppForegrounded() {
        synchronized(lock) {
            updateLatchLocked()
        }
    }

    // ---------------------------------------------------------------------
    // Lógica interna
    // ---------------------------------------------------------------------

    private fun onGamepadAxesChanged(axes: GamepadAxes) {
        synchronized(lock) {
            val deadZone = settingsStore.settings.value.deadZone
            val (x, y) = axes.effective(deadZone)
            val buttonDirection = gamepad.directionFromButtons()

            val pair = when {
                buttonDirection != null -> MotorMapping.fromCommand(buttonDirection)
                abs(x) > deadZone || abs(y) > deadZone -> MotorMapping.fromAxes(x, y, deadZone)
                else -> MotorPair.STOP
            }

            if (!pair.isStop && _mode.value != ControlMode.GAMEPAD) {
                // El mando cobra el control: la cruceta táctil queda liberada.
                _mode.value = ControlMode.GAMEPAD
                touchHeld.clear()
                updateLatchLocked()
            }

            if (_mode.value == ControlMode.GAMEPAD) {
                updateLatchLocked()
                setActivePair(pair)
            }
        }
    }

    /**
     * Par de consignas que corresponde al modo de control activo.
     */
    private fun currentPairLocked(): MotorPair = when (_mode.value) {
        ControlMode.GAMEPAD -> gamepadPairLocked()
        ControlMode.TACTILE -> MotorMapping.fromCommands(touchHeld)
    }

    private fun gamepadPairLocked(): MotorPair {
        val deadZone = settingsStore.settings.value.deadZone
        val axes = gamepad.axes.value
        val (x, y) = axes.effective(deadZone)
        val buttonDirection = gamepad.directionFromButtons()
        return when {
            buttonDirection != null -> MotorMapping.fromCommand(buttonDirection)
            abs(x) > deadZone || abs(y) > deadZone -> MotorMapping.fromAxes(x, y, deadZone)
            else -> MotorPair.STOP
        }
    }

    /**
     * Libera el bloqueo de STOP cuando ya no queda ningún control pulsado.
     */
    private fun updateLatchLocked() {
        if (touchHeld.isEmpty() && gamepadPairLocked().isStop) {
            stopLatch = false
        }
    }

    private fun setActivePair(pair: MotorPair) {
        if (stopLatch && !pair.isStop) {
            // STOP tiene prioridad: no se retoma el movimiento hasta que el
            // usuario suelte todos los controles.
            _activePair.value = MotorPair.STOP
            _activeCommand.value = null
            stopDriveLoop()
            return
        }

        _activePair.value = pair
        if (pair.isStop) {
            _activeCommand.value = null
            stopDriveLoop()
            client.sendStop()
        } else {
            _activeCommand.value = commandOf(pair)
            startDriveLoop()
        }
    }

    /**
     * Traduce un par de consignas a la orden lógica que se muestra en la UI.
     */
    private fun commandOf(pair: MotorPair): DriveCommand? {
        val epsilon = 0.01f
        return when {
            pair.left > epsilon && pair.right > epsilon -> DriveCommand.FORWARD
            pair.left < -epsilon && pair.right < -epsilon -> DriveCommand.BACKWARD
            pair.left < -epsilon && pair.right > epsilon -> DriveCommand.LEFT
            pair.left > epsilon && pair.right < -epsilon -> DriveCommand.RIGHT
            else -> null
        }
    }

    private fun startDriveLoop() {
        if (driveJob?.isActive == true) return
        driveJob = scope.launch {
            while (isActive) {
                val pair = _activePair.value
                if (pair.isStop) {
                    client.sendStop()
                    break
                }
                client.sendDrive(pair.left, pair.right)
                delay(DRIVE_INTERVAL_MS)
            }
        }
    }

    private fun stopDriveLoop() {
        driveJob?.cancel()
        driveJob = null
    }
}
