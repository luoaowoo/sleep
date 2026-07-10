package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.preferences.SettingsPreferences
import java.time.Instant
import org.junit.Test

class SleepEventBoundaryTest {

    @Test
    fun sleepEventIgnoreEventsBefore_prefersActiveRecordingTriggerStart() {
        val settings = SettingsPreferences(
            activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE,
            activeRecordingTriggerStartedAtMillis = 2_000L
        )

        val result = sleepEventIgnoreEventsBefore(
            settings = settings,
            fallbackMillis = 1_000L
        )

        assertThat(result).isEqualTo(Instant.ofEpochMilli(2_000L))
    }

    @Test
    fun sleepEventIgnoreEventsBefore_usesFallbackWithoutActiveRecording() {
        val result = sleepEventIgnoreEventsBefore(
            settings = SettingsPreferences(),
            fallbackMillis = 1_000L
        )

        assertThat(result).isEqualTo(Instant.ofEpochMilli(1_000L))
    }

    @Test
    fun sleepEventIgnoreEventsBefore_ignoresDifferentTriggerSource() {
        val settings = SettingsPreferences(
            activeRecordingTriggerSource = "manual",
            activeRecordingTriggerStartedAtMillis = 2_000L
        )

        val result = sleepEventIgnoreEventsBefore(
            settings = settings,
            fallbackMillis = 1_000L
        )

        assertThat(result).isEqualTo(Instant.ofEpochMilli(1_000L))
    }

    @Test
    fun sleepEventIgnoreEventsBefore_returnsNullWithoutAnyBoundary() {
        val result = sleepEventIgnoreEventsBefore(settings = SettingsPreferences())

        assertThat(result).isNull()
    }
}
