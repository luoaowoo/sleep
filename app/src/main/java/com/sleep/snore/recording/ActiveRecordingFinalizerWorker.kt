package com.sleep.snore.recording

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ActiveRecordingFinalizerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val activeRecordingFinalizer: ActiveRecordingFinalizer,
    private val sleepRepository: SleepRepository,
    private val settingsRepository: SettingsPreferencesRepository,
    private val wearableSleepEndTimeResolver: WearableSleepEndTimeResolver
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val expectedSource = inputData.getString(KEY_EXPECTED_SOURCE)
        val activeRecord = sleepRepository.getActiveRecordingRecord()
        val inputSleepEndTimeMillis = inputData.getLong(KEY_SLEEP_END_TIME_MILLIS, 0L)
            .takeIf { it > 0L }
        val expectedActiveRecordingStartMillis = inputData.getLong(KEY_ACTIVE_RECORDING_START_MILLIS, 0L)
            .takeIf { it > 0L }
        if (
            shouldSkipHealthConnectFinalizerWithoutToken(
                expectedSource = expectedSource,
                activeRecordExists = activeRecord != null,
                expectedActiveRecordingStartMillis = expectedActiveRecordingStartMillis
            )
        ) {
            return Result.success()
        }
        if (
            shouldSkipFinalizerForDifferentActiveRecording(
                activeRecordStartMillis = activeRecord?.startTime,
                expectedActiveRecordingStartMillis = expectedActiveRecordingStartMillis
            )
        ) {
            return Result.success()
        }
        val wearableSleepEndResolveResult = if (
            expectedSource == HealthConnectSleepTriggerSource.SOURCE &&
            activeRecord != null &&
            inputSleepEndTimeMillis == null
        ) {
            wearableSleepEndTimeResolver.resolveResult(activeRecord)
        } else {
            null
        }
        val resolvedWearableSleepEnd =
            (wearableSleepEndResolveResult as? WearableSleepEndResolveResult.Resolved)?.sleepEnd
        if (wearableSleepEndResolveResult == WearableSleepEndResolveResult.NotWearableRecording) {
            return Result.success()
        }
        if (
            shouldRetryWearableFinalizer(
                expectedSource = expectedSource,
                activeRecordExists = activeRecord != null,
                inputSleepEndTimeMillis = inputSleepEndTimeMillis,
                resolvedWearableSleepEnd = resolvedWearableSleepEnd,
                resolveResult = wearableSleepEndResolveResult,
                runAttemptCount = runAttemptCount
            )
        ) {
            settingsRepository.setWearableSleepTriggerStatus(
                wearableFinalizerRetryStatus(wearableSleepEndResolveResult)
            )
            return Result.retry()
        }
        val finalized = activeRecordingFinalizer.finalizeIfActive(
            expectedTriggerSource = expectedSource,
            expectedActiveRecordingStartMillis = expectedActiveRecordingStartMillis,
            endTimeMillis = wearableFallbackEndTimeMillis(
                inputSleepEndTimeMillis = inputSleepEndTimeMillis,
                resolvedSleepEndTimeMillis = resolvedWearableSleepEnd?.endTimeMillis,
                fallbackNowMillis = System.currentTimeMillis(),
                activeRecordingStartMillis = activeRecord?.startTime,
                maxRecordingDurationMillis = MAX_WEARABLE_RECORDING_DURATION_MS
            )
        )
        if (finalized && expectedSource == HealthConnectSleepTriggerSource.SOURCE) {
            if (inputSleepEndTimeMillis != null) {
                settingsRepository.setWearableSleepTriggerStatus("已按 Health Connect 睡眠结束时间兜底结算")
            } else if (resolvedWearableSleepEnd != null) {
                settingsRepository.setLastWearableSleepEventKey(resolvedWearableSleepEnd.eventKey)
                settingsRepository.setWearableSleepTriggerStatus("已按 Health Connect 睡眠结束时间兜底结算")
            } else {
                settingsRepository.setWearableSleepTriggerStatus("多次未能读取 Health Connect 睡眠结束时间，已按当前时间截断兜底结算")
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "active_recording_finalizer"
        private const val KEY_EXPECTED_SOURCE = "expected_source"
        private const val KEY_SLEEP_END_TIME_MILLIS = "sleep_end_time_millis"
        private const val KEY_ACTIVE_RECORDING_START_MILLIS = "active_recording_start_millis"
        private const val FALLBACK_DELAY_SECONDS = 90L

        fun enqueueFallback(
            context: Context,
            expectedSource: String,
            sleepEndTimeMillis: Long? = null,
            activeRecordingStartMillis: Long? = null
        ) {
            val request = OneTimeWorkRequestBuilder<ActiveRecordingFinalizerWorker>()
                .setInitialDelay(FALLBACK_DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    HEALTH_CONNECT_RESOLVE_BACKOFF_MINUTES,
                    TimeUnit.MINUTES
                )
                .setInputData(
                    workDataOf(
                        KEY_EXPECTED_SOURCE to expectedSource,
                        KEY_SLEEP_END_TIME_MILLIS to (sleepEndTimeMillis ?: 0L),
                        KEY_ACTIVE_RECORDING_START_MILLIS to (activeRecordingStartMillis ?: 0L)
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                activeRecordingFinalizerExistingWorkPolicy(
                    sleepEndTimeMillis = sleepEndTimeMillis,
                    activeRecordingStartMillis = activeRecordingStartMillis
                ),
                request
            )
        }
    }
}

internal const val MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS = 8
internal const val HEALTH_CONNECT_RESOLVE_BACKOFF_MINUTES = 15L
internal const val MAX_WEARABLE_RECORDING_DURATION_MS = 16L * 60L * 60L * 1000L

internal fun shouldRetryWearableFinalizer(
    expectedSource: String?,
    activeRecordExists: Boolean,
    inputSleepEndTimeMillis: Long?,
    resolvedWearableSleepEnd: ResolvedWearableSleepEnd?,
    resolveResult: WearableSleepEndResolveResult? = null,
    runAttemptCount: Int
): Boolean {
    if (
        expectedSource != HealthConnectSleepTriggerSource.SOURCE ||
        !activeRecordExists ||
        inputSleepEndTimeMillis != null ||
        resolvedWearableSleepEnd != null ||
        resolveResult == WearableSleepEndResolveResult.NotWearableRecording
    ) {
        return false
    }
    return runAttemptCount < MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS
}

internal fun wearableFinalizerRetryStatus(resolveResult: WearableSleepEndResolveResult?): String {
    return when (resolveResult) {
        WearableSleepEndResolveResult.PermissionMissing -> {
            "缺少 Health Connect 睡眠/后台读取权限，恢复授权后继续等待同步兜底结算"
        }
        WearableSleepEndResolveResult.BackgroundReadUnavailable -> {
            "当前设备或 Health Connect 版本不支持后台读取，将继续等待；若仍不支持，最终按安全上限兜底结算"
        }
        WearableSleepEndResolveResult.ReadFailed -> {
            "Health Connect 读取失败，将继续等待同步后兜底结算"
        }
        else -> "尚未读取到 Health Connect 睡眠结束时间，将继续等待同步后兜底结算"
    }
}

internal fun activeRecordingFinalizerExistingWorkPolicy(
    sleepEndTimeMillis: Long?,
    activeRecordingStartMillis: Long? = null
): ExistingWorkPolicy {
    return if (
        sleepEndTimeMillis != null && sleepEndTimeMillis > 0L ||
        activeRecordingStartMillis != null && activeRecordingStartMillis > 0L
    ) {
        ExistingWorkPolicy.REPLACE
    } else {
        ExistingWorkPolicy.KEEP
    }
}

internal fun wearableFallbackEndTimeMillis(
    inputSleepEndTimeMillis: Long?,
    resolvedSleepEndTimeMillis: Long?,
    fallbackNowMillis: Long,
    activeRecordingStartMillis: Long? = null,
    maxRecordingDurationMillis: Long = MAX_WEARABLE_RECORDING_DURATION_MS
): Long {
    val selectedEndTime = inputSleepEndTimeMillis
        ?: resolvedSleepEndTimeMillis
        ?: fallbackNowMillis
    if (activeRecordingStartMillis == null ||
        activeRecordingStartMillis <= 0L ||
        maxRecordingDurationMillis <= 0L
    ) {
        return selectedEndTime
    }
    return selectedEndTime.coerceAtMost(activeRecordingStartMillis + maxRecordingDurationMillis)
}

internal fun shouldSkipFinalizerForDifferentActiveRecording(
    activeRecordStartMillis: Long?,
    expectedActiveRecordingStartMillis: Long?
): Boolean {
    if (expectedActiveRecordingStartMillis == null || expectedActiveRecordingStartMillis <= 0L) return false
    return activeRecordStartMillis != expectedActiveRecordingStartMillis
}

internal fun shouldSkipHealthConnectFinalizerWithoutToken(
    expectedSource: String?,
    activeRecordExists: Boolean,
    expectedActiveRecordingStartMillis: Long?
): Boolean {
    if (expectedSource != HealthConnectSleepTriggerSource.SOURCE) return false
    if (!activeRecordExists) return false
    return expectedActiveRecordingStartMillis == null || expectedActiveRecordingStartMillis <= 0L
}
