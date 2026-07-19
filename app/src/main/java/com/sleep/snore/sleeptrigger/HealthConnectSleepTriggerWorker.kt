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
import java.time.Instant
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
        val requireBackgroundRead = inputData.getBoolean(KEY_REQUIRE_BACKGROUND_READ, true)
        if (!settings.wearableSleepTriggerEnabled && requireBackgroundRead) return Result.success()
        val allowSleepStartFromThisCheck = requireBackgroundRead &&
            settings.wearableAutoStartOnSleepStartEnabled

        val pollResult = runCatching {
            healthConnectSleepTriggerSource.pollLatestSleepSession(
                now = Instant.now(),
                requireBackgroundRead = requireBackgroundRead,
                ignoreEventsBefore = sleepEventIgnoreEventsBefore(settings),
                emitOngoingSleepStart = allowSleepStartFromThisCheck
            )
        }.getOrElse { throwable ->
            settingsRepository.setWearableSleepTriggerStatus(
                "Health Connect 读取失败：${throwable.message.orEmpty()}".trimEnd('：')
            )
            return Result.success()
        }

        val handleResult = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = settings.wearableStopOnSleepEndEnabled,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = requireBackgroundRead,
            allowSleepStartRecording = allowSleepStartRecordingFromBackgroundCheck(
                allowSleepStartFromThisCheck
            )
        )
        settingsRepository.setWearableSleepTriggerStatus(handleResult.statusText)
        return Result.success()
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

        fun cancel(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
        }

        private const val KEY_REQUIRE_BACKGROUND_READ = "require_background_read"
    }
}
