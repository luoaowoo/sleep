package com.sleep.snore.ui.screen.settings

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.model.Sensitivity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.preferences.defaultArgb
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.sleeptrigger.BedtimeDetectionReminderWorker
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerWorker
import com.sleep.snore.sleeptrigger.WearableSleepStandbyService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
    val deepSeekApiKey: String = "",
    val deepSeekBaseUrl: String = SettingsPreferencesRepository.DEFAULT_DEEPSEEK_BASE_URL,
    val deepSeekModelName: String = SettingsPreferencesRepository.DEFAULT_DEEPSEEK_MODEL_NAME,
    val aiCustomInfo: String = "",
    val wearableSleepTriggerEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_WEARABLE_SLEEP_TRIGGER_ENABLED,
    val wearableStopOnSleepEndEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_WEARABLE_STOP_ON_SLEEP_END_ENABLED,
    val bedtimeReminderEnabled: Boolean = SettingsPreferencesRepository.DEFAULT_BEDTIME_REMINDER_ENABLED,
    val bedtimeReminderMinuteOfDay: Int = SettingsPreferencesRepository.DEFAULT_BEDTIME_REMINDER_MINUTE_OF_DAY,
    val bedtimeReminderTimeText: String = SettingsPreferencesRepository.DEFAULT_BEDTIME_REMINDER_MINUTE_OF_DAY.toMinuteOfDayText(),
    val wearableSleepTriggerStatus: String = SettingsPreferencesRepository.DEFAULT_WEARABLE_SLEEP_TRIGGER_STATUS,
    val wearableSleepTriggerLastCheckText: String = "尚未检查",
    val latestWearableSleepSessionText: String = "尚未发现同步睡眠记录",
    val latestWearableSleepSessionStartMillis: Long = 0L,
    val latestWearableSleepSessionEndMillis: Long = 0L,
    val latestWearableSleepSessionStatus: String = "",
    val latestWearableSleepSessionSourcePackage: String = "",
    val activeRecordingTriggerSource: String = "",
    val activeRecordingTriggerStartedAtText: String = "无",
    val activeRecordingTriggerStartedAtMillis: Long = 0L,
    val storageUsageText: String = "计算中..."
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: SettingsPreferencesRepository,
    private val sleepRepository: SleepRepository,
    private val recordingController: RecordingController,
    private val wearableStandbyPrerequisiteChecker: WearableStandbyPrerequisiteChecker
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
                        customAccentColorArgb = settings.customAccentColorArgb,
                        deepSeekApiKey = settings.deepSeekApiKey,
                        deepSeekBaseUrl = settings.deepSeekBaseUrl,
                        deepSeekModelName = settings.deepSeekModelName,
                        aiCustomInfo = settings.aiCustomInfo,
                        wearableSleepTriggerEnabled = settings.wearableSleepTriggerEnabled,
                        wearableStopOnSleepEndEnabled = settings.wearableStopOnSleepEndEnabled,
                        bedtimeReminderEnabled = settings.bedtimeReminderEnabled,
                        bedtimeReminderMinuteOfDay = settings.bedtimeReminderMinuteOfDay,
                        bedtimeReminderTimeText = settings.bedtimeReminderMinuteOfDay.toMinuteOfDayText(),
                        wearableSleepTriggerStatus = settings.wearableSleepTriggerStatus,
                        wearableSleepTriggerLastCheckText = settings.wearableSleepTriggerLastCheckMillis.toLastCheckText(),
                        latestWearableSleepSessionText = toSleepSessionText(
                            startMillis = settings.latestWearableSleepSessionStartMillis,
                            endMillis = settings.latestWearableSleepSessionEndMillis,
                            status = settings.latestWearableSleepSessionStatus
                        ),
                        latestWearableSleepSessionStartMillis = settings.latestWearableSleepSessionStartMillis,
                        latestWearableSleepSessionEndMillis = settings.latestWearableSleepSessionEndMillis,
                        latestWearableSleepSessionStatus = settings.latestWearableSleepSessionStatus,
                        latestWearableSleepSessionSourcePackage = settings.latestWearableSleepSessionSourcePackage,
                        activeRecordingTriggerSource = settings.activeRecordingTriggerSource,
                        activeRecordingTriggerStartedAtText = settings.activeRecordingTriggerStartedAtMillis.toTriggerStartText(),
                        activeRecordingTriggerStartedAtMillis = settings.activeRecordingTriggerStartedAtMillis
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
        _uiState.update {
            it.copy(
                dynamicColorEnabled = false,
                customAccentColorArgb = value.defaultArgb
            )
        }
        viewModelScope.launch {
            preferencesRepository.setAccentColor(value)
        }
    }

    fun setCustomAccentColorArgb(value: Int) {
        _uiState.update {
            it.copy(
                dynamicColorEnabled = false,
                customAccentColorArgb = value or ALPHA_MASK
            )
        }
        viewModelScope.launch {
            preferencesRepository.setCustomAccentColorArgb(value)
        }
    }

    fun onDeepSeekApiKeyChange(value: String) {
        _uiState.update { it.copy(deepSeekApiKey = value) }
        viewModelScope.launch {
            preferencesRepository.setDeepSeekApiKey(value)
        }
    }

    fun onDeepSeekBaseUrlChange(value: String) {
        _uiState.update { it.copy(deepSeekBaseUrl = value) }
        viewModelScope.launch {
            preferencesRepository.setDeepSeekBaseUrl(value)
        }
    }

    fun onDeepSeekModelNameChange(value: String) {
        _uiState.update { it.copy(deepSeekModelName = value) }
        viewModelScope.launch {
            preferencesRepository.setDeepSeekModelName(value)
        }
    }

    fun onAiCustomInfoChange(value: String) {
        _uiState.update { it.copy(aiCustomInfo = value) }
        viewModelScope.launch {
            preferencesRepository.setAiCustomInfo(value)
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

    fun onWearableSleepTriggerChange(enabled: Boolean) {
        _uiState.update { it.copy(wearableSleepTriggerEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setWearableSleepTriggerEnabled(enabled)
            if (enabled) {
                preferencesRepository.setWearableSleepTriggerStatus(PERIODIC_CHECK_ENABLED_STATUS)
                HealthConnectSleepTriggerWorker.enqueue(context)
                HealthConnectSleepTriggerWorker.enqueueNow(context)
            } else {
                runCatching { HealthConnectSleepTriggerWorker.cancel(context) }
                runCatching {
                    context.startService(WearableSleepStandbyService.stopIntent(context))
                }
                recordingController.stopFromSleepTrigger(HealthConnectSleepTriggerSource.SOURCE)
                preferencesRepository.setWearableSleepTriggerMessage("Health Connect 周期检查已关闭")
            }
        }
    }

    fun onHealthConnectPermissionsResult(grantedPermissions: Set<String>) {
        when {
            grantedPermissions.containsAll(HealthConnectSleepTriggerSource.BACKGROUND_REQUIRED_PERMISSIONS) -> {
                checkWearableSleepNow("Health Connect 已授权，正在检查最近睡眠记录；这不会开始录音，睡前仍需开启前台检测")
            }
            grantedPermissions.containsAll(HealthConnectSleepTriggerSource.FOREGROUND_REQUIRED_PERMISSIONS) -> {
                checkWearableSleepNow("已授权睡眠读取，正在检查最近睡眠记录；这不会开始录音，后台轮询仍需后台读取权限")
            }
            else -> {
                viewModelScope.launch {
                    preferencesRepository.setWearableSleepTriggerMessage("未授予 Health Connect 睡眠读取权限，无法检查睡眠记录")
                }
            }
        }
    }

    fun checkWearableSleepNow(
        status: String = "正在检查最近睡眠记录；这不会开始录音，睡前仍需开启前台检测"
    ) {
        _uiState.update {
            it.copy(
                wearableSleepTriggerEnabled = true,
                wearableSleepTriggerStatus = status
            )
        }
        viewModelScope.launch {
            preferencesRepository.setWearableSleepTriggerEnabled(true)
            preferencesRepository.setWearableSleepTriggerStatus(status)
            HealthConnectSleepTriggerWorker.enqueue(context)
            HealthConnectSleepTriggerWorker.enqueueNow(context)
        }
    }

    fun onWearableStopOnSleepEndChange(enabled: Boolean) {
        _uiState.update { it.copy(wearableStopOnSleepEndEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setWearableStopOnSleepEndEnabled(enabled)
        }
    }

    fun onBedtimeReminderChange(enabled: Boolean) {
        _uiState.update { it.copy(bedtimeReminderEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.setBedtimeReminderEnabled(enabled)
            if (enabled) {
                BedtimeDetectionReminderWorker.enqueueNext(
                    context = context,
                    minuteOfDay = _uiState.value.bedtimeReminderMinuteOfDay
                )
            } else {
                BedtimeDetectionReminderWorker.cancel(context)
            }
        }
    }

    fun adjustBedtimeReminderMinutes(deltaMinutes: Int) {
        val nextMinuteOfDay = Math.floorMod(
            _uiState.value.bedtimeReminderMinuteOfDay + deltaMinutes,
            MINUTES_PER_DAY
        )
        _uiState.update {
            it.copy(
                bedtimeReminderMinuteOfDay = nextMinuteOfDay,
                bedtimeReminderTimeText = nextMinuteOfDay.toMinuteOfDayText()
            )
        }
        viewModelScope.launch {
            preferencesRepository.setBedtimeReminderMinuteOfDay(nextMinuteOfDay)
            if (_uiState.value.bedtimeReminderEnabled) {
                BedtimeDetectionReminderWorker.enqueueNext(context, nextMinuteOfDay)
            }
        }
    }

    fun startWearableSleepStandby() {
        _uiState.update {
            it.copy(
                wearableSleepTriggerEnabled = true,
                wearableSleepTriggerStatus = "睡前前台检测正在开启，录音服务将等待 Health Connect 睡眠结束"
            )
        }
        viewModelScope.launch {
            val blocker = wearableStandbyPrerequisiteChecker.startBlocker()
            if (blocker != null) {
                _uiState.update {
                    it.copy(
                        wearableSleepTriggerEnabled = true,
                        wearableSleepTriggerStatus = blocker
                    )
                }
                preferencesRepository.setWearableSleepTriggerEnabled(true)
                preferencesRepository.setWearableSleepTriggerStatus(blocker)
                return@launch
            }
            preferencesRepository.setWearableSleepTriggerEnabled(true)
            preferencesRepository.setWearableSleepTriggerStatus("睡前前台检测正在开启，录音服务将等待 Health Connect 睡眠结束")
            val recordingStartResult = recordingController.startFromSleepTrigger(
                HealthConnectSleepTriggerSource.SOURCE
            )
            if (!recordingStartResult.confirmed) {
                _uiState.update { state ->
                    state.copy(wearableSleepTriggerStatus = recordingStartResult.statusText)
                }
                preferencesRepository.setWearableSleepTriggerStatus(recordingStartResult.statusText)
                return@launch
            }
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    WearableSleepStandbyService.startIntent(context)
                )
            }
            val status = if (_uiState.value.wearableStopOnSleepEndEnabled) {
                "睡前前台检测已开启，录音服务将低频检查 Health Connect 睡眠结束"
            } else {
                "睡前前台检测已开启，但自动停录已关闭；睡醒后请手动停止"
            }
            _uiState.update { state -> state.copy(wearableSleepTriggerStatus = status) }
            preferencesRepository.setWearableSleepTriggerStatus(status)
        }
    }

    fun stopWearableSleepStandby() {
        runCatching {
            context.startService(WearableSleepStandbyService.stopIntent(context))
        }
        viewModelScope.launch {
            val stoppedRecording = recordingController.stopFromSleepTrigger(
                HealthConnectSleepTriggerSource.SOURCE
            )
            val status = if (stoppedRecording) {
                "睡前前台检测已停止，鼾声检测也已请求停止"
            } else {
                "睡前前台检测已停止"
            }
            _uiState.update { it.copy(wearableSleepTriggerStatus = status) }
            preferencesRepository.setWearableSleepTriggerMessage(status)
        }
    }

    suspend fun collectWearableDatabaseDiagnostics(): String {
        return collectWearableDatabaseDiagnostics(
            sleepRepository = sleepRepository,
            settingsRepository = preferencesRepository
        )
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

    private fun Long.toLastCheckText(): String {
        if (this <= 0L) return "尚未检查"
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(this))
    }

    private fun Long.toTriggerStartText(): String {
        if (this <= 0L) return "无"
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(this))
    }

    private fun toSleepSessionText(startMillis: Long, endMillis: Long, status: String): String {
        if (startMillis <= 0L || endMillis <= 0L) return "尚未发现同步睡眠记录"
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val startText = formatter.format(Date(startMillis))
        val endText = formatter.format(Date(endMillis))
        return if (status.isBlank()) {
            "$startText - $endText"
        } else {
            "$startText - $endText（$status）"
        }
    }

    private companion object {
        const val ALPHA_MASK = -0x1000000
        const val MINUTES_PER_DAY = 24 * 60
        const val PERIODIC_CHECK_ENABLED_STATUS = "已开启 Health Connect 周期检查；它不会开始录音，睡前请点击前台检测"
    }
}

private fun Int.toMinuteOfDayText(): String {
    val safeMinute = coerceIn(
        SettingsPreferencesRepository.MIN_BEDTIME_REMINDER_MINUTE_OF_DAY,
        SettingsPreferencesRepository.MAX_BEDTIME_REMINDER_MINUTE_OF_DAY
    )
    return "%02d:%02d".format(Locale.getDefault(), safeMinute / 60, safeMinute % 60)
}
