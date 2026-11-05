package com.rovert.rovercontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rovert.rovercontrol.ControlEngine
import com.rovert.rovercontrol.R
import com.rovert.rovercontrol.input.TouchDpad
import com.rovert.rovercontrol.model.DriveCommand

/**
 * Panel de control: cruceta táctil, botón de STOP y aviso del modo activo.
 */
@Composable
fun ControlPanel(
    mode: ControlEngine.ControlMode,
    onPress: (DriveCommand) -> Unit,
    onRelease: (DriveCommand) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TouchDpad(
                onPress = onPress,
                onRelease = onRelease,
                enabled = enabled,
            )
        }

        Column(
            modifier = Modifier.width(132.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ModeBadge(mode = mode)

            StopButton(onStop = onStop, enabled = enabled)

            Text(
                text = stringResource(R.string.dpad_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Aviso del modo de control activo (táctil o mando).
 */
@Composable
fun ModeBadge(mode: ControlEngine.ControlMode, modifier: Modifier = Modifier) {
    val isGamepad = mode == ControlEngine.ControlMode.GAMEPAD
    val accent = if (isGamepad) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val label = if (isGamepad) {
        stringResource(R.string.mode_gamepad)
    } else {
        stringResource(R.string.mode_tactile)
    }
    val description = if (isGamepad) {
        stringResource(R.string.mode_gamepad_desc)
    } else {
        stringResource(R.string.mode_tactile_desc)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isGamepad) Icons.Filled.SportsEsports else Icons.Filled.TouchApp,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Botón rojo de parada: siempre tiene prioridad sobre cualquier orden.
 */
@Composable
fun StopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val stopLabel = stringResource(R.string.action_stop)
    val stopDescription = stringResource(R.string.action_stop_desc)

    Button(
        onClick = onStop,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .semantics { contentDescription = stopDescription },
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
    ) {
        Text(
            text = stopLabel,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
    }
}
