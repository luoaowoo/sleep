package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.WorkerParameters
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class HealthConnectSleepTriggerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsPreferencesRepository,
    private val healthConnectSleepTriggerSource: HealthConnectSleepTriggerSource,
    private val coordinator: AutoSnoreDetectionCoordinator
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.wearableSleepTriggerEnabled) return Result.success()

        val pollResult = runCatching {
            healthConnectSleepTriggerSource.pollLatestSleepSession(
                requireBackgroundRead = inputData.getBoolean(KEY_REQUIRE_BACKGROUND_READ, true)
            )
        }.getOrElse { throwable ->
            settingsRepository.setWearableSleepTriggerStatus(
                "Health Connect 读取失败：${throwable.message.orEmpty()}".trimEnd('：')
            )
            return Result.success()
        }

        val requireBackgroundRead = inputData.getBoolean(KEY_REQUIRE_BACKGROUND_READ, true)
        val status = if (pollResult is HealthConnectSleepTriggerSource.PollResult.EventEmitted) {
            handleEmittedEvent(pollResult, settings.wearableStopOnSleepEndEnabled)
        } else {
            pollResult.toStatusText(requireBackgroundRead)
        }
        settingsRepository.setWearableSleepTriggerStatus(status)
        return Result.success()
    }

    private suspend fun handleEmittedEvent(
        pollResult: HealthConnectSleepTriggerSource.PollResult.EventEmitted,
        stopOnSleepEnd: Boolean
    ): String {
        val result = coordinator.handleEvent(
            event = pollResult.event,
            enabled = true,
            stopOnSleepEnd = stopOnSleepEnd
        )
        if (result.shouldRememberEvent) {
            settingsRepository.setLastWearableSleepEventKey(pollResult.eventKey)
        }
        return result.statusText ?: pollResult.toStatusText(requireBackgroundRead = true)
    }

    private fun HealthConnectSleepTriggerSource.PollResult.toStatusText(requireBackgroundRead: Boolean): String {
        return when (this) {
            HealthConnectSleepTriggerSource.PollResult.HealthConnectUnavailable -> "Health Connect 不可用"
            HealthConnectSleepTriggerSource.PollResult.PermissionMissing -> if (requireBackgroundRead) {
                "缺少 Health Connect 睡眠/后台读取权限"
            } else {
                "缺少 Health Connect 睡眠读取权限"
            }
            HealthConnectSleepTriggerSource.PollResult.NoRecentSleep -> "未发现最近睡眠记录"
            HealthConnectSleepTriggerSource.PollResult.DuplicateEvent -> "最近睡眠记录已处理，等待新记录"
            HealthConnectSleepTriggerSource.PollResult.ReadFailed -> "Health Connect 读取失败"
            is HealthConnectSleepTriggerSource.PollResult.EventEmitted -> "已处理睡眠事件"
        }
    }

    companion object {
        const val WORK_NAME = "health_connect_sleep_trigger"
        const val ONE_TIME_WORK_NAME = "health_connect_sleep_trigger_now"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthConnectSleepTriggerWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<HealthConnectSleepTriggerWorker>()
                .setInputData(workDataOf(KEY_REQUIRE_BACKGROUND_READ to false))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private const val KEY_REQUIRE_BACKGROUND_READ = "require_background_read"
    }
}
