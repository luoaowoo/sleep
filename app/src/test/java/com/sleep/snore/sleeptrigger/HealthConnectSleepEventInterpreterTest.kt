package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class HealthConnectSleepEventInterpreterTest {

    @Test
    fun interpret_returnsSleepStartedForOngoingSession() {
        val now = Instant.parse("2026-07-11T02:00:00Z")
        val result = HealthConnectSleepEventInterpreter.interpret(
            session = SleepSessionSnapshot(
                startTime = Instant.parse("2026-07-11T01:00:00Z"),
                endTime = Instant.parse("2026-07-11T08:00:00Z")
            ),
            now = now
        )

        val event = result?.event as SleepTriggerEvent.SleepStarted
        assertThat(event.source).isEqualTo(HealthConnectSleepTriggerSource.SOURCE)
        assertThat(event.timestamp).isEqualTo(Instant.parse("2026-07-11T01:00:00Z").toEpochMilli())
        assertThat(event.confidence).isEqualTo(HealthConnectSleepTriggerSource.HEALTH_CONNECT_CONFIDENCE)
        assertThat(result.eventKey).isEqualTo("SleepStarted:${event.timestamp}:${event.timestamp}")
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
}
