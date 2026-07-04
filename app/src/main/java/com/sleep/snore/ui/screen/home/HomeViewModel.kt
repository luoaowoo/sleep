package com.sleep.snore.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.remote.DeepSeekClient
import com.sleep.snore.data.remote.DeepSeekConfig
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.domain.WeeklyReport
import com.sleep.snore.domain.WeeklyReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeeklyReportUiState(
    val report: WeeklyReport? = null,
    val isGenerating: Boolean = false,
    val usesRemoteAi: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val settingsRepository: SettingsPreferencesRepository,
    private val weeklyReportGenerator: WeeklyReportGenerator,
    private val deepSeekClient: DeepSeekClient
) : ViewModel() {

    val latestRecord: StateFlow<SleepRecordEntity?> = repository.getLatestRecordFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentRecords: StateFlow<List<SleepRecordEntity>> = repository.getRecentRecords(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weeklyReportState = MutableStateFlow(WeeklyReportUiState())
    val weeklyReportState: StateFlow<WeeklyReportUiState> = _weeklyReportState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(recentRecords, settingsRepository.settings) { records, settings ->
                weeklyReportGenerator.generate(records, settings.aiCustomInfo)
            }.collect { report ->
                _weeklyReportState.update {
                    if (it.isGenerating) it else it.copy(report = report, usesRemoteAi = false, errorMessage = null)
                }
            }
        }
    }

    fun generateDeepSeekWeeklyReport() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val config = DeepSeekConfig(
                apiKey = settings.deepSeekApiKey,
                baseUrl = settings.deepSeekBaseUrl,
                modelName = settings.deepSeekModelName
            )
            val records = recentRecords.value
            val localReport = weeklyReportGenerator.generate(records, settings.aiCustomInfo)
            if (!config.isConfigured) {
                _weeklyReportState.value = WeeklyReportUiState(
                    report = localReport,
                    errorMessage = "请先在设置里填写 DeepSeek API Key、Base URL 和模型名。"
                )
                return@launch
            }

            _weeklyReportState.value = _weeklyReportState.value.copy(
                report = localReport,
                isGenerating = true,
                errorMessage = null
            )

            deepSeekClient.analyze(localReport.prompt, config)
                .onSuccess { aiSummary ->
                    _weeklyReportState.value = WeeklyReportUiState(
                        report = weeklyReportGenerator.generate(records, settings.aiCustomInfo, aiSummary),
                        usesRemoteAi = true
                    )
                }
                .onFailure { error ->
                    _weeklyReportState.value = WeeklyReportUiState(
                        report = localReport,
                        errorMessage = error.message ?: "DeepSeek 分析失败，已保留本地总结。"
                    )
                }
        }
    }
}
