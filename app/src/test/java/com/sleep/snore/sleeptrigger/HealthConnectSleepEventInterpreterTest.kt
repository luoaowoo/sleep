package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class HealthConnectSleepEventInterpreterTest {

    @Test
    fun latestValidSession_ignoresFutureSessionAndReturnsMostRecentValidOne() {
        val now = Instant.parse("2026-07-11T09:00:00Z")
        val older = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-10T23:00:00Z"),
            endTime = Instant.parse("2026-07-11T06:00:00Z")
        )
        val latestValid = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T01:00:00Z"),
            endTime = Instant.parse("2026-07-11T08:00:00Z")
        )
        val future = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T01:00:00Z"),
            endTime = Instant.parse("2026-07-12T08:00:00Z")
        )

        val result = HealthConnectSleepEventInterpreter.latestValidSession(
            sessions = listOf(older, future, latestValid),
            now = now
        )

        assertThat(result).isEqualTo(latestValid)
    }

    @Test
    fun latestValidSession_returnsNullWhenOnlyFutureSessionsExist() {
        val result = HealthConnectSleepEventInterpreter.latestValidSession(
            sessions = listOf(
                SleepSessionSnapshot(
                    startTime = Instant.parse("2026-07-12T01:00:00Z"),
                    endTime = Instant.parse("2026-07-12T08:00:00Z")
                )
            ),
            now = Instant.parse("2026-07-11T09:00:00Z")
        )

        assertThat(result).isNull()
    }

    @Test
    fun latestValidSession_ignoresInvalidSessionAndReturnsMostRecentValidOne() {
        val now = Instant.parse("2026-07-11T09:00:00Z")
        val valid = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T01:00:00Z"),
            endTime = Instant.parse("2026-07-11T08:00:00Z")
        )
        val invalid = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T02:00:00Z"),
            endTime = Instant.parse("2026-07-11T01:30:00Z")
        )

        val result = HealthConnectSleepEventInterpreter.latestValidSession(
            sessions = listOf(valid, invalid),
            now = now
        )

        assertThat(result).isEqualTo(valid)
    }

    @Test
    fun interpret_ignoresOngoingSessionBecauseHealthConnectIsNotRealtimeStartSignal() {
        val now = Instant.parse("2026-07-11T02:00:00Z")
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T01:00:00Z"),
                endTime = Instant.parse("2026-07-11T08:00:00Z")
            ),
            now = now
        )

        assertThat(result).isNull()
    }


    @Test
    fun interpret_returnsSleepStartedForOngoingSessionWhenEnabled() {
        val now = Instant.parse("2026-07-11T02:00:00Z")
        val start = Instant.parse("2026-07-11T01:00:00Z")
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = start,
                endTime = Instant.parse("2026-07-11T08:00:00Z")
            ),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T00:30:00Z"),
            emitOngoingSleepStart = true
        )

        val event = result?.event as SleepTriggerEvent.SleepStarted
        assertThat(event.timestamp).isEqualTo(start.toEpochMilli())
        assertThat(event.confidence).isEqualTo(HealthConnectSleepTriggerSource.HEALTH_CONNECT_CONFIDENCE)
        assertThat(result.eventKey).isEqualTo("SleepStarted:${start.toEpochMilli()}")
    }

    @Test
    fun interpret_ignoresOngoingSessionBeforeArmedTimeEvenWhenStartEnabled() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T01:00:00Z"),
                endTime = Instant.parse("2026-07-11T08:00:00Z")
            ),
            now = Instant.parse("2026-07-11T02:00:00Z"),
            ignoreEventsBefore = Instant.parse("2026-07-11T01:30:00Z"),
            emitOngoingSleepStart = true
        )

        assertThat(result).isNull()
    }

    @Test
    fun interpret_returnsSleepEndedForFinishedSession() {
        val now = Instant.parse("2026-07-11T09:00:00Z")
        val start = Instant.parse("2026-07-11T01:00:00Z")
        val end = Instant.parse("2026-07-11T08:00:00Z")

        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(startTime = start, endTime = end),
            now = now
        )

        val event = result?.event as SleepTriggerEvent.SleepEnded
        assertThat(event.timestamp).isEqualTo(end.toEpochMilli())
        assertThat(result.eventKey).isEqualTo("SleepEnded:${end.toEpochMilli()}:${start.toEpochMilli()}")
    }

    @Test
    fun interpret_ignoresFinishedSessionBeforeArmedTime() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-10T23:00:00Z"),
                endTime = Instant.parse("2026-07-11T06:00:00Z")
            ),
            now = Instant.parse("2026-07-11T22:00:00Z"),
            ignoreEventsBefore = Instant.parse("2026-07-11T21:55:00Z")
        )

        assertThat(result).isNull()
    }

    @Test
    fun interpret_keepsFinishedSessionAfterArmedTime() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T22:00:00Z"),
                endTime = Instant.parse("2026-07-12T06:00:00Z")
            ),
            now = Instant.parse("2026-07-12T06:05:00Z"),
            ignoreEventsBefore = Instant.parse("2026-07-11T21:55:00Z")
        )

        assertThat(result?.event).isInstanceOf(SleepTriggerEvent.SleepEnded::class.java)
    }

    @Test
    fun interpret_ignoresFinishedSessionWithShortOverlapAfterArmedTime() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T20:00:00Z"),
                endTime = Instant.parse("2026-07-11T22:10:00Z")
            ),
            now = Instant.parse("2026-07-11T22:15:00Z"),
            ignoreEventsBefore = Instant.parse("2026-07-11T22:00:00Z")
        )

        assertThat(result).isNull()
    }

    @Test
    fun interpret_keepsFinishedSessionWithEnoughOverlapAfterArmedTime() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T20:00:00Z"),
                endTime = Instant.parse("2026-07-11T22:45:00Z")
            ),
            now = Instant.parse("2026-07-11T23:00:00Z"),
            ignoreEventsBefore = Instant.parse("2026-07-11T22:00:00Z")
        )

        assertThat(result?.event).isInstanceOf(SleepTriggerEvent.SleepEnded::class.java)
    }

    @Test
    fun interpret_ignoresShortNapAfterArmedTime() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T22:00:00Z"),
                endTime = Instant.parse("2026-07-11T22:45:00Z")
            ),
            now = Instant.parse("2026-07-11T23:00:00Z"),
            ignoreEventsBefore = Instant.parse("2026-07-11T21:55:00Z")
        )

        assertThat(result).isNull()
    }

    @Test
    fun latestActionableSession_prefersLongSleepOverNewerShortNap() {
        val now = Instant.parse("2026-07-12T12:00:00Z")
        val longSleep = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T23:00:00Z"),
            endTime = Instant.parse("2026-07-12T07:00:00Z")
        )
        val shortNap = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-12T11:00:00Z"),
            endTime = Instant.parse("2026-07-12T11:30:00Z")
        )

        val result = HealthConnectSleepEventInterpreter.latestActionableSession(
            sessions = listOf(longSleep, shortNap),
            now = now,
            ignoreEventsBefore = Instant.parse("2026-07-11T22:30:00Z")
        )

        assertThat(result?.first).isEqualTo(longSleep)
        assertThat(result?.second?.eventKey)
            .isEqualTo("SleepEnded:${longSleep.endTime.toEpochMilli()}:${longSleep.startTime.toEpochMilli()}")
    }

    @Test
    fun interpret_treatsEndEqualNowAsFinished() {
        val now = Instant.parse("2026-07-11T08:00:00Z")
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T01:00:00Z"),
                endTime = now
            ),
            now = now
        )

        assertThat(result?.event).isInstanceOf(SleepTriggerEvent.SleepEnded::class.java)
    }

    @Test
    fun interpret_ignoresFutureSession() {
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-12T01:00:00Z"),
                endTime = Instant.parse("2026-07-12T08:00:00Z")
            ),
            now = Instant.parse("2026-07-11T09:00:00Z")
        )

        assertThat(result).isNull()
    }

    @Test
    fun interpret_returnsStableKeyForSameSession() {
        val now = Instant.parse("2026-07-11T09:00:00Z")
        val session = SleepSessionSnapshot(
            startTime = Instant.parse("2026-07-11T01:00:00Z"),
            endTime = Instant.parse("2026-07-11T08:00:00Z")
        )

        val first = HealthConnectSleepEventInterpreter.interpret(session = session, now = now)
        val second = HealthConnectSleepEventInterpreter.interpret(session = session, now = now)

        assertThat(first?.eventKey).isEqualTo(second?.eventKey)
        assertThat(first?.eventKey).startsWith("SleepEnded:")
    }

    @Test
    fun sleepSessionSnapshot_marksKnownXiaomiSourcePackages() {
        assertThat(
            SleepSessionSnapshot(
                startTime = Instant.EPOCH,
                endTime = Instant.EPOCH,
                dataOriginPackageName = "com.mi.health"
            ).isKnownXiaomiSource
        ).isTrue()
        assertThat(
            SleepSessionSnapshot(
                startTime = Instant.EPOCH,
                endTime = Instant.EPOCH,
                dataOriginPackageName = "com.example.sleep"
            ).isKnownXiaomiSource
        ).isFalse()
    }
}
