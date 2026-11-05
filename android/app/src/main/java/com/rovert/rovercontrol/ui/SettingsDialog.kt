package com.rovert.rovercontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.rovert.rovercontrol.R
import com.rovert.rovercontrol.model.RoverSettings

/**
 * Diálogo de ajustes del rover.
 *
 * Los cambios se aplican en el cliente (polaridad y límite de velocidad) y
 * se guardan en `SharedPreferences`.
 */
@Composable
fun SettingsDialog(
    settings: RoverSettings,
    onDismiss: () -> Unit,
    onSave: (RoverSettings) -> Unit,
) {
    var host by rememberSaveable(settings.host) { mutableStateOf(settings.host) }
    var port by rememberSaveable(settings.port) { mutableStateOf(settings.port.toString()) }
    var ssid by rememberSaveable(settings.ssidHint) { mutableStateOf(settings.ssidHint) }
    var maxSpeed by rememberSaveable(settings.maxSpeed) { mutableFloatStateOf(settings.maxSpeed) }
    var leftInverted by rememberSaveable(settings.leftPolarity) {
        mutableStateOf(settings.leftPolarity < 0)
    }
    var rightInverted by rememberSaveable(settings.rightPolarity) {
        mutableStateOf(settings.rightPolarity < 0)
    }
    var deadZone by rememberSaveable(settings.deadZone) { mutableFloatStateOf(settings.deadZone) }
    var autoConnect by rememberSaveable(settings.autoConnect) { mutableStateOf(settings.autoConnect) }

    val portNumber = port.toIntOrNull()
    val hostValid = host.isNotBlank() && isValidHost(host.trim())
    val portValid = portNumber != null && portNumber in RoverSettings.MIN_PORT..RoverSettings.MAX_PORT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(text = stringResource(R.string.settings_host)) },
                    singleLine = true,
                    isError = !hostValid,
                    supportingText = if (!hostValid) {
                        { Text(text = stringResource(R.string.settings_invalid_host)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { char -> char.isDigit() }.take(5) },
                    label = { Text(text = stringResource(R.string.settings_port)) },
                    singleLine = true,
                    isError = !portValid,
                    supportingText = if (!portValid) {
                        { Text(text = stringResource(R.string.settings_invalid_port)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text(text = stringResource(R.string.settings_ssid)) },
                    singleLine = true,
                    supportingText = { Text(text = stringResource(R.string.settings_ssid_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                SliderRow(
                    label = stringResource(R.string.settings_speed),
                    valueLabel = stringResource(
                        R.string.settings_speed_value,
                        (maxSpeed * 100).toInt(),
                    ),
                    value = maxSpeed,
                    valueRange = RoverSettings.MIN_SPEED..RoverSettings.MAX_SPEED,
                    onValueChange = { maxSpeed = it },
                )

                SliderRow(
                    label = stringResource(R.string.settings_dead_zone),
                    valueLabel = stringResource(
                        R.string.settings_dead_zone_value,
                        (deadZone * 100).toInt(),
                    ),
                    value = deadZone,
                    valueRange = RoverSettings.MIN_DEAD_ZONE..RoverSettings.MAX_DEAD_ZONE,
                    onValueChange = { deadZone = it },
                )

                SwitchRow(
                    label = stringResource(R.string.settings_left_polarity),
                    checked = leftInverted,
                    onCheckedChange = { leftInverted = it },
                )

                SwitchRow(
                    label = stringResource(R.string.settings_right_polarity),
                    checked = rightInverted,
                    onCheckedChange = { rightInverted = it },
                )

                SwitchRow(
                    label = stringResource(R.string.settings_auto_connect),
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it },
                )

                Text(
                    text = stringResource(R.string.settings_network_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = hostValid && portValid,
                onClick = {
                    onSave(
                        settings.copy(
                            host = host.trim(),
                            port = portNumber ?: settings.port,
                            ssidHint = ssid.trim(),
                            maxSpeed = maxSpeed,
                            leftPolarity = if (leftInverted) -1 else 1,
                            rightPolarity = if (rightInverted) -1 else 1,
                            deadZone = deadZone,
                            autoConnect = autoConnect,
                        ),
                    )
                },
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * Fila con etiqueta, valor y deslizador.
 */
@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

/**
 * Fila con etiqueta y conmutador.
 */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Comprueba que el host sea una IP o un nombre de host razonable.
 */
private fun isValidHost(host: String): Boolean {
    if (host.isEmpty() || host.length > 253) return false
    if (host.contains(' ')) return false
    val ipv4 = host.split('.')
    if (ipv4.size == 4) {
        return ipv4.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all { it.isDigit() } &&
                part.toInt() in 0..255
        }
    }
    return host.matches(Regex("^[A-Za-z0-9._-]+$"))
}
