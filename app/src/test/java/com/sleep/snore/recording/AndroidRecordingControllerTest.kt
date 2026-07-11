package com.sleep.snore.recording

import android.Manifest
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.service.SleepRecordingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidRecordingControllerTest {

    @Test
    fun startFromSleepTrigger_whenAppNotVisibleFailsBeforeStartingMicrophoneService() = runTest {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        val notifier = mockk<RecordingFailureNotifier>(relaxed = true)
        val controller = AndroidRecordingController(
            context = context,
            settingsRepository = settingsRepository,
            recordingFailureNotifier = notifier,
            appVisibilityState = object : AppVisibilityState {
                override val isAppVisible: Boolean = false
            }
        )

        val result = controller.startFromSleepTrigger("health_connect_sleep")

        assertThat(result.confirmed).isFalse()
        assertThat(result.requestSubmitted).isFalse()
        assertThat(result.statusText).contains("应用不在前台")
        assertThat(shadowOf(context).nextStartedService).isNull()
        coVerify { settingsRepository.setWearableSleepTriggerStatus(match { it.contains("应用不在前台") }, any()) }
        verify { notifier.notifyWearableStartIssue(match { it.contains("应用不在前台") }) }
    }

    @Test
    fun startFromSleepTrigger_whenAppVisibleSubmitsForegroundServiceStart() = runTest {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        coEvery { settingsRepository.getActiveRecordingTriggerSource() } returns null
        val notifier = mockk<RecordingFailureNotifier>(relaxed = true)
        val controller = AndroidRecordingController(
            context = context,
            settingsRepository = settingsRepository,
            recordingFailureNotifier = notifier,
            appVisibilityState = object : AppVisibilityState {
                override val isAppVisible: Boolean = true
            }
        )

        controller.startFromSleepTrigger("health_connect_sleep")

        val startedService = shadowOf(context).nextStartedService
        assertThat(startedService.component?.className).isEqualTo(SleepRecordingService::class.java.name)
        assertThat(startedService.action).isEqualTo(SleepRecordingService.ACTION_START)
    }
}
