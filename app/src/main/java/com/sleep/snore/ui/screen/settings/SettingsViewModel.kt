package com.sleep.snore.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val silenceThresholdDb: Float = SettingsPreferencesRepository.DEFAULT_SILENCE_THRESHOLD_DB,
    val autoCleanEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_AUTO_CLEAN_ENABLED
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: SettingsPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.settings.collect { settings ->
                _uiState.value = SettingsUiState(
                    silenceThresholdDb = settings.silenceThresholdDb,
                    autoCleanEnabled = settings.autoCleanEnabled
                )
            }
        }
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
}
