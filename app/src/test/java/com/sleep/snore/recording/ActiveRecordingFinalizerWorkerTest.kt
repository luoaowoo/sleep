package com.sleep.snore.recording

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ActiveRecordingFinalizerWorkerTest {

    @Test
    fun doWork_prefersInputSleepEndTimeAndDoesNotResolveHealthConnectAgain() = runTest {
        val fixture = createFixture(
            inputSleepEndTimeMillis = 8_000L,
            activeRecord = activeRecord()
        )
        coEvery {
            fixture.activeRecordingFinalizer.finalizeIfActive(
                expectedTriggerSource = HealthConnectSleepTriggerSource.SOURCE,
                endTimeMillis = 8_000L
            )
        } returns true

        val result = fixture.worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { fixture.wearableSleepEndTimeResolver.resolveResult(any()) }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("Health Connect") },
                any()
            )
        }
        coVerify(exactly = 0) { fixture.settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun doWork_usesResolvedHealthConnectEndTimeAndStoresEventKey() = runTest {
        val fixture = createFixture(activeRecord = activeRecord())
        coEvery {
            fixture.wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.Resolved(
            ResolvedWearableSleepEnd(
                endTimeMillis = 9_000L,
                eventKey = "SleepEnded:9000:1000"
            )
        )
        coEvery {
            fixture.activeRecordingFinalizer.finalizeIfActive(
                expectedTriggerSource = HealthConnectSleepTriggerSource.SOURCE,
                endTimeMillis = 9_000L
            )
        } returns true

        val result = fixture.worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify { fixture.wearableSleepEndTimeResolver.resolveResult(activeRecord()) }
        coVerify { fixture.settingsRepository.setLastWearableSleepEventKey("SleepEnded:9000:1000") }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("Health Connect") },
                any()
            )
        }
    }

    @Test
    fun doWork_whenFinalizerDoesNotFinalizeDoesNotWriteWearableStatus() = runTest {
        val fixture = createFixture(activeRecord = activeRecord())
        coEvery {
            fixture.wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.Resolved(
            ResolvedWearableSleepEnd(
                endTimeMillis = 9_000L,
                eventKey = "SleepEnded:9000:1000"
            )
        )
        coEvery {
            fixture.activeRecordingFinalizer.finalizeIfActive(any(), any())
        } returns false

        val result = fixture.worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { fixture.settingsRepository.setWearableSleepTriggerStatus(any(), any()) }
        coVerify(exactly = 0) { fixture.settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun doWork_retriesWhenHealthConnectEndTimeIsNotSyncedYet() = runTest {
        val fixture = createFixture(activeRecord = activeRecord())
        coEvery {
            fixture.wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.WaitingForSync

        val result = fixture.worker(runAttemptCount = 0).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify { fixture.wearableSleepEndTimeResolver.resolveResult(activeRecord()) }
        coVerify(exactly = 0) { fixture.activeRecordingFinalizer.finalizeIfActive(any(), any()) }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("继续等待同步") },
                any()
            )
        }
    }

    @Test
    fun doWork_afterMaxMissingHealthConnectEndAttemptsFallsBackToNow() = runTest {
        val fixture = createFixture(activeRecord = activeRecord())
        coEvery {
            fixture.wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.WaitingForSync
        coEvery {
            fixture.activeRecordingFinalizer.finalizeIfActive(
                expectedTriggerSource = HealthConnectSleepTriggerSource.SOURCE,
                endTimeMillis = any()
            )
        } returns true

        val result = fixture.worker(runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify { fixture.activeRecordingFinalizer.finalizeIfActive(HealthConnectSleepTriggerSource.SOURCE, any()) }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("多次未能读取") && it.contains("当前时间") },
                any()
            )
        }
    }

    @Test
    fun doWork_permissionMissingKeepsRetryingWithoutCurrentTimeTruncation() = runTest {
        val fixture = createFixture(activeRecord = activeRecord())
        coEvery {
            fixture.wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.PermissionMissing

        val result = fixture.worker(runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify(exactly = 0) { fixture.activeRecordingFinalizer.finalizeIfActive(any(), any()) }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("缺少 Health Connect") && it.contains("恢复授权") },
                any()
            )
        }
    }

    @Test
    fun doWork_readFailedKeepsRetryingWithoutCurrentTimeTruncation() = runTest {
        val fixture = createFixture(activeRecord = activeRecord())
        coEvery {
            fixture.wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.ReadFailed

        val result = fixture.worker(runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify(exactly = 0) { fixture.activeRecordingFinalizer.finalizeIfActive(any(), any()) }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("读取失败") && it.contains("继续等待") },
                any()
            )
        }
    }

    @Test
    fun doWork_skipsOldFinalizerWhenActiveRecordingStartDoesNotMatch() = runTest {
        val fixture = createFixture(
            inputSleepEndTimeMillis = 8_000L,
            activeRecordingStartMillis = 1_000L,
            activeRecord = activeRecord(startTime = 10_000L)
        )

        val result = fixture.worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { fixture.wearableSleepEndTimeResolver.resolveResult(any()) }
        coVerify(exactly = 0) { fixture.activeRecordingFinalizer.finalizeIfActive(any(), any()) }
        coVerify(exactly = 0) { fixture.settingsRepository.setWearableSleepTriggerStatus(any(), any()) }
    }

    @Test
    fun shouldRetryWearableFinalizer_waitsUntilMaxAttemptWindow() {
        assertThat(
            shouldRetryWearableFinalizer(
                expectedSource = HealthConnectSleepTriggerSource.SOURCE,
                activeRecordExists = true,
                inputSleepEndTimeMillis = null,
                resolvedWearableSleepEnd = null,
                runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS - 1
            )
        ).isTrue()
        assertThat(
            shouldRetryWearableFinalizer(
                expectedSource = HealthConnectSleepTriggerSource.SOURCE,
                activeRecordExists = true,
                inputSleepEndTimeMillis = null,
                resolvedWearableSleepEnd = null,
                runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS
            )
        ).isFalse()
    }

    @Test
    fun shouldRetryWearableFinalizer_doesNotStopRetryingForPermissionMissing() {
        assertThat(
            shouldRetryWearableFinalizer(
                expectedSource = HealthConnectSleepTriggerSource.SOURCE,
                activeRecordExists = true,
                inputSleepEndTimeMillis = null,
                resolvedWearableSleepEnd = null,
                resolveResult = WearableSleepEndResolveResult.PermissionMissing,
                runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS
            )
        ).isTrue()
    }

    @Test
    fun shouldRetryWearableFinalizer_doesNotStopRetryingForReadFailed() {
        assertThat(
            shouldRetryWearableFinalizer(
                expectedSource = HealthConnectSleepTriggerSource.SOURCE,
                activeRecordExists = true,
                inputSleepEndTimeMillis = null,
                resolvedWearableSleepEnd = null,
                resolveResult = WearableSleepEndResolveResult.ReadFailed,
                runAttemptCount = MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS
            )
        ).isTrue()
    }

    @Test
    fun healthConnectResolveBackoff_isLongEnoughForDelayedXiaomiSync() {
        assertThat(HEALTH_CONNECT_RESOLVE_BACKOFF_MINUTES).isAtLeast(15L)
        assertThat(MAX_HEALTH_CONNECT_RESOLVE_ATTEMPTS).isAtLeast(8)
    }

    @Test
    fun shouldSkipFinalizerForDifferentActiveRecording_onlySkipsWhenTokenMismatches() {
        assertThat(
            shouldSkipFinalizerForDifferentActiveRecording(
                activeRecordStartMillis = 10_000L,
                expectedActiveRecordingStartMillis = 1_000L
            )
        ).isTrue()
        assertThat(
            shouldSkipFinalizerForDifferentActiveRecording(
                activeRecordStartMillis = 1_000L,
                expectedActiveRecordingStartMillis = 1_000L
            )
        ).isFalse()
        assertThat(
            shouldSkipFinalizerForDifferentActiveRecording(
                activeRecordStartMillis = 10_000L,
                expectedActiveRecordingStartMillis = null
            )
        ).isFalse()
    }

    @Test
    fun wearableFallbackEndTimeMillis_prefersInputSleepEndTime() {
        val result = wearableFallbackEndTimeMillis(
            inputSleepEndTimeMillis = 8_000L,
            resolvedSleepEndTimeMillis = 9_000L,
            fallbackNowMillis = 10_000L
        )

        assertThat(result).isEqualTo(8_000L)
    }

    @Test
    fun wearableFallbackEndTimeMillis_usesResolvedSleepEndWhenInputMissing() {
        val result = wearableFallbackEndTimeMillis(
            inputSleepEndTimeMillis = null,
            resolvedSleepEndTimeMillis = 9_000L,
            fallbackNowMillis = 10_000L
        )

        assertThat(result).isEqualTo(9_000L)
    }

    @Test
    fun wearableFallbackEndTimeMillis_fallsBackToNowWhenHealthConnectMissing() {
        val result = wearableFallbackEndTimeMillis(
            inputSleepEndTimeMillis = null,
            resolvedSleepEndTimeMillis = null,
            fallbackNowMillis = 10_000L
        )

        assertThat(result).isEqualTo(10_000L)
    }

    @Test
    fun activeRecordingFinalizerExistingWorkPolicy_replacesWhenAccurateSleepEndTimeProvided() {
        assertThat(activeRecordingFinalizerExistingWorkPolicy(8_000L))
            .isEqualTo(ExistingWorkPolicy.REPLACE)
    }

    @Test
    fun activeRecordingFinalizerExistingWorkPolicy_keepsExistingAccurateWorkWhenSleepEndTimeMissing() {
        assertThat(activeRecordingFinalizerExistingWorkPolicy(null))
            .isEqualTo(ExistingWorkPolicy.KEEP)
        assertThat(activeRecordingFinalizerExistingWorkPolicy(0L))
            .isEqualTo(ExistingWorkPolicy.KEEP)
    }

    @Test
    fun activeRecordingFinalizerExistingWorkPolicy_replacesWhenSessionTokenProvided() {
        assertThat(
            activeRecordingFinalizerExistingWorkPolicy(
                sleepEndTimeMillis = null,
                activeRecordingStartMillis = 1_000L
            )
        ).isEqualTo(ExistingWorkPolicy.REPLACE)
    }

    private fun createFixture(
        expectedSource: String = HealthConnectSleepTriggerSource.SOURCE,
        inputSleepEndTimeMillis: Long? = null,
        activeRecordingStartMillis: Long? = null,
        activeRecord: SleepRecordEntity? = null
    ): WorkerFixture {
        val activeRecordingFinalizer = mockk<ActiveRecordingFinalizer>()
        val sleepRepository = mockk<SleepRepository>()
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val wearableSleepEndTimeResolver = mockk<WearableSleepEndTimeResolver>()
        coEvery { sleepRepository.getActiveRecordingRecord() } returns activeRecord
        coEvery {
            wearableSleepEndTimeResolver.resolveResult(any())
        } returns WearableSleepEndResolveResult.WaitingForSync
        coEvery {
            activeRecordingFinalizer.finalizeIfActive(any(), any())
        } returns true
        return WorkerFixture(
            expectedSource = expectedSource,
            inputSleepEndTimeMillis = inputSleepEndTimeMillis,
            activeRecordingStartMillis = activeRecordingStartMillis,
            activeRecordingFinalizer = activeRecordingFinalizer,
            sleepRepository = sleepRepository,
            settingsRepository = settingsRepository,
            wearableSleepEndTimeResolver = wearableSleepEndTimeResolver
        )
    }

    private data class WorkerFixture(
        val expectedSource: String,
        val inputSleepEndTimeMillis: Long?,
        val activeRecordingStartMillis: Long?,
        val activeRecordingFinalizer: ActiveRecordingFinalizer,
        val sleepRepository: SleepRepository,
        val settingsRepository: SettingsPreferencesRepository,
        val wearableSleepEndTimeResolver: WearableSleepEndTimeResolver
    ) {
        fun worker(runAttemptCount: Int = 0): ActiveRecordingFinalizerWorker {
            val factory = object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return ActiveRecordingFinalizerWorker(
                        appContext = appContext,
                        workerParams = workerParameters,
                        activeRecordingFinalizer = activeRecordingFinalizer,
                        sleepRepository = sleepRepository,
                        settingsRepository = settingsRepository,
                        wearableSleepEndTimeResolver = wearableSleepEndTimeResolver
                    )
                }
            }
            return TestListenableWorkerBuilder<ActiveRecordingFinalizerWorker>(
                context = RuntimeEnvironment.getApplication()
            )
                .setInputData(
                    workDataOf(
                        "expected_source" to expectedSource,
                        "sleep_end_time_millis" to (inputSleepEndTimeMillis ?: 0L),
                        "active_recording_start_millis" to (activeRecordingStartMillis ?: 0L)
                    )
                )
                .setWorkerFactory(factory)
                .setRunAttemptCount(runAttemptCount)
                .build()
        }
    }

    private fun activeRecord(startTime: Long = 1_000L): SleepRecordEntity {
        return SleepRecordEntity(
            id = 42L,
            startTime = startTime,
            endTime = 0L,
            sleepDurationMin = 0,
            snoreScore = 0,
            severity = Severity.GOOD.name,
            estAHI = 0f,
            snoreDurationMin = 0,
            snoreRatio = 0f,
            avgDb = 0f,
            maxDb = 0f,
            snoreEventCount = 0,
            apneaEventCount = 0,
            longestApneaSec = 0,
            snoreTypeDistribution = "{}",
            hourlyDistribution = "[]",
            aiSummary = "recording",
            aiEvaluation = "",
            aiSuggestions = "[]",
            createdAt = startTime
        )
    }
}
