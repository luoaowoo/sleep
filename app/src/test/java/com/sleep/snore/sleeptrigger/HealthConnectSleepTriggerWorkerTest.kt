package com.sleep.snore.sleeptrigger

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HealthConnectSleepTriggerWorkerTest {

    @Test
    fun doWork_whenDisabledDoesNotPollHealthConnect() = runTest {
        val fixture = createFixture(
            settings = SettingsPreferences(wearableSleepTriggerEnabled = false)
        )

        val result = fixture.worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) {
            fixture.source.pollLatestSleepSession(any(), any(), any())
        }
    }

    @Test
    fun doWork_whenReadFailsStoresRetryableStatus() = runTest {
        val fixture = createFixture(
            settings = SettingsPreferences(wearableSleepTriggerEnabled = true)
        )
        coEvery {
            fixture.source.pollLatestSleepSession(any(), any(), any())
        } throws IllegalStateException("boom")

        val result = fixture.worker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("Health Connect") && it.contains("boom") },
                any()
            )
        }
    }

    @Test
    fun doWork_immediateCheckUsesForegroundReadPermission() = runTest {
        val fixture = createFixture(
            settings = SettingsPreferences(wearableSleepTriggerEnabled = true),
            inputRequireBackgroundRead = false
        )

        fixture.worker().doWork()

        coVerify {
            fixture.source.pollLatestSleepSession(
                now = any<Instant>(),
                requireBackgroundRead = false,
                ignoreEventsBefore = null
            )
        }
    }

    @Test
    fun doWork_periodicCheckRequiresBackgroundReadPermission() = runTest {
        val fixture = createFixture(
            settings = SettingsPreferences(wearableSleepTriggerEnabled = true),
            includeRequireBackgroundReadInput = false
        )

        fixture.worker().doWork()

        coVerify {
            fixture.source.pollLatestSleepSession(
                now = any<Instant>(),
                requireBackgroundRead = true,
                ignoreEventsBefore = null
            )
        }
    }

    @Test
    fun doWork_sleepStartedFromBackgroundCheckDoesNotStartRecording() = runTest {
        val recordingController = FakeRecordingController()
        val fixture = createFixture(
            settings = SettingsPreferences(wearableSleepTriggerEnabled = true),
            recordingController = recordingController
        )
        coEvery {
            fixture.source.pollLatestSleepSession(any(), any(), any())
        } returns HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted(
                source = HealthConnectSleepTriggerSource.SOURCE,
                timestamp = 1000L,
                confidence = HealthConnectSleepTriggerSource.HEALTH_CONNECT_CONFIDENCE
            ),
            eventKey = "SleepStarted:1000:1"
        )

        fixture.worker().doWork()

        assertThat(recordingController.started).isFalse()
        coVerify(exactly = 0) {
            fixture.settingsRepository.setLastWearableSleepEventKey(any())
        }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(
                match { it.contains("不会直接启动麦克风") },
                any()
            )
        }
    }

    @Test
    fun doWork_sleepEndedStopsRecordingAndRemembersEvent() = runTest {
        val recordingController = FakeRecordingController()
        val fixture = createFixture(
            settings = SettingsPreferences(
                wearableSleepTriggerEnabled = true,
                wearableStopOnSleepEndEnabled = true
            ),
            recordingController = recordingController,
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE
        )
        coEvery {
            fixture.source.pollLatestSleepSession(any(), any(), any())
        } returns HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded(
                source = HealthConnectSleepTriggerSource.SOURCE,
                timestamp = 8000L
            ),
            eventKey = "SleepEnded:8000:1000"
        )

        fixture.worker().doWork()

        assertThat(recordingController.stopped).isTrue()
        assertThat(recordingController.stopSource).isEqualTo(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(recordingController.stopSleepEndTimeMillis).isEqualTo(8000L)
        coVerify {
            fixture.settingsRepository.setLastWearableSleepEventKey("SleepEnded:8000:1000")
        }
        coVerify {
            fixture.settingsRepository.setWearableSleepTriggerStatus(any(), any())
        }
    }

    private fun createFixture(
        settings: SettingsPreferences,
        inputRequireBackgroundRead: Boolean = true,
        includeRequireBackgroundReadInput: Boolean = true,
        recordingController: RecordingController = FakeRecordingController(),
        activeRecordingTriggerSource: String? = null
    ): WorkerFixture {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        every { settingsRepository.settings } returns flowOf(settings)
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns activeRecordingTriggerSource
        val source = mockk<HealthConnectSleepTriggerSource>()
        coEvery {
            source.pollLatestSleepSession(any(), any(), any())
        } returns HealthConnectSleepTriggerSource.PollResult.NoRecentSleep
        val coordinator = AutoSnoreDetectionCoordinator(recordingController)
        return WorkerFixture(
            settingsRepository = settingsRepository,
            source = source,
            coordinator = coordinator,
            inputRequireBackgroundRead = inputRequireBackgroundRead,
            includeRequireBackgroundReadInput = includeRequireBackgroundReadInput
        )
    }

    private data class WorkerFixture(
        val settingsRepository: SettingsPreferencesRepository,
        val source: HealthConnectSleepTriggerSource,
        val coordinator: AutoSnoreDetectionCoordinator,
        val inputRequireBackgroundRead: Boolean,
        val includeRequireBackgroundReadInput: Boolean
    ) {
        fun worker(): HealthConnectSleepTriggerWorker {
            val factory = object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return HealthConnectSleepTriggerWorker(
                        appContext = appContext,
                        workerParams = workerParameters,
                        settingsRepository = settingsRepository,
                        healthConnectSleepTriggerSource = source,
                        coordinator = coordinator
                    )
                }
            }
            return TestListenableWorkerBuilder<HealthConnectSleepTriggerWorker>(
                context = RuntimeEnvironment.getApplication()
            )
                .setInputData(
                    if (includeRequireBackgroundReadInput) {
                        workDataOf("require_background_read" to inputRequireBackgroundRead)
                    } else {
                        workDataOf()
                    }
                )
                .setWorkerFactory(factory)
                .build()
        }
    }

    private class FakeRecordingController : RecordingController {
        var started = false
            private set
        var stopped = false
            private set
        var stopSource: String? = null
            private set
        var stopSleepEndTimeMillis: Long? = null
            private set

        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            started = true
            return RecordingStartResult.Confirmed("started")
        }

        override suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long?): Boolean {
            stopped = true
            stopSource = source
            stopSleepEndTimeMillis = sleepEndTimeMillis
            return true
        }

        override fun isRecordingActive(): Boolean = started
    }
}
