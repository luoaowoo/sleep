package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
        val pollResult = healthConnectSleepTriggerSource.pollLatestSleepSession()
        if (pollResult is HealthConnectSleepTriggerSource.PollResult.EventEmitted) {
            coordinator.handleEvent(
                event = pollResult.event,
                enabled = settings.wearableSleepTriggerEnabled,
                stopOnSleepEnd = settings.wearableStopOnSleepEndEnabled
            )
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "health_connect_sleep_trigger"

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
    }
}
