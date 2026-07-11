package com.sleep.snore.ui.screen.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SecretTextCipher
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.RecordingController
import com.sleep.snore.recording.RecordingStartResult
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelWearableStandbyTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        WorkManagerTestInitHelper.initializeTestWorkManager(
            RuntimeEnvironment.getApplication(),
            Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onWearableSleepTriggerChange_offStopsPrestartedRecording() = runTest(dispatcher) {
        val repository = createRepository()
        val recordingController = FakeRecordingController()
        val viewModel = SettingsViewModel(
            context = RuntimeEnvironment.getApplication(),
            preferencesRepository = repository,
            recordingController = recordingController,
            wearableStandbyPrerequisiteChecker = FakeWearableStandbyPrerequisiteChecker()
        )

        viewModel.onWearableSleepTriggerChange(false)
        advanceUntilIdle()

        assertThat(recordingController.stoppedSources)
            .containsExactly(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(repository.settingsSnapshot().wearableSleepTriggerEnabled).isFalse()
        assertThat(repository.settingsSnapshot().wearableSleepTriggerStatus).isEqualTo("Health Connect 周期检查已关闭")
    }

    @Test
    fun checkWearableSleepNow_enablesPeriodicCheckWithoutStartingRecording() = runTest(dispatcher) {
        val repository = createRepository()
        val recordingController = FakeRecordingController()
        val viewModel = SettingsViewModel(
            context = RuntimeEnvironment.getApplication(),
            preferencesRepository = repository,
            recordingController = recordingController,
            wearableStandbyPrerequisiteChecker = FakeWearableStandbyPrerequisiteChecker()
        )

        viewModel.checkWearableSleepNow()
        advanceUntilIdle()

        val settings = repository.settingsSnapshot()
        assertThat(settings.wearableSleepTriggerEnabled).isTrue()
        assertThat(settings.wearableSleepTriggerStatus).contains("不会开始录音")
        assertThat(settings.wearableSleepTriggerStatus).contains("前台检测")
        assertThat(recordingController.startedSources).isEmpty()
    }

    @Test
    fun startWearableSleepStandby_whenPrerequisiteBlockedDoesNotStartRecording() = runTest(dispatcher) {
        val repository = createRepository()
        val recordingController = FakeRecordingController()
        val viewModel = SettingsViewModel(
            context = RuntimeEnvironment.getApplication(),
            preferencesRepository = repository,
            recordingController = recordingController,
            wearableStandbyPrerequisiteChecker = FakeWearableStandbyPrerequisiteChecker(
                blocker = "缺少 Health Connect 睡眠/后台读取权限，请先授权 Health Connect"
            )
        )

        viewModel.startWearableSleepStandby()
        advanceUntilIdle()

        assertThat(recordingController.startedSources).isEmpty()
        assertThat(repository.settingsSnapshot().wearableSleepTriggerEnabled).isTrue()
        assertThat(repository.settingsSnapshot().wearableSleepTriggerStatus)
            .isEqualTo("缺少 Health Connect 睡眠/后台读取权限，请先授权 Health Connect")
    }

    @Test
    fun startWearableSleepStandby_whenRecordingConfirmedWritesSuccessStatus() = runTest(dispatcher) {
        val repository = createRepository()
        val recordingController = FakeRecordingController(
            startResult = RecordingStartResult.Confirmed("started")
        )
        val viewModel = SettingsViewModel(
            context = RuntimeEnvironment.getApplication(),
            preferencesRepository = repository,
            recordingController = recordingController,
            wearableStandbyPrerequisiteChecker = FakeWearableStandbyPrerequisiteChecker()
        )

        viewModel.startWearableSleepStandby()
        advanceUntilIdle()

        assertThat(recordingController.startedSources)
            .containsExactly(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(repository.settingsSnapshot().wearableSleepTriggerStatus)
            .isEqualTo("睡前前台检测已开启，录音服务将低频检查 Health Connect 睡眠结束")
    }

    @Test
    fun startWearableSleepStandby_whenAutoStopDisabledWritesManualStopStatus() = runTest(dispatcher) {
        val repository = createRepository()
        repository.setWearableStopOnSleepEndEnabled(false)
        val recordingController = FakeRecordingController(
            startResult = RecordingStartResult.Confirmed("started")
        )
        val viewModel = SettingsViewModel(
            context = RuntimeEnvironment.getApplication(),
            preferencesRepository = repository,
            recordingController = recordingController,
            wearableStandbyPrerequisiteChecker = FakeWearableStandbyPrerequisiteChecker()
        )

        viewModel.startWearableSleepStandby()
        advanceUntilIdle()

        assertThat(recordingController.startedSources)
            .containsExactly(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(repository.settingsSnapshot().wearableSleepTriggerStatus)
            .isEqualTo("睡前前台检测已开启，但自动停录已关闭；睡醒后请手动停止")
    }

    @Test
    fun startWearableSleepStandby_whenRecordingNotConfirmedKeepsControllerStatus() = runTest(dispatcher) {
        val repository = createRepository()
        val recordingController = FakeRecordingController(
            startResult = RecordingStartResult.Submitted("等待录音服务确认")
        )
        val viewModel = SettingsViewModel(
            context = RuntimeEnvironment.getApplication(),
            preferencesRepository = repository,
            recordingController = recordingController,
            wearableStandbyPrerequisiteChecker = FakeWearableStandbyPrerequisiteChecker()
        )

        viewModel.startWearableSleepStandby()
        advanceUntilIdle()

        assertThat(recordingController.startedSources)
            .containsExactly(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(repository.settingsSnapshot().wearableSleepTriggerStatus)
            .isEqualTo("等待录音服务确认")
    }

    private fun createRepository(): SettingsPreferencesRepository {
        val dataStoreFile = File.createTempFile("sleep-settings-view-model", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(dispatcher),
            produceFile = { dataStoreFile }
        )
        return SettingsPreferencesRepository(dataStore, FakeSecretTextCipher)
    }

    private suspend fun SettingsPreferencesRepository.settingsSnapshot() = settings.first()

    private class FakeRecordingController(
        private val startResult: RecordingStartResult = RecordingStartResult.Confirmed("started")
    ) : RecordingController {
        val startedSources = mutableListOf<String>()
        val stoppedSources = mutableListOf<String>()

        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            startedSources += source
            return startResult
        }

        override suspend fun stopFromSleepTrigger(source: String, sleepEndTimeMillis: Long?): Boolean {
            stoppedSources += source
            return true
        }

        override fun isRecordingActive(): Boolean = false
    }

    private class FakeWearableStandbyPrerequisiteChecker(
        private val blocker: String? = null
    ) : WearableStandbyPrerequisiteChecker(RuntimeEnvironment.getApplication()) {
        override suspend fun startBlocker(): String? = blocker
    }

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }
}
