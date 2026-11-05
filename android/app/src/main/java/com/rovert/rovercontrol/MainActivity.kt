package com.rovert.rovercontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rovert.rovercontrol.ui.RoverScreen
import com.rovert.rovercontrol.ui.theme.RoverTheme

/**
 * Actividad principal: una sola pantalla de control.
 */
class MainActivity : ComponentActivity() {

    private val roverApp: RoverApplication
        get() = application as RoverApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RoverTheme {
                RoverScreen(
                    client = roverApp.roverClient,
                    engine = roverApp.controlEngine,
                    gamepad = roverApp.gamepadController,
                    settingsStore = roverApp.settingsStore,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        roverApp.controlEngine.onAppForegrounded()
    }

    override fun onPause() {
        super.onPause()
        // Al salir de primer plano (o apagar la pantalla) el rover se detiene.
        roverApp.controlEngine.onAppBackgrounded()
    }

    override fun onStop() {
        super.onStop()
        roverApp.controlEngine.onAppBackgrounded()
    }
}
