package com.rovert.rovercontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rovert.rovercontrol.ControlEngine
import com.rovert.rovercontrol.R
import com.rovert.rovercontrol.data.SettingsStore
import com.rovert.rovercontrol.input.GamepadCaptureView
import com.rovert.rovercontrol.input.GamepadController
import com.rovert.rovercontrol.input.GamepadState
import com.rovert.rovercontrol.model.ConnectionState
import com.rovert.rovercontrol.model.RoverError
import com.rovert.rovercontrol.net.RoverClient
import com.rovert.rovercontrol.video.MjpegState
import com.rovert.rovercontrol.video.MjpegStreamView

/**
 * Pantalla única de control del rover.
 *
 * De arriba abajo: vídeo en directo (4:3), tira de estado y panel de control
 * con la cruceta táctil, el botón de STOP y el aviso del modo activo.
 */
@Composable
fun RoverScreen(
    client: RoverClient,
    engine: ControlEngine,
    gamepad: GamepadController,
    settingsStore: SettingsStore,
) {
    val context = LocalContext.current
    val settings by settingsStore.settings.collectAsState()
    val connectionState by client.connectionState.collectAsState()
    val lastError by client.lastError.collectAsState()
    val status by client.status.collectAsState()
    val mode by engine.mode.collectAsState()
    val activeCommand by engine.activeCommand.collectAsState()
    val gamepadState by gamepad.state.collectAsState()

    val streamView = remember { MjpegStreamView(context) }
    val streamState by streamView.state.collectAsState()
    val fps by streamView.fps.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    val captureView = remember { mutableStateOf<GamepadCaptureView?>(null) }

    LaunchedEffect(settings.host, settings.port) {
        streamView.start(settings.host, settings.port)
    }

    DisposableEffect(Unit) {
        onDispose { streamView.stop() }
    }

    // Si el stream de vídeo falla, el rover se detiene por seguridad.
    LaunchedEffect(streamState) {
        if (streamState is MjpegState.ERROR) {
            engine.emergencyStop()
        }
    }

    // La captura del mando necesita el foco para recibir los ejes del stick.
    LaunchedEffect(captureView.value) {
        captureView.value?.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { streamView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConnectionChip(connectionState)
                    FilledTonalButton(
                        onClick = {
                            if (connectionState.isConnected) {
                                client.disconnect()
                            } else {
                                client.connect()
                            }
                        },
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                if (connectionState.isConnected) {
                                    R.string.action_disconnect
                                } else {
                                    R.string.action_connect
                                },
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GamepadBadge(gamepadState)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                GamepadCaptureView(ctx).also { view ->
                                    view.onGamepadKey = { event -> gamepad.onKeyEvent(event) }
                                    view.onGamepadMotion = { event ->
                                        gamepad.onGenericMotionEvent(event)
                                    }
                                    captureView.value = view
                                }
                            },
                            modifier = Modifier.size(46.dp),
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val streamError = (streamState as? MjpegState.ERROR)?.message
                if (streamError != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.video_error, streamError),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            TextButton(
                                onClick = { streamView.restart(settings.host, settings.port) },
                            ) {
                                Text(
                                    text = stringResource(R.string.video_retry),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }

            val connectionError = lastError
            if (connectionError != null && !connectionState.isConnected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = connectionErrorText(
                            error = connectionError,
                            ssid = settings.ssidHint,
                            host = settings.host,
                            port = settings.port,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            StatusBar(
                connectionState = connectionState,
                fps = fps,
                status = status,
                activeCommand = activeCommand,
            )

            ControlPanel(
                mode = mode,
                onPress = { command -> engine.onTouchPress(command) },
                onRelease = { command -> engine.onTouchRelease(command) },
                onStop = { engine.emergencyStop() },
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onSave = { updated ->
                settingsStore.update(updated)
                showSettings = false
                if (updated.host != settings.host || updated.port != settings.port) {
                    // El rover ha cambiado: se cierra el canal anterior para
                    // no seguir hablando con la IP equivocada.
                    client.disconnect()
                    client.connect()
                }
            },
        )
    }
}

/**
 * Texto en español del último error de conexión.
 */
@Composable
private fun connectionErrorText(
    error: RoverError,
    ssid: String,
    host: String,
    port: Int,
): String = when (error) {
    RoverError.HOST_NOT_FOUND -> stringResource(R.string.error_host_not_found, ssid)
    RoverError.TIMEOUT -> stringResource(R.string.error_timeout)
    RoverError.CONNECT_FAILED -> stringResource(R.string.error_connect, host, port)
    RoverError.NETWORK -> stringResource(R.string.error_network)
    RoverError.SEND_FAILED -> stringResource(R.string.error_send_failed)
    RoverError.UNKNOWN -> stringResource(R.string.error_generic)
}

/**
 * Aviso del mando detectado.
 */
@Composable
private fun GamepadBadge(gamepadState: GamepadState, modifier: Modifier = Modifier) {
    val connected = gamepadState is GamepadState.CONNECTED
    val accent = if (connected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val text = when (gamepadState) {
        is GamepadState.CONNECTED -> stringResource(R.string.gamepad_connected, gamepadState.name)
        GamepadState.DISCONNECTED -> stringResource(R.string.gamepad_disconnected)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
