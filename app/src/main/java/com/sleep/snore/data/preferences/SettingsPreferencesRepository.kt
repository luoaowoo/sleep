package com.sleep.snore.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.model.Sensitivity
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class SettingsPreferences(
    val silenceThresholdDb: Float = SettingsPreferencesRepository.DEFAULT_SILENCE_THRESHOLD_DB,
    val autoCleanEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_AUTO_CLEAN_ENABLED,
    val dynamicColorEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_DYNAMIC_COLOR_ENABLED,
    val themeMode: String = SettingsPreferencesRepository.DEFAULT_THEME_MODE,
    val compactModeEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_COMPACT_MODE_ENABLED,
    val showTechnicalDetails: Boolean = SettingsPreferencesRepository.DEFAULT_SHOW_TECHNICAL_DETAILS,
    val maxSegmentDurationSec: Int = SettingsPreferencesRepository.DEFAULT_MAX_SEGMENT_DURATION_SEC,
    val customAccentColorArgb: Int = SettingsPreferencesRepository.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
    val deepSeekApiKey: String = "",
    val deepSeekBaseUrl: String = SettingsPreferencesRepository.DEFAULT_DEEPSEEK_BASE_URL,
    val deepSeekModelName: String = SettingsPreferencesRepository.DEFAULT_DEEPSEEK_MODEL_NAME,
    val aiCustomInfo: String = ""
)

@Singleton
class SettingsPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val settings: Flow<SettingsPreferences> = safePreferences
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
                    ?: DEFAULT_SHOW_TECHNICAL_DETAILS,
                maxSegmentDurationSec = preferences[Keys.MAX_SEGMENT_DURATION_SEC]
                    ?.coerceIn(MIN_MAX_SEGMENT_DURATION_SEC, MAX_MAX_SEGMENT_DURATION_SEC)
                    ?: DEFAULT_MAX_SEGMENT_DURATION_SEC,
                customAccentColorArgb = preferences[Keys.CUSTOM_ACCENT_COLOR_ARGB]
                    ?: DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
                deepSeekApiKey = preferences[Keys.DEEPSEEK_API_KEY].orEmpty(),
                deepSeekBaseUrl = preferences[Keys.DEEPSEEK_BASE_URL]
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_DEEPSEEK_BASE_URL,
                deepSeekModelName = preferences[Keys.DEEPSEEK_MODEL_NAME]
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_DEEPSEEK_MODEL_NAME,
                aiCustomInfo = preferences[Keys.AI_CUSTOM_INFO].orEmpty()
            )
        }

    val accentColor: Flow<AccentColor> = safePreferences
        .map { preferences ->
            preferences[Keys.ACCENT_COLOR]
                ?.let { runCatching { AccentColor.valueOf(it) }.getOrDefault(AccentColor.INDIGO) }
                ?: AccentColor.INDIGO
        }

    val customAccentColorArgb: Flow<Int> = safePreferences
        .map { preferences ->
            preferences[Keys.CUSTOM_ACCENT_COLOR_ARGB] ?: DEFAULT_CUSTOM_ACCENT_COLOR_ARGB
        }

    val fontScale: Flow<FontScale> = safePreferences
        .map { preferences ->
            preferences[Keys.FONT_SCALE]
                ?.let { runCatching { FontScale.valueOf(it) }.getOrDefault(FontScale.STANDARD) }
                ?: FontScale.STANDARD
        }

    val cardCornerStyle: Flow<CardCornerStyle> = safePreferences
        .map { preferences ->
            preferences[Keys.CARD_CORNER_STYLE]
                ?.let { runCatching { CardCornerStyle.valueOf(it) }.getOrDefault(CardCornerStyle.STANDARD) }
                ?: CardCornerStyle.STANDARD
        }

    val sensitivity: Flow<Sensitivity> = safePreferences
        .map { preferences ->
            preferences[Keys.SENSITIVITY]
                ?.let { runCatching { Sensitivity.valueOf(it) }.getOrDefault(Sensitivity.MEDIUM) }
                ?: Sensitivity.MEDIUM
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

    suspend fun setMaxSegmentDurationSec(value: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.MAX_SEGMENT_DURATION_SEC] = value.coerceIn(
                MIN_MAX_SEGMENT_DURATION_SEC,
                MAX_MAX_SEGMENT_DURATION_SEC
            )
        }
    }

    suspend fun setAccentColor(value: AccentColor) {
        dataStore.edit { preferences ->
            preferences[Keys.ACCENT_COLOR] = value.name
            preferences[Keys.CUSTOM_ACCENT_COLOR_ARGB] = value.defaultArgb
            preferences[Keys.DYNAMIC_COLOR_ENABLED] = false
        }
    }

    suspend fun setCustomAccentColorArgb(value: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.ACCENT_COLOR] = AccentColor.CUSTOM.name
            preferences[Keys.CUSTOM_ACCENT_COLOR_ARGB] = value or ALPHA_MASK
            preferences[Keys.DYNAMIC_COLOR_ENABLED] = false
        }
    }

    suspend fun setDeepSeekApiKey(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.DEEPSEEK_API_KEY] = value.trim()
        }
    }

    suspend fun setDeepSeekBaseUrl(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.DEEPSEEK_BASE_URL] = value.trim().ifBlank { DEFAULT_DEEPSEEK_BASE_URL }
        }
    }

    suspend fun setDeepSeekModelName(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.DEEPSEEK_MODEL_NAME] = value.trim().ifBlank { DEFAULT_DEEPSEEK_MODEL_NAME }
        }
    }

    suspend fun setAiCustomInfo(value: String) {
        dataStore.edit { preferences ->
            preferences[Keys.AI_CUSTOM_INFO] = value
        }
    }

    suspend fun setFontScale(value: FontScale) {
        dataStore.edit { preferences ->
            preferences[Keys.FONT_SCALE] = value.name
        }
    }

    suspend fun setCardCornerStyle(value: CardCornerStyle) {
        dataStore.edit { preferences ->
            preferences[Keys.CARD_CORNER_STYLE] = value.name
        }
    }

    suspend fun setSensitivity(value: Sensitivity) {
        dataStore.edit { preferences ->
            preferences[Keys.SENSITIVITY] = value.name
        }
    }

    private object Keys {
        val SILENCE_THRESHOLD_DB = floatPreferencesKey("silence_threshold_db")
        val AUTO_CLEAN_ENABLED = booleanPreferencesKey("auto_clean_enabled")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COMPACT_MODE_ENABLED = booleanPreferencesKey("compact_mode_enabled")
        val SHOW_TECHNICAL_DETAILS = booleanPreferencesKey("show_technical_details")
        val MAX_SEGMENT_DURATION_SEC = intPreferencesKey("max_segment_duration_sec")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val CUSTOM_ACCENT_COLOR_ARGB = intPreferencesKey("custom_accent_color_argb")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val DEEPSEEK_BASE_URL = stringPreferencesKey("deepseek_base_url")
        val DEEPSEEK_MODEL_NAME = stringPreferencesKey("deepseek_model_name")
        val AI_CUSTOM_INFO = stringPreferencesKey("ai_custom_info")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val CARD_CORNER_STYLE = stringPreferencesKey("card_corner_style")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
    }

    companion object {
        const val MIN_SILENCE_THRESHOLD_DB = -60f
        const val MAX_SILENCE_THRESHOLD_DB = -20f
        const val MIN_MAX_SEGMENT_DURATION_SEC = 15
        const val MAX_MAX_SEGMENT_DURATION_SEC = 120
        const val DEFAULT_SILENCE_THRESHOLD_DB = -40f
        const val DEFAULT_MAX_SEGMENT_DURATION_SEC = 60
        const val DEFAULT_AUTO_CLEAN_ENABLED = true
        const val DEFAULT_DYNAMIC_COLOR_ENABLED = true
        const val DEFAULT_COMPACT_MODE_ENABLED = false
        const val DEFAULT_SHOW_TECHNICAL_DETAILS = true
        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"
        const val DEFAULT_THEME_MODE = THEME_MODE_SYSTEM
        const val DEFAULT_CUSTOM_ACCENT_COLOR_ARGB = -10071900
        const val DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1/chat/completions"
        const val DEFAULT_DEEPSEEK_MODEL_NAME = "deepseek-chat"
        private const val ALPHA_MASK = -0x1000000
    }
}

val AccentColor.defaultArgb: Int
    get() = when (this) {
        AccentColor.INDIGO -> 0xFF6750A4.toInt()
        AccentColor.BLUE -> 0xFF2196F3.toInt()
        AccentColor.GREEN -> 0xFF4CAF50.toInt()
        AccentColor.ORANGE -> 0xFFFF9800.toInt()
        AccentColor.RED -> 0xFFF44336.toInt()
        AccentColor.CYAN -> 0xFF00BCD4.toInt()
        AccentColor.PINK -> 0xFFE91E63.toInt()
        AccentColor.CUSTOM -> SettingsPreferencesRepository.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB
    }
