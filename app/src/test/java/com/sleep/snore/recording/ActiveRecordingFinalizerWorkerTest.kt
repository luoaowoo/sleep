package com.sleep.snore.recording

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ActiveRecordingFinalizerWorkerTest {

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
}
