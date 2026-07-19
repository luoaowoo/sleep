package com.sleep.snore.recording

import android.Manifest
import androidx.work.Configuration
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.recording.ActiveRecordingFinalizerWorker.Companion.WORK_NAME
import com.sleep.snore.service.SleepRecordingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
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
        assertThat(startedService.getStringExtra("trigger_source")).isEqualTo("health_connect_sleep")
    }

    @Test
    fun stopFromSleepTrigger_whenActiveSourceIsManualDoesNotStopService() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        every { settingsRepository.settings } returns flowOf(
            SettingsPreferences(activeRecordingTriggerSource = "manual")
        )
        val notifier = mockk<RecordingFailureNotifier>(relaxed = true)
        val controller = AndroidRecordingController(
            context = context,
            settingsRepository = settingsRepository,
            recordingFailureNotifier = notifier,
            appVisibilityState = object : AppVisibilityState {
                override val isAppVisible: Boolean = true
            }
        )

        val stopped = controller.stopFromSleepTrigger("health_connect_sleep", 2_000L)

        assertThat(stopped).isFalse()
        assertThat(shadowOf(context).nextStartedService).isNull()
    }

    @Test
    fun stopFromSleepTrigger_whenActiveSourceMatchesSubmitsStopWithSleepEndTime() = runTest {
        val context = RuntimeEnvironment.getApplication()
        initializeWorkManager(context)
        val settingsRepository = mockk<SettingsPreferencesRepository>(relaxed = true)
        every { settingsRepository.settings } returns flowOf(
            SettingsPreferences(
                activeRecordingTriggerSource = "health_connect_sleep",
                activeRecordingTriggerStartedAtMillis = 1_000L
            )
        )
        val notifier = mockk<RecordingFailureNotifier>(relaxed = true)
        val controller = AndroidRecordingController(
            context = context,
            settingsRepository = settingsRepository,
            recordingFailureNotifier = notifier,
            appVisibilityState = object : AppVisibilityState {
                override val isAppVisible: Boolean = true
            }
        )

        val stopped = controller.stopFromSleepTrigger("health_connect_sleep", 2_000L)

        val startedService = shadowOf(context).nextStartedService
        assertThat(stopped).isTrue()
        assertThat(startedService.component?.className).isEqualTo(SleepRecordingService::class.java.name)
        assertThat(startedService.action).isEqualTo(SleepRecordingService.ACTION_STOP)
        assertThat(startedService.getStringExtra("expected_trigger_source")).isEqualTo("health_connect_sleep")
        assertThat(startedService.getLongExtra("sleep_end_time_millis", 0L)).isEqualTo(2_000L)
        assertThat(startedService.getLongExtra("expected_active_recording_start_millis", 0L)).isEqualTo(1_000L)
        val workManager = WorkManagerImpl.getInstance(context)
        val workSpecIds = workManager.workDatabase.workNameDao().getWorkSpecIdsWithName(WORK_NAME)
        val workSpec = workManager.workDatabase.workSpecDao().getWorkSpec(workSpecIds.single())
        assertThat(workSpec).isNotNull()
        workSpec!!
        assertThat(workSpec.input.getString("expected_source")).isEqualTo("health_connect_sleep")
        assertThat(workSpec.input.getLong("sleep_end_time_millis", 0L)).isEqualTo(2_000L)
        assertThat(workSpec.input.getLong("active_recording_start_millis", 0L)).isEqualTo(1_000L)
    }

    private fun initializeWorkManager(context: android.content.Context) {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
        )
    }
}
