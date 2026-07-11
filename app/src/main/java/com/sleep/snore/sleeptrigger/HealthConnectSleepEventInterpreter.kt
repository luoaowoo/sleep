package com.sleep.snore.sleeptrigger

import java.time.Instant

data class SleepSessionSnapshot(
    val startTime: Instant,
    val endTime: Instant
)

data class InterpretedSleepEvent(
    val event: SleepTriggerEvent,
    val eventKey: String
)

object HealthConnectSleepEventInterpreter {
    fun latestValidSession(
        sessions: List<SleepSessionSnapshot>,
        now: Instant
    ): SleepSessionSnapshot? {
        return sessions
            .filter { !it.startTime.isAfter(now) && !it.endTime.isBefore(it.startTime) }
            .maxByOrNull { it.startTime }
    }

    fun interpret(
        session: SleepSessionSnapshot,
        now: Instant,
        ignoreEventsBefore: Instant? = null,
        source: String = HealthConnectSleepTriggerSource.SOURCE
    ): InterpretedSleepEvent? {
        if (session.startTime.isAfter(now)) return null
        if (session.endTime.isAfter(now)) return null
        if (ignoreEventsBefore != null && session.endTime.isBefore(ignoreEventsBefore)) {
            return null
        }
        val event = SleepTriggerEvent.SleepEnded(
            source = source,
            timestamp = session.endTime.toEpochMilli()
        )
        return InterpretedSleepEvent(
            event = event,
            eventKey = "$EVENT_TYPE_SLEEP_ENDED:${event.timestamp}:${session.startTime.toEpochMilli()}"
        )
    }

    private const val EVENT_TYPE_SLEEP_ENDED = "SleepEnded"
}
