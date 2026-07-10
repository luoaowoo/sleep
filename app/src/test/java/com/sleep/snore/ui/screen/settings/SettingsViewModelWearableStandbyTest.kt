package com.sleep.snore.ui.screen.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
            recordingController = recordingController
        )

        viewModel.onWearableSleepTriggerChange(false)
        advanceUntilIdle()

        assertThat(recordingController.stoppedSources)
            .containsExactly(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(repository.settingsSnapshot().wearableSleepTriggerEnabled).isFalse()
        assertThat(repository.settingsSnapshot().wearableSleepTriggerStatus).isEqualTo("手环自动检测已关闭")
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

    private class FakeRecordingController : RecordingController {
        val stoppedSources = mutableListOf<String>()

        override suspend fun startFromSleepTrigger(source: String): RecordingStartResult {
            return RecordingStartResult.Confirmed("started")
        }

        override suspend fun stopFromSleepTrigger(source: String): Boolean {
            stoppedSources += source
            return true
        }

        override fun isRecordingActive(): Boolean = false
    }

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }
}
