package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class HealthConnectSleepTriggerSourceSelectionTest {

    private val now: Instant = Instant.parse("2026-07-12T08:00:00Z")

    @Test
    fun selectXiaomiActionableSleepSession_nonXiaomiOnlyReturnsDiagnosticReason() {
        val nonXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-12T07:00:00Z"),
            dataOriginPackageName = "com.example.sleep"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(nonXiaomiSession),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T22:00:00Z")
        )

        assertThat(result).isInstanceOf(XiaomiActionableSleepSelection.NoActionable::class.java)
        val noActionable = result as XiaomiActionableSleepSelection.NoActionable
        assertThat(noActionable.observedSession).isEqualTo(nonXiaomiSession)
        assertThat(noActionable.reason)
            .isEqualTo(HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.NON_XIAOMI_SOURCE)
    }

    @Test
    fun selectXiaomiActionableSleepSession_usesXiaomiSessionWhenMixedWithNonXiaomi() {
        val nonXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T22:00:00Z"),
            endTime = Instant.parse("2026-07-12T06:00:00Z"),
            dataOriginPackageName = "com.example.sleep"
        )
        val xiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-12T07:00:00Z"),
            dataOriginPackageName = "com.mi.health"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(nonXiaomiSession, xiaomiSession),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T22:00:00Z")
        )

        assertThat(result).isInstanceOf(XiaomiActionableSleepSelection.Actionable::class.java)
        val actionable = result as XiaomiActionableSleepSelection.Actionable
        assertThat(actionable.session).isEqualTo(xiaomiSession)
        assertThat(actionable.event.event).isInstanceOf(SleepTriggerEvent.SleepEnded::class.java)
    }
}
