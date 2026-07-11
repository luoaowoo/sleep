package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.service.SleepRecordingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class BedtimeDetectionReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsPreferencesRepository,
    private val sleepRepository: SleepRepository,
    private val notifier: BedtimeDetectionReminderNotifier
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.bedtimeReminderEnabled) return Result.success()

        if (!isDetectionActive()) {
            notifier.notifyBedtimeDetectionReminder()
        }
        enqueueNext(applicationContext, settings.bedtimeReminderMinuteOfDay)
        return Result.success()
    }

    private suspend fun isDetectionActive(): Boolean {
        return SleepRecordingService.recordingState.value.isActive ||
            WearableSleepStandbyService.standbyState.value.isActive ||
            sleepRepository.getActiveRecordingRecord() != null
    }

    companion object {
        const val WORK_NAME = "bedtime_detection_reminder"

        fun enqueueNext(context: Context, minuteOfDay: Int) {
            val delayMillis = nextBedtimeReminderDelayMillis(
                nowMillis = System.currentTimeMillis(),
                minuteOfDay = minuteOfDay
            )
            val request = OneTimeWorkRequestBuilder<BedtimeDetectionReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
