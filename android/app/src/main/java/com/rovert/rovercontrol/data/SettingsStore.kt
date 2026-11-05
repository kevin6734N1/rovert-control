package com.rovert.rovercontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.rovert.rovercontrol.model.RoverSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Almacén de ajustes del rover respaldado por `SharedPreferences`.
 *
 * Los valores se leen una vez al arrancar y se mantienen en un [StateFlow]
 * para que la capa de red pueda consultarlos en cualquier momento.
 */
class SettingsStore(context: Context) {

    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())

    /** Ajustes actuales. */
    val settings: StateFlow<RoverSettings> = _settings.asStateFlow()

    /**
     * Guarda unos ajustes nuevos, saneando los valores fuera de rango.
     */
    fun update(settings: RoverSettings) {
        val sanitized = settings.sanitized()
        _settings.value = sanitized
        persist(sanitized)
    }

    /**
     * Vuelve a los valores por defecto del contrato.
     */
    fun resetToDefaults() {
        update(RoverSettings())
    }

    private fun load(): RoverSettings = RoverSettings(
        host = preferences.getString(KEY_HOST, RoverSettings.DEFAULT_HOST) ?: RoverSettings.DEFAULT_HOST,
        port = preferences.getInt(KEY_PORT, RoverSettings.DEFAULT_PORT),
        ssidHint = preferences.getString(KEY_SSID, RoverSettings.DEFAULT_SSID) ?: RoverSettings.DEFAULT_SSID,
        maxSpeed = preferences.getFloat(KEY_MAX_SPEED, RoverSettings.DEFAULT_MAX_SPEED),
        leftPolarity = preferences.getInt(KEY_LEFT_POLARITY, 1),
        rightPolarity = preferences.getInt(KEY_RIGHT_POLARITY, 1),
        deadZone = preferences.getFloat(KEY_DEAD_ZONE, RoverSettings.DEFAULT_DEAD_ZONE),
        autoConnect = preferences.getBoolean(KEY_AUTO_CONNECT, true),
    ).sanitized()

    private fun persist(settings: RoverSettings) {
        preferences.edit()
            .putString(KEY_HOST, settings.host)
            .putInt(KEY_PORT, settings.port)
            .putString(KEY_SSID, settings.ssidHint)
            .putFloat(KEY_MAX_SPEED, settings.maxSpeed)
            .putInt(KEY_LEFT_POLARITY, settings.leftPolarity)
            .putInt(KEY_RIGHT_POLARITY, settings.rightPolarity)
            .putFloat(KEY_DEAD_ZONE, settings.deadZone)
            .putBoolean(KEY_AUTO_CONNECT, settings.autoConnect)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "rovert_settings"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_SSID = "ssid_hint"
        const val KEY_MAX_SPEED = "max_speed"
        const val KEY_LEFT_POLARITY = "left_polarity"
        const val KEY_RIGHT_POLARITY = "right_polarity"
        const val KEY_DEAD_ZONE = "dead_zone"
        const val KEY_AUTO_CONNECT = "auto_connect"
    }
}
