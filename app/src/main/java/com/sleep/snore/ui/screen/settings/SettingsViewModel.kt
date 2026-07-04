package com.sleep.snore.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.model.Sensitivity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val silenceThresholdDb: Float = SettingsPreferencesRepository.DEFAULT_SILENCE_THRESHOLD_DB,
    val autoCleanEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_AUTO_CLEAN_ENABLED,
    val dynamicColorEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_DYNAMIC_COLOR_ENABLED,
    val themeMode: String = SettingsPreferencesRepository.DEFAULT_THEME_MODE,
    val compactModeEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_COMPACT_MODE_ENABLED,
    val showTechnicalDetails: Boolean = SettingsPreferencesRepository.DEFAULT_SHOW_TECHNICAL_DETAILS,
    val maxSegmentDurationSec: Int = SettingsPreferencesRepository.DEFAULT_MAX_SEGMENT_DURATION_SEC,
    val customAccentColorArgb: Int = SettingsPreferencesRepository.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
    val storageUsageText: String = "计算中..."
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: SettingsPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val accentColor: StateFlow<AccentColor> = preferencesRepository.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentColor.INDIGO)

    val customAccentColorArgb: StateFlow<Int> = preferencesRepository.customAccentColorArgb
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsPreferencesRepository.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB
        )

    val fontScale: StateFlow<FontScale> = preferencesRepository.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontScale.STANDARD)

    val cardCornerStyle: StateFlow<CardCornerStyle> = preferencesRepository.cardCornerStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardCornerStyle.STANDARD)

    val sensitivity: StateFlow<Sensitivity> = preferencesRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Sensitivity.MEDIUM)

    init {
        viewModelScope.launch {
            preferencesRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        silenceThresholdDb = settings.silenceThresholdDb,
                        autoCleanEnabled = settings.autoCleanEnabled,
                        dynamicColorEnabled = settings.dynamicColorEnabled,
                        themeMode = settings.themeMode,
                        compactModeEnabled = settings.compactModeEnabled,
                        showTechnicalDetails = settings.showTechnicalDetails,
                        maxSegmentDurationSec = settings.maxSegmentDurationSec,
                        customAccentColorArgb = settings.customAccentColorArgb
                    )
                }
            }
        }
        refreshStorageUsage()
    }

    fun onSilenceThresholdChange(value: Float) {
        val threshold = value.coerceIn(
            SettingsPreferencesRepository.MIN_SILENCE_THRESHOLD_DB,
            SettingsPreferencesRepository.MAX_SILENCE_THRESHOLD_DB
        )
        _uiState.update { it.copy(silenceThresholdDb = threshold) }
        viewModelScope.launch {
            preferencesRepository.setSilenceThresholdDb(threshold)
        }
    }

    fun onAutoCleanChange(enabled: Boolean) {
        _uiState.update { it.copy(autoCleanEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setAutoCleanEnabled(enabled)
        }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        _uiState.update { it.copy(dynamicColorEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun onThemeModeChange(mode: String) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun onCompactModeChange(enabled: Boolean) {
        _uiState.update { it.copy(compactModeEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setCompactModeEnabled(enabled)
        }
    }

    fun onShowTechnicalDetailsChange(enabled: Boolean) {
        _uiState.update { it.copy(showTechnicalDetails = enabled) }
        viewModelScope.launch {
            preferencesRepository.setShowTechnicalDetails(enabled)
        }
    }

    fun onMaxSegmentDurationChange(value: Float) {
        val durationSec = value.toInt().coerceIn(
            SettingsPreferencesRepository.MIN_MAX_SEGMENT_DURATION_SEC,
            SettingsPreferencesRepository.MAX_MAX_SEGMENT_DURATION_SEC
        )
        _uiState.update { it.copy(maxSegmentDurationSec = durationSec) }
        viewModelScope.launch {
            preferencesRepository.setMaxSegmentDurationSec(durationSec)
        }
    }

    fun setAccentColor(value: AccentColor) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(value)
        }
    }

    fun setCustomAccentColorArgb(value: Int) {
        _uiState.update { it.copy(customAccentColorArgb = value or ALPHA_MASK) }
        viewModelScope.launch {
            preferencesRepository.setCustomAccentColorArgb(value)
        }
    }

    fun setFontScale(value: FontScale) {
        viewModelScope.launch {
            preferencesRepository.setFontScale(value)
        }
    }

    fun setCardCornerStyle(value: CardCornerStyle) {
        viewModelScope.launch {
            preferencesRepository.setCardCornerStyle(value)
        }
    }

    fun setSensitivity(value: Sensitivity) {
        viewModelScope.launch {
            preferencesRepository.setSensitivity(value)
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { calculateStorageBytes() }
            val megabytes = bytes / 1024.0 / 1024.0
            _uiState.update {
                it.copy(storageUsageText = String.format(Locale.getDefault(), "%.1f MB", megabytes))
            }
        }
    }

    private fun calculateStorageBytes(): Long {
        val audioDir = File(context.filesDir, "snore_audio")
        val audioBytes = audioDir.sizeBytes()
        val dbFile = context.getDatabasePath("sleep_snore.db")
        val dbBytes = listOf(
            dbFile,
            File("${dbFile.absolutePath}-wal"),
            File("${dbFile.absolutePath}-shm")
        ).sumOf { if (it.exists()) it.length() else 0L }
        return audioBytes + dbBytes
    }

    private fun File.sizeBytes(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private companion object {
        const val ALPHA_MASK = -0x1000000
    }
}
