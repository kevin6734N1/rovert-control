package com.rovert.rovercontrol.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rovert.rovercontrol.R
import com.rovert.rovercontrol.model.DriveCommand

/**
 * Cruceta táctil con semántica de "mantener pulsado para mover".
 *
 * Al pulsar una dirección se avisa con [onPress]; en cuanto se suelta el dedo,
 * se cancela el gesto o el puntero sale del control, se avisa con [onRelease]
 * para detener el rover de inmediato.
 *
 * El componente no decide qué hacer con varias direcciones simultáneas: eso
 * lo resuelve la capa de control, que resuelve a STOP cualquier par
 * contradictorio.
 */
@Composable
fun TouchDpad(
    onPress: (DriveCommand) -> Unit,
    onRelease: (DriveCommand) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 78.dp,
    enabled: Boolean = true,
) {
    val spacing = 10.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(buttonSize))
            DpadButton(
                command = DriveCommand.FORWARD,
                icon = Icons.Filled.KeyboardArrowUp,
                description = stringResource(R.string.dpad_up),
                size = buttonSize,
                enabled = enabled,
                onPress = onPress,
                onRelease = onRelease,
            )
            Spacer(Modifier.size(buttonSize))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DpadButton(
                command = DriveCommand.LEFT,
                icon = Icons.Filled.KeyboardArrowLeft,
                description = stringResource(R.string.dpad_left),
                size = buttonSize,
                enabled = enabled,
                onPress = onPress,
                onRelease = onRelease,
            )
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize(0.55f),
                )
            }
            DpadButton(
                command = DriveCommand.RIGHT,
                icon = Icons.Filled.KeyboardArrowRight,
                description = stringResource(R.string.dpad_right),
                size = buttonSize,
                enabled = enabled,
                onPress = onPress,
                onRelease = onRelease,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(buttonSize))
            DpadButton(
                command = DriveCommand.BACKWARD,
                icon = Icons.Filled.KeyboardArrowDown,
                description = stringResource(R.string.dpad_down),
                size = buttonSize,
                enabled = enabled,
                onPress = onPress,
                onRelease = onRelease,
            )
            Spacer(Modifier.size(buttonSize))
        }
    }
}

/**
 * Botón individual de la cruceta con pulsación sostenida.
 */
@Composable
private fun DpadButton(
    command: DriveCommand,
    icon: ImageVector,
    description: String,
    size: Dp,
    enabled: Boolean,
    onPress: (DriveCommand) -> Unit,
    onRelease: (DriveCommand) -> Unit,
) {
    var pressed by remember(command) { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)

    val background by animateColorAsState(
        targetValue = if (pressed && enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "dpadBackground",
    )
    val content by animateColorAsState(
        targetValue = if (pressed && enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "dpadContent",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (pressed && enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                },
                shape = shape,
            )
            .pointerInput(command, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onPress(command)
                    val pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            // Puntero liberado, cancelado o fuera del control:
                            // en cualquier caso hay que parar.
                            if (change == null || !change.pressed) break
                            if (!bounds.contains(change.position)) break
                            change.consumePositionChange()
                        }
                    } finally {
                        pressed = false
                        onRelease(command)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = content,
            modifier = Modifier.fillMaxSize(0.6f),
        )
    }
}
