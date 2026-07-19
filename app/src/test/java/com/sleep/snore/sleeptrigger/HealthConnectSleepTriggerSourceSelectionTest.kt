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
    fun selectXiaomiActionableSleepSession_ignoresInvalidXiaomiSourceForSourceDecision() {
        val nonXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-12T07:00:00Z"),
            dataOriginPackageName = "com.example.sleep"
        )
        val futureXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T09:00:00Z"),
            endTime = Instant.parse("2026-07-12T10:00:00Z"),
            dataOriginPackageName = "com.mi.health"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(nonXiaomiSession, futureXiaomiSession),
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

    @Test
    fun selectXiaomiActionableSleepSession_prefersOlderXiaomiSessionOverNewerNonXiaomiSession() {
        val newerNonXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T00:30:00Z"),
            endTime = Instant.parse("2026-07-12T07:30:00Z"),
            dataOriginPackageName = "com.example.sleep"
        )
        val olderXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-12T07:00:00Z"),
            dataOriginPackageName = "com.xiaomi.wearable"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(newerNonXiaomiSession, olderXiaomiSession),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T22:00:00Z")
        )

        assertThat(result).isInstanceOf(XiaomiActionableSleepSelection.Actionable::class.java)
        val actionable = result as XiaomiActionableSleepSelection.Actionable
        assertThat(actionable.session).isEqualTo(olderXiaomiSession)
        assertThat(actionable.event.event).isInstanceOf(SleepTriggerEvent.SleepEnded::class.java)
    }

    @Test
    fun selectXiaomiActionableSleepSession_prefersOlderLongXiaomiSleepOverNewerShortXiaomiNap() {
        val olderLongSleep = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-12T07:00:00Z"),
            dataOriginPackageName = "com.mi.health"
        )
        val newerShortNap = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T07:20:00Z"),
            endTime = Instant.parse("2026-07-12T07:45:00Z"),
            dataOriginPackageName = "com.xiaomi.wearable"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(olderLongSleep, newerShortNap),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T22:30:00Z")
        )

        assertThat(result).isInstanceOf(XiaomiActionableSleepSelection.Actionable::class.java)
        val actionable = result as XiaomiActionableSleepSelection.Actionable
        assertThat(actionable.session).isEqualTo(olderLongSleep)
        assertThat(actionable.event.event).isInstanceOf(SleepTriggerEvent.SleepEnded::class.java)
    }

    @Test
    fun selectXiaomiActionableSleepSession_reportsXiaomiShortSleepEvenWithNewerNonXiaomiSession() {
        val newerNonXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T00:30:00Z"),
            endTime = Instant.parse("2026-07-12T07:30:00Z"),
            dataOriginPackageName = "com.example.sleep"
        )
        val olderXiaomiShortSleep = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-11T23:45:00Z"),
            dataOriginPackageName = "com.mi.health"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(newerNonXiaomiSession, olderXiaomiShortSleep),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T22:30:00Z")
        )

        assertThat(result).isInstanceOf(XiaomiActionableSleepSelection.NoActionable::class.java)
        val noActionable = result as XiaomiActionableSleepSelection.NoActionable
        assertThat(noActionable.observedSession).isEqualTo(olderXiaomiShortSleep)
        assertThat(noActionable.reason)
            .isEqualTo(HealthConnectSleepTriggerSource.PollResult.NoActionableSleepReason.SHORT_SLEEP_SESSION)
    }


    @Test
    fun selectXiaomiActionableSleepSession_emitsSleepStartForOngoingXiaomiSessionWhenEnabled() {
        val ongoingXiaomiSession = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T07:30:00Z"),
            endTime = Instant.parse("2026-07-12T12:00:00Z"),
            dataOriginPackageName = "com.mi.health"
        )

        val result = selectXiaomiActionableSleepSession(
            sessions = listOf(ongoingXiaomiSession),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-12T07:00:00Z"),
            emitOngoingSleepStart = true
        )

        assertThat(result).isInstanceOf(XiaomiActionableSleepSelection.Actionable::class.java)
        val actionable = result as XiaomiActionableSleepSelection.Actionable
        assertThat(actionable.session).isEqualTo(ongoingXiaomiSession)
        assertThat(actionable.event.event).isInstanceOf(SleepTriggerEvent.SleepStarted::class.java)
        assertThat(actionable.event.eventKey)
            .isEqualTo("SleepStarted:${ongoingXiaomiSession.startTime.toEpochMilli()}")
    }

}
