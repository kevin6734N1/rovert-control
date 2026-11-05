package com.rovert.rovercontrol

import android.app.Application
import com.rovert.rovercontrol.data.SettingsStore
import com.rovert.rovercontrol.input.GamepadController
import com.rovert.rovercontrol.net.RoverClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contenedor de singletons de la app.
 *
 * Aquí viven el cliente de red, el almacén de ajustes, la lectura del mando y
 * el motor de control, de modo que toda la app comparte la misma conexión.
 */
class RoverApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Ajustes persistentes. */
    lateinit var settingsStore: SettingsStore
        private set

    /** Cliente de red del rover. */
    lateinit var roverClient: RoverClient
        private set

    /** Lector del mando Bluetooth. */
    lateinit var gamepadController: GamepadController
        private set

    /** Motor de control (ordenes, cadencia y reglas de seguridad). */
    lateinit var controlEngine: ControlEngine
        private set

    override fun onCreate() {
        super.onCreate()

        settingsStore = SettingsStore(this)
        roverClient = RoverClient(applicationScope) { settingsStore.settings.value }
        gamepadController = GamepadController(applicationScope)
        controlEngine = ControlEngine(
            scope = applicationScope,
            client = roverClient,
            settingsStore = settingsStore,
            gamepad = gamepadController,
        )

        gamepadController.start()
        controlEngine.start()

        if (settingsStore.settings.value.autoConnect) {
            roverClient.connect()
        }
    }
}
