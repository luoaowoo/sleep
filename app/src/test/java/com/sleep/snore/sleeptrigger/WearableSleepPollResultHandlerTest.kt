package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WearableSleepPollResultHandlerTest {

    @Test
    fun handleWearableSleepPollResult_remembersHandledSleepStart() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(
            FakeRecordingController(RecordingStartResult.Confirmed("started"))
        )
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = false,
            allowSleepStartRecording = true
        )

        assertThat(result.statusText).isEqualTo("started")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.emittedSleepEnd).isFalse()
        assertThat(result.eventHandled).isTrue()
        coVerify { settingsRepository.setLastWearableSleepEventKey("SleepStarted:1:1") }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotRememberUnconfirmedSleepStart() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(
            FakeRecordingController(RecordingStartResult.Failed("missing permission"))
        )
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = false,
            allowSleepStartRecording = true
        )

        assertThat(result.statusText).isEqualTo("missing permission")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.emittedSleepEnd).isFalse()
        assertThat(result.eventHandled).isFalse()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_mapsForegroundPermissionStatus() = runTest {
        val result = handleWearableSleepPollResult(
            pollResult = HealthConnectSleepTriggerSource.PollResult.PermissionMissing,
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController()),
            settingsRepository = mockk(relaxed = true),
            requireBackgroundRead = false
        )

        assertThat(result.statusText).isEqualTo("缺少 Health Connect 睡眠读取权限")
        assertThat(result.emittedSleepStart).isFalse()
        assertThat(result.emittedSleepEnd).isFalse()
        assertThat(result.eventHandled).isFalse()
    }

    @Test
    fun handleWearableSleepPollResult_savesObservedSleepSessionForDuplicateEvent() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val session = SleepSessionSnapshot(
            startTime = Instant.ofEpochMilli(1_000L),
            endTime = Instant.ofEpochMilli(8_000L),
            dataOriginPackageName = "com.xiaomi.wearable"
        )

        val result = handleWearableSleepPollResult(
            pollResult = HealthConnectSleepTriggerSource.PollResult.DuplicateEvent(
                observedSession = session,
                eventKey = "SleepEnded:8000:1000"
            ),
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController()),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.statusText).isEqualTo("最近睡眠记录已处理，等待新记录")
        assertThat(result.eventHandled).isFalse()
        coVerify {
            settingsRepository.setLatestWearableSleepSession(
                startMillis = 1_000L,
                endMillis = 8_000L,
                status = "已处理",
                sourcePackage = "com.xiaomi.wearable"
            )
        }
    }

    @Test
    fun handleWearableSleepPollResult_savesObservedSleepSessionForOldRecord() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val session = SleepSessionSnapshot(
            startTime = Instant.ofEpochMilli(1_000L),
            endTime = Instant.ofEpochMilli(8_000L),
            dataOriginPackageName = "com.xiaomi.wearable"
        )

        val result = handleWearableSleepPollResult(
            pollResult = HealthConnectSleepTriggerSource.PollResult.NoActionableSleep(
                observedSession = session,
                reason = HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.BEFORE_ACTIVE_RECORDING
            ),
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController()),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.statusText).contains("早于本次睡前前台检测")
        assertThat(result.eventHandled).isFalse()
        coVerify {
            settingsRepository.setLatestWearableSleepSession(
                startMillis = 1_000L,
                endMillis = 8_000L,
                status = "早于本次检测，已忽略",
                sourcePackage = "com.xiaomi.wearable"
            )
        }
    }

    @Test
    fun handleWearableSleepPollResult_reportsShortOverlapSleepSession() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val session = SleepSessionSnapshot(
            startTime = Instant.ofEpochMilli(1_000L),
            endTime = Instant.ofEpochMilli(8_000L),
            dataOriginPackageName = "com.xiaomi.wearable"
        )

        val result = handleWearableSleepPollResult(
            pollResult = HealthConnectSleepTriggerSource.PollResult.NoActionableSleep(
                observedSession = session,
                reason = HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason
                    .INSUFFICIENT_ACTIVE_RECORDING_OVERLAP
            ),
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController()),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.statusText).contains("重叠不足 30 分钟")
        assertThat(result.eventHandled).isFalse()
        coVerify {
            settingsRepository.setLatestWearableSleepSession(
                startMillis = 1_000L,
                endMillis = 8_000L,
                status = "与本次检测重叠过短，已忽略",
                sourcePackage = "com.xiaomi.wearable"
            )
        }
    }

    @Test
    fun handleWearableSleepPollResult_reportsNonXiaomiSourceAsDiagnosticOnly() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val session = SleepSessionSnapshot(
            startTime = Instant.ofEpochMilli(1_000L),
            endTime = Instant.ofEpochMilli(8_000L),
            dataOriginPackageName = "com.example.sleep"
        )

        val result = handleWearableSleepPollResult(
            pollResult = HealthConnectSleepTriggerSource.PollResult.NoActionableSleep(
                observedSession = session,
                reason = HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.NON_XIAOMI_SOURCE
            ),
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController()),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.statusText).contains("不是已知小米伴侣")
        assertThat(result.eventHandled).isFalse()
        coVerify {
            settingsRepository.setLatestWearableSleepSession(
                startMillis = 1_000L,
                endMillis = 8_000L,
                status = "非小米来源，仅诊断",
                sourcePackage = "com.example.sleep"
            )
        }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotStartRecordingWhenSleepStartDisallowed() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val controller = FakeRecordingController()
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(controller),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true,
            allowSleepStartRecording = false
        )

        assertThat(result.statusText).isEqualTo("检测到睡眠开始；后台轮询不会直接启动麦克风，请使用睡前前台检测")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.eventKey).isEqualTo("SleepStarted:1:1")
        assertThat(controller.started).isFalse()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotStartRecordingFromSleepStartByDefault() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val controller = FakeRecordingController()
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepStarted("health_connect_sleep", timestamp = 1L, confidence = 0.8f),
            eventKey = "SleepStarted:1:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = AutoSnoreDetectionCoordinator(controller),
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.statusText).isEqualTo("检测到睡眠开始；后台轮询不会直接启动麦克风，请使用睡前前台检测")
        assertThat(result.emittedSleepStart).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.eventKey).isEqualTo("SleepStarted:1:1")
        assertThat(controller.started).isFalse()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_marksHandledSleepEnd() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns "health_connect_sleep"
        val controller = FakeRecordingController()
        val coordinator = AutoSnoreDetectionCoordinator(controller)
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepStart).isFalse()
        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isTrue()
        assertThat(controller.stoppedSource).isEqualTo("health_connect_sleep")
        assertThat(controller.stoppedAtMillis).isEqualTo(2L)
        coVerify { settingsRepository.setLastWearableSleepEventKey("SleepEnded:2:1") }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotRememberFailedSleepEnd() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns "health_connect_sleep"
        val coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController(stopResult = false))
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，但未能停止鼾声检测；将继续重试")
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_doesNotRememberSleepEndWithoutActiveWearableRecording() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns null
        val controller = FakeRecordingController()
        val coordinator = AutoSnoreDetectionCoordinator(controller)
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = true,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.statusText).contains("已作为诊断记录")
        assertThat(controller.stoppedSource).isNull()
        coVerify(exactly = 0) { settingsRepository.setLastWearableSleepEventKey(any()) }
    }

    @Test
    fun handleWearableSleepPollResult_remembersSleepEndWhenAutoStopDisabled() = runTest {
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val coordinator = AutoSnoreDetectionCoordinator(FakeRecordingController())
        val pollResult = HealthConnectSleepTriggerSource.PollResult.EventEmitted(
            event = SleepTriggerEvent.SleepEnded("health_connect_sleep", timestamp = 2L),
            eventKey = "SleepEnded:2:1"
        )

        val result = handleWearableSleepPollResult(
            pollResult = pollResult,
            stopOnSleepEnd = false,
            coordinator = coordinator,
            settingsRepository = settingsRepository,
            requireBackgroundRead = true
        )

        assertThat(result.emittedSleepEnd).isTrue()
        assertThat(result.eventHandled).isFalse()
        assertThat(result.statusText).isEqualTo("检测到睡眠结束，已按设置保持鼾声检测")
        coVerify { settingsRepository.setLastWearableSleepEventKey("SleepEnded:2:1") }
    }

    private class FakeRecordingController(
        private val startResult: RecordingStartResult = RecordingStartResult.Confirmed("started"),
        private val stopResult: Boolean = true
    ) : RecordingController {
        var started = false
            private set
        var stoppedSource: String? = null
            private set
        var stoppedAtMillis: Long? = null
            private set

        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            started = true
            return startResult
        }
        override suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long?): Boolean {
            stoppedSource = source
            stoppedAtMillis = sleepEndTimeMillis
            return stopResult
        }
        override fun isRecordingActive(): Boolean = startResult.confirmed
    }
}
