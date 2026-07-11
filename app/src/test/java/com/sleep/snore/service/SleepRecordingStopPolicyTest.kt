package com.sleep.snore.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SleepRecordingStopPolicyTest {

    @Test
    fun shouldAcceptStopRequest_allowsManualStopWithoutExpectedSource() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "manual",
                expectedTriggerSource = null
            )
        ).isTrue()
    }

    @Test
    fun shouldAcceptStopRequest_allowsMatchingWearableSource() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "health_connect_sleep",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isTrue()
    }

    @Test
    fun shouldAcceptStopRequest_rejectsWearableStopForManualRecording() {
        assertThat(
            shouldAcceptStopRequest(
                activeTriggerSource = "manual",
                expectedTriggerSource = "health_connect_sleep"
            )
        ).isFalse()
    }
}
