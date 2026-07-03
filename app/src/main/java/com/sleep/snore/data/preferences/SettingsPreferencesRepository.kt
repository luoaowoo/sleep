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
    val autoCleanEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_AUTO_CLEAN_ENABLED
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
                    ?: DEFAULT_AUTO_CLEAN_ENABLED
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

    private object Keys {
        val SILENCE_THRESHOLD_DB = floatPreferencesKey("silence_threshold_db")
        val AUTO_CLEAN_ENABLED = booleanPreferencesKey("auto_clean_enabled")
    }

    companion object {
        const val MIN_SILENCE_THRESHOLD_DB = -60f
        const val MAX_SILENCE_THRESHOLD_DB = -20f
        const val DEFAULT_SILENCE_THRESHOLD_DB = -40f
        const val DEFAULT_AUTO_CLEAN_ENABLED = true
    }
}
