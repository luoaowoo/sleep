package com.sleep.snore.recording

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ActiveRecordingFinalizerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val activeRecordingFinalizer: ActiveRecordingFinalizer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val expectedSource = inputData.getString(KEY_EXPECTED_SOURCE)
        activeRecordingFinalizer.finalizeIfActive(expectedTriggerSource = expectedSource)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "active_recording_finalizer"
        private const val KEY_EXPECTED_SOURCE = "expected_source"
        private const val FALLBACK_DELAY_SECONDS = 90L

        fun enqueueFallback(context: Context, expectedSource: String) {
            val request = OneTimeWorkRequestBuilder<ActiveRecordingFinalizerWorker>()
                .setInitialDelay(FALLBACK_DELAY_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_EXPECTED_SOURCE to expectedSource))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
