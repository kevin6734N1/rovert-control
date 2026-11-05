package com.rovert.rovercontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rovert.rovercontrol.R
import com.rovert.rovercontrol.model.ConnectionState
import com.rovert.rovercontrol.model.DriveCommand
import com.rovert.rovercontrol.model.RoverStatus

/**
 * Tira de estado: conexión, fotogramas por segundo, estado del rover y orden
 * activa.
 */
@Composable
fun StatusBar(
    connectionState: ConnectionState,
    fps: Float,
    status: RoverStatus?,
    activeCommand: DriveCommand?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectionChip(connectionState)
            StatusPill(
                label = stringResource(R.string.label_fps),
                value = String.format(java.util.Locale.US, "%.0f", fps),
                accent = MaterialTheme.colorScheme.tertiary,
            )
            StatusPill(
                label = stringResource(R.string.label_rover),
                value = roverStateLabel(status),
                accent = if (status?.isDriving == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = stringResource(R.string.label_command),
                value = commandLabel(activeCommand),
                accent = if (activeCommand != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            StatusPill(
                label = stringResource(R.string.label_uptime),
                value = uptimeLabel(status),
                accent = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Indicador del estado de la conexión con el rover.
 */
@Composable
fun ConnectionChip(connectionState: ConnectionState, modifier: Modifier = Modifier) {
    val color = when (connectionState) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionState.ERROR -> MaterialTheme.colorScheme.error
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
    }
    val label = when (connectionState) {
        ConnectionState.CONNECTED -> stringResource(R.string.status_connected)
        ConnectionState.CONNECTING -> stringResource(R.string.status_connecting)
        ConnectionState.ERROR -> stringResource(R.string.status_error)
        ConnectionState.DISCONNECTED -> stringResource(R.string.status_disconnected)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Píldora con una etiqueta y un valor.
 */
@Composable
private fun StatusPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/**
 * Texto del estado del rover.
 */
@Composable
private fun roverStateLabel(status: RoverStatus?): String = when {
    status == null -> stringResource(R.string.status_disconnected)
    status.isDriving -> stringResource(R.string.rover_driving)
    else -> stringResource(R.string.rover_stopped)
}

/**
 * Texto de la orden activa.
 */
@Composable
private fun commandLabel(command: DriveCommand?): String = when (command) {
    DriveCommand.FORWARD -> stringResource(R.string.command_forward)
    DriveCommand.BACKWARD -> stringResource(R.string.command_backward)
    DriveCommand.LEFT -> stringResource(R.string.command_left)
    DriveCommand.RIGHT -> stringResource(R.string.command_right)
    DriveCommand.STOP -> stringResource(R.string.command_stop)
    null -> stringResource(R.string.command_none)
}

/**
 * Texto del uptime del rover.
 */
@Composable
private fun uptimeLabel(status: RoverStatus?): String {
    if (status == null) return "--"
    val seconds = status.uptimeSeconds
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes > 0) "${minutes}m ${rest}s" else "${rest}s"
}
