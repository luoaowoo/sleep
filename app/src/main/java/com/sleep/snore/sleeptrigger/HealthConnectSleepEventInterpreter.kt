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
        source: String = HealthConnectSleepTriggerSource.SOURCE,
        confidence: Float = HealthConnectSleepTriggerSource.HEALTH_CONNECT_CONFIDENCE
    ): InterpretedSleepEvent? {
        if (session.startTime.isAfter(now)) return null
        val eventType: String
        val event = if (session.endTime.isAfter(now)) {
            eventType = EVENT_TYPE_SLEEP_STARTED
            SleepTriggerEvent.SleepStarted(
                source = source,
                timestamp = session.startTime.toEpochMilli(),
                confidence = confidence
            )
        } else {
            if (ignoreEventsBefore != null && session.endTime.isBefore(ignoreEventsBefore)) {
                return null
            }
            eventType = EVENT_TYPE_SLEEP_ENDED
            SleepTriggerEvent.SleepEnded(
                source = source,
                timestamp = session.endTime.toEpochMilli()
            )
        }
        return InterpretedSleepEvent(
            event = event,
            eventKey = "$eventType:${event.timestamp}:${session.startTime.toEpochMilli()}"
        )
    }

    private const val EVENT_TYPE_SLEEP_STARTED = "SleepStarted"
    private const val EVENT_TYPE_SLEEP_ENDED = "SleepEnded"
}
