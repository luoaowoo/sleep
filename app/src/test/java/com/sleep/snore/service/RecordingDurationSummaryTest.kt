package com.sleep.snore.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordingDurationSummaryTest {

    @Test
    fun recordingDurationSummary_roundsShortSnoreUpToOneMinute() {
        val summary = recordingDurationSummary(
            durationMs = 5 * 60_000L,
            snoreDurationMs = 500L
        )

        assertThat(summary.sleepDurationMin).isEqualTo(5)
        assertThat(summary.snoreDurationMin).isEqualTo(1)
    }

    @Test
    fun recordingDurationSummary_capsSnoreDurationToSleepDuration() {
        val summary = recordingDurationSummary(
            durationMs = 59 * 60_000L + 1L,
            snoreDurationMs = 60 * 60_000L
        )

        assertThat(summary.sleepDurationMin).isEqualTo(60)
        assertThat(summary.snoreDurationMin).isEqualTo(60)
    }

    @Test
    fun recordingDurationSummary_keepsNoSnoreAtZero() {
        val summary = recordingDurationSummary(
            durationMs = 30_000L,
            snoreDurationMs = 0L
        )

        assertThat(summary.sleepDurationMin).isEqualTo(1)
        assertThat(summary.snoreDurationMin).isEqualTo(0)
    }
}
