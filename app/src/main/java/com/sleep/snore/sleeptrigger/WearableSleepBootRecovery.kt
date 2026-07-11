package com.sleep.snore.sleeptrigger

import android.content.Context
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.recording.ActiveRecordingFinalizerWorker
import kotlinx.coroutines.flow.first

internal enum class WearableRestartRecoveryEntryPoint {
    BootCompleted,
    AppStart
}

internal data class WearableRestartRecoveryPlan(
    val expectedSource: String,
    val statusText: String
)

internal fun shouldFinalizeWearableRecordingAfterBoot(
    settings: SettingsPreferences,
    activeRecord: SleepRecordEntity?
): Boolean {
    return shouldFinalizeWearableRecordingAfterRestart(
        activeRecordExists = activeRecord != null,
        activeRecordingTriggerSource = settings.activeRecordingTriggerSource
    )
}

internal fun shouldFinalizeWearableRecordingAfterRestart(
    activeRecordExists: Boolean,
    activeRecordingTriggerSource: String
): Boolean {
    return activeRecordExists && activeRecordingTriggerSource == HealthConnectSleepTriggerSource.SOURCE
}

internal fun wearableRestartRecoveryPlan(
    entryPoint: WearableRestartRecoveryEntryPoint,
    activeRecordExists: Boolean,
    activeRecordingTriggerSource: String
): WearableRestartRecoveryPlan? {
    if (!shouldFinalizeWearableRecordingAfterRestart(activeRecordExists, activeRecordingTriggerSource)) return null
    return WearableRestartRecoveryPlan(
        expectedSource = HealthConnectSleepTriggerSource.SOURCE,
        statusText = when (entryPoint) {
            WearableRestartRecoveryEntryPoint.BootCompleted -> {
                "检测到重启/更新后有未完成的手环鼾声记录，已安排兜底结算"
            }
            WearableRestartRecoveryEntryPoint.AppStart -> {
                "检测到应用启动后有未完成的手环鼾声记录，已安排兜底结算"
            }
        }
    )
}

internal suspend fun recoverWearableRecordingAfterRestartIfNeeded(
    context: Context,
    settingsRepository: SettingsPreferencesRepository,
    sleepRepository: SleepRepository,
    entryPoint: WearableRestartRecoveryEntryPoint,
    enqueueFinalizer: (Context, String) -> Unit = { appContext, expectedSource ->
        ActiveRecordingFinalizerWorker.enqueueFallback(
            context = appContext,
            expectedSource = expectedSource
        )
    }
): Boolean {
    val settings = settingsRepository.settings.first()
    val activeRecord = sleepRepository.getActiveRecordingRecord()
    val plan = wearableRestartRecoveryPlan(
        entryPoint = entryPoint,
        activeRecordExists = activeRecord != null,
        activeRecordingTriggerSource = settings.activeRecordingTriggerSource
    ) ?: return false
    enqueueFinalizer(context.applicationContext, plan.expectedSource)
    settingsRepository.setWearableSleepTriggerStatus(plan.statusText)
    return true
}
