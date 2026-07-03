package com.sleep.snore.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val silenceThresholdDb: Float = SettingsPreferencesRepository.DEFAULT_SILENCE_THRESHOLD_DB,
    val autoCleanEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_AUTO_CLEAN_ENABLED,
    val dynamicColorEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_DYNAMIC_COLOR_ENABLED,
    val themeMode: String = SettingsPreferencesRepository.DEFAULT_THEME_MODE,
    val storageUsageText: String = "计算中..."
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: SettingsPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        silenceThresholdDb = settings.silenceThresholdDb,
                        autoCleanEnabled = settings.autoCleanEnabled,
                        dynamicColorEnabled = settings.dynamicColorEnabled,
                        themeMode = settings.themeMode
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
}
