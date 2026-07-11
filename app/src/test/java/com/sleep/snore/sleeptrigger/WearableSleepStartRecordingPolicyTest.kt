package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearableSleepStartRecordingPolicyTest {

    @Test
    fun wearableBackgroundChecks_doNotStartRecordingFromSleepStarted() {
        assertThat(allowSleepStartRecordingFromBackgroundCheck()).isFalse()
    }
}
