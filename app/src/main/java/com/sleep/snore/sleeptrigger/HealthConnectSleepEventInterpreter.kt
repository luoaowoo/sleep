package com.sleep.snore.sleeptrigger

import java.time.Instant
import java.time.temporal.ChronoUnit

data class SleepSessionSnapshot(
    val startTime: Instant,
    val endTime: Instant,
    val dataOriginPackageName: String = ""
) {
    val isKnownXiaomiSource: Boolean
        get() = dataOriginPackageName in XiaomiSleepCompanionApps.packageNames
}

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

    fun latestActionableSession(
        sessions: List<SleepSessionSnapshot>,
        now: Instant,
        ignoreEventsBefore: Instant? = null,
        emitOngoingSleepStart: Boolean = false
    ): Pair<SleepSessionSnapshot, InterpretedSleepEvent>? {
        return sessions
            .asSequence()
            .filter { !it.startTime.isAfter(now) && !it.endTime.isBefore(it.startTime) }
            .sortedWith(compareByDescending<SleepSessionSnapshot> { it.endTime }.thenByDescending { it.startTime })
            .mapNotNull { session ->
                val event = interpret(
                    session = session,
                    now = now,
                    ignoreEventsBefore = ignoreEventsBefore,
                    emitOngoingSleepStart = emitOngoingSleepStart
                ) ?: return@mapNotNull null
                session to event
            }
            .firstOrNull()
    }

    fun interpret(
        session: SleepSessionSnapshot,
        now: Instant,
        ignoreEventsBefore: Instant? = null,
        minimumOverlapAfterIgnoreBoundaryMillis: Long = MINIMUM_OVERLAP_AFTER_IGNORE_BOUNDARY_MILLIS,
        emitOngoingSleepStart: Boolean = false,
        source: String = HealthConnectSleepTriggerSource.SOURCE
    ): InterpretedSleepEvent? {
        if (session.startTime.isAfter(now)) return null
        if (session.endTime.isAfter(now)) {
            if (!emitOngoingSleepStart) return null
            if (ignoreEventsBefore != null && session.startTime.isBefore(ignoreEventsBefore)) return null
            val event = SleepTriggerEvent.SleepStarted(
                source = source,
                timestamp = session.startTime.toEpochMilli(),
                confidence = HealthConnectSleepTriggerSource.HEALTH_CONNECT_CONFIDENCE
            )
            return InterpretedSleepEvent(
                event = event,
                eventKey = "$EVENT_TYPE_SLEEP_STARTED:${event.timestamp}"
            )
        }
        if (ignoreEventsBefore != null && session.endTime.isBefore(ignoreEventsBefore)) {
            return null
        }
        if (
            ignoreEventsBefore != null &&
            minimumOverlapAfterIgnoreBoundaryMillis > 0L &&
            overlapAfterBoundaryMillis(session, ignoreEventsBefore) < minimumOverlapAfterIgnoreBoundaryMillis
        ) {
            return null
        }
        if (
            ignoreEventsBefore != null &&
            sessionDurationMillis(session) < MINIMUM_AUTO_STOP_SLEEP_SESSION_DURATION_MILLIS
        ) {
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

    private fun overlapAfterBoundaryMillis(
        session: SleepSessionSnapshot,
        boundary: Instant
    ): Long {
        val overlapStart = maxOf(session.startTime, boundary)
        return ChronoUnit.MILLIS.between(overlapStart, session.endTime).coerceAtLeast(0L)
    }

    internal fun sessionDurationMillis(session: SleepSessionSnapshot): Long {
        return ChronoUnit.MILLIS.between(session.startTime, session.endTime).coerceAtLeast(0L)
    }

    private const val EVENT_TYPE_SLEEP_STARTED = "SleepStarted"
    private const val EVENT_TYPE_SLEEP_ENDED = "SleepEnded"
    private const val MINIMUM_OVERLAP_AFTER_IGNORE_BOUNDARY_MILLIS = 30L * 60L * 1000L
    internal const val MINIMUM_AUTO_STOP_SLEEP_SESSION_DURATION_MILLIS = 2L * 60L * 60L * 1000L
}
