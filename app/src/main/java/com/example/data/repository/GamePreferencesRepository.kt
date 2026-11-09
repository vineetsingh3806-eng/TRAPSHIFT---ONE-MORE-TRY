package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.BallSkin
import com.example.core.model.BoardTheme
import com.example.core.model.ControlScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trapshift_settings")

class GamePreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val CONTROL_SCHEME = stringPreferencesKey("control_scheme")
        val SELECTED_SKIN_ID = stringPreferencesKey("selected_skin_id")
        val SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SOUND_ENABLED] ?: true
    }

    val musicEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.MUSIC_ENABLED] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
    }

    val controlScheme: Flow<ControlScheme> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.CONTROL_SCHEME] ?: ControlScheme.BOTH.name
        try {
            ControlScheme.valueOf(raw)
        } catch (_: Exception) {
            ControlScheme.BOTH
        }
    }

    val selectedSkin: Flow<BallSkin> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.SELECTED_SKIN_ID] ?: BallSkin.CLASSIC.id
        BallSkin.fromId(id)
    }

    val selectedTheme: Flow<BoardTheme> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.SELECTED_THEME_ID] ?: BoardTheme.MIDNIGHT.id
        BoardTheme.fromId(id)
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setControlScheme(scheme: ControlScheme) {
        context.dataStore.edit { it[PreferencesKeys.CONTROL_SCHEME] = scheme.name }
    }

    suspend fun setSelectedSkin(skin: BallSkin) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_SKIN_ID] = skin.id }
    }

    suspend fun setSelectedTheme(theme: BoardTheme) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_THEME_ID] = theme.id }
    }
}
