package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dots_boxes_settings")

data class UserSettings(
    val nickname: String = "Player",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val localPlayerId: String = "",
    val lastHostIp: String = "192.168.43.1",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LOCAL_PLAYER_ID = stringPreferencesKey("local_player_id")
        val LAST_HOST_IP = stringPreferencesKey("last_host_ip")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            nickname = prefs[Keys.NICKNAME] ?: "Player",
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            localPlayerId = prefs[Keys.LOCAL_PLAYER_ID] ?: "",
            lastHostIp = prefs[Keys.LAST_HOST_IP] ?: "192.168.43.1",
        )
    }

    suspend fun ensureLocalPlayerId(): String {
        val existing = context.settingsDataStore.data.first()[Keys.LOCAL_PLAYER_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LOCAL_PLAYER_ID] = generated
        }
        return generated
    }

    suspend fun setNickname(nickname: String) {
        val sanitized = nickname.trim().take(18).ifEmpty { "Player" }
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NICKNAME] = sanitized
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setLastHostIp(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isNotEmpty()) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.LAST_HOST_IP] = trimmed
            }
        }
    }
}
