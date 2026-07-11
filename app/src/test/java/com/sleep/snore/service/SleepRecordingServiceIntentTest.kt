package com.sleep.snore.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SleepRecordingServiceIntentTest {

    @Test
    fun startIntent_includesTriggerSourceOnlyWhenProvided() {
        val context = RuntimeEnvironment.getApplication()

        val triggerIntent = SleepRecordingService.startIntent(context, "health_connect_sleep")
        val manualIntent = SleepRecordingService.startIntent(context)

        assertThat(triggerIntent.action).isEqualTo(SleepRecordingService.ACTION_START)
        assertThat(triggerIntent.getStringExtra("trigger_source")).isEqualTo("health_connect_sleep")
        assertThat(manualIntent.action).isEqualTo(SleepRecordingService.ACTION_START)
        assertThat(manualIntent.hasExtra("trigger_source")).isFalse()
    }

    @Test
    fun stopFromTriggerIntent_carriesExpectedSourceAndSleepEndTime() {
        val context = RuntimeEnvironment.getApplication()

        val intent = SleepRecordingService.stopFromTriggerIntent(
            context = context,
            expectedTriggerSource = "health_connect_sleep",
            sleepEndTimeMillis = 2_000L
        )

        assertThat(intent.action).isEqualTo(SleepRecordingService.ACTION_STOP)
        assertThat(intent.getStringExtra("expected_trigger_source")).isEqualTo("health_connect_sleep")
        assertThat(intent.getLongExtra("sleep_end_time_millis", 0L)).isEqualTo(2_000L)
    }
}
