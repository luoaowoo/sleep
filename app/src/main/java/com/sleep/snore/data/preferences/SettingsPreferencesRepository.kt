package com.sleep.snore.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class SettingsPreferences(
    val silenceThresholdDb: Float = SettingsPreferencesRepository.DEFAULT_SILENCE_THRESHOLD_DB,
    val autoCleanEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_AUTO_CLEAN_ENABLED,
    val dynamicColorEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_DYNAMIC_COLOR_ENABLED,
    val themeMode: String = SettingsPreferencesRepository.DEFAULT_THEME_MODE,
    val compactModeEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_COMPACT_MODE_ENABLED,
    val showTechnicalDetails: Boolean = SettingsPreferencesRepository.DEFAULT_SHOW_TECHNICAL_DETAILS
)

@Singleton
class SettingsPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val settings: Flow<SettingsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SettingsPreferences(
                silenceThresholdDb = preferences[Keys.SILENCE_THRESHOLD_DB]
                    ?.coerceIn(MIN_SILENCE_THRESHOLD_DB, MAX_SILENCE_THRESHOLD_DB)
                    ?: DEFAULT_SILENCE_THRESHOLD_DB,
                autoCleanEnabled = preferences[Keys.AUTO_CLEAN_ENABLED]
                    ?: DEFAULT_AUTO_CLEAN_ENABLED,
                dynamicColorEnabled = preferences[Keys.DYNAMIC_COLOR_ENABLED]
                    ?: DEFAULT_DYNAMIC_COLOR_ENABLED,
                themeMode = preferences[Keys.THEME_MODE]
                    ?: DEFAULT_THEME_MODE,
                compactModeEnabled = preferences[Keys.COMPACT_MODE_ENABLED]
                    ?: DEFAULT_COMPACT_MODE_ENABLED,
                showTechnicalDetails = preferences[Keys.SHOW_TECHNICAL_DETAILS]
                    ?: DEFAULT_SHOW_TECHNICAL_DETAILS
            )
        }

    suspend fun setSilenceThresholdDb(value: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.SILENCE_THRESHOLD_DB] = value.coerceIn(
                MIN_SILENCE_THRESHOLD_DB,
                MAX_SILENCE_THRESHOLD_DB
            )
        }
    }

    suspend fun setAutoCleanEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_CLEAN_ENABLED] = enabled
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = when (mode) {
                THEME_MODE_LIGHT, THEME_MODE_DARK -> mode
                else -> THEME_MODE_SYSTEM
            }
        }
    }

    suspend fun setCompactModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPACT_MODE_ENABLED] = enabled
        }
    }

    suspend fun setShowTechnicalDetails(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_TECHNICAL_DETAILS] = enabled
        }
    }

    private object Keys {
        val SILENCE_THRESHOLD_DB = floatPreferencesKey("silence_threshold_db")
        val AUTO_CLEAN_ENABLED = booleanPreferencesKey("auto_clean_enabled")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        val COMPACT_MODE_ENABLED = booleanPreferencesKey("compact_mode_enabled")
        val SHOW_TECHNICAL_DETAILS = booleanPreferencesKey("show_technical_details")
    }

    companion object {
        const val MIN_SILENCE_THRESHOLD_DB = -60f
        const val MAX_SILENCE_THRESHOLD_DB = -20f
        const val DEFAULT_SILENCE_THRESHOLD_DB = -40f
        const val DEFAULT_AUTO_CLEAN_ENABLED = true
        const val DEFAULT_DYNAMIC_COLOR_ENABLED = true
        const val DEFAULT_COMPACT_MODE_ENABLED = false
        const val DEFAULT_SHOW_TECHNICAL_DETAILS = true
        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"
        const val DEFAULT_THEME_MODE = THEME_MODE_SYSTEM
    }
}
