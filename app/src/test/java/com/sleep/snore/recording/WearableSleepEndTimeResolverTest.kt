package com.sleep.snore.recording

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.preferences.SettingsPreferences
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.sleeptrigger.HealthConnectSleepSessionPoller
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import com.sleep.snore.sleeptrigger.SleepTriggerEvent
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class WearableSleepEndTimeResolverTest {

    @Test
    fun resolve_returnsSleepEndTimeAndEventKey() = runTest {
        val settingsRepository = settingsRepository(
            SettingsPreferences(
                activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE,
                activeRecordingTriggerStartedAtMillis = 1_000L
            )
        )
        val poller = FakeSleepSessionPoller(
            HealthConnectSleepTriggerSource.PollResult.EventEmitted(
                event = SleepTriggerEvent.SleepEnded(
                    source = HealthConnectSleepTriggerSource.SOURCE,
                    timestamp = 8_000L
                ),
                eventKey = "SleepEnded:8000:1000"
            )
        )

        val result = WearableSleepEndTimeResolver(settingsRepository, poller).resolve(activeRecord())

        assertThat(result).isEqualTo(
            ResolvedWearableSleepEnd(
                endTimeMillis = 8_000L,
                eventKey = "SleepEnded:8000:1000"
            )
        )
        assertThat(poller.lastRequireBackgroundRead).isTrue()
        assertThat(poller.lastIgnoreEventsBefore).isEqualTo(Instant.ofEpochMilli(1_000L))
    }

    @Test
    fun resolve_usesActiveRecordStartWhenTriggerStartMissing() = runTest {
        val settingsRepository = settingsRepository(
            SettingsPreferences(activeRecordingTriggerSource = HealthConnectSleepTriggerSource.SOURCE)
        )
        val poller = FakeSleepSessionPoller(HealthConnectSleepTriggerSource.PollResult.NoRecentSleep)

        WearableSleepEndTimeResolver(settingsRepository, poller).resolve(activeRecord(startTime = 2_000L))

        assertThat(poller.lastIgnoreEventsBefore).isEqualTo(Instant.ofEpochMilli(2_000L))
    }

    @Test
    fun resolve_returnsNullWhenActiveRecordingIsNotWearableTriggered() = runTest {
        val settingsRepository = settingsRepository(SettingsPreferences(activeRecordingTriggerSource = ""))
        val poller = FakeSleepSessionPoller(
            HealthConnectSleepTriggerSource.PollResult.EventEmitted(
                event = SleepTriggerEvent.SleepEnded(
                    source = HealthConnectSleepTriggerSource.SOURCE,
                    timestamp = 8_000L
                ),
                eventKey = "SleepEnded:8000:1000"
            )
        )

        val result = WearableSleepEndTimeResolver(settingsRepository, poller).resolve(activeRecord())

        assertThat(result).isNull()
        assertThat(poller.called).isFalse()
    }

    private fun settingsRepository(settings: SettingsPreferences): SettingsPreferencesRepository {
        return mockk {
            every { this@mockk.settings } returns flowOf(settings)
        }
    }

    private class FakeSleepSessionPoller(
        private val result: HealthConnectSleepTriggerSource.PollResult
    ) : HealthConnectSleepSessionPoller {
        var called: Boolean = false
            private set
        var lastRequireBackgroundRead: Boolean = false
            private set
        var lastIgnoreEventsBefore: Instant? = null
            private set

        override suspend fun pollLatestSleepSession(
            now: Instant,
            requireBackgroundRead: Boolean,
            ignoreEventsBefore: Instant?
        ): HealthConnectSleepTriggerSource.PollResult {
            called = true
            lastRequireBackgroundRead = requireBackgroundRead
            lastIgnoreEventsBefore = ignoreEventsBefore
            return result
        }
    }

    private fun activeRecord(startTime: Long = 1_000L): SleepRecordEntity {
        return SleepRecordEntity(
            id = 42L,
            startTime = startTime,
            endTime = 0L,
            sleepDurationMin = 0,
            snoreScore = 0,
            severity = Severity.GOOD.name,
            estAHI = 0f,
            snoreDurationMin = 0,
            snoreRatio = 0f,
            avgDb = 0f,
            maxDb = 0f,
            snoreEventCount = 0,
            apneaEventCount = 0,
            longestApneaSec = 0,
            snoreTypeDistribution = "{}",
            hourlyDistribution = "[]",
            aiSummary = "recording",
            aiEvaluation = "",
            aiSuggestions = "[]",
            createdAt = startTime
        )
    }
}
