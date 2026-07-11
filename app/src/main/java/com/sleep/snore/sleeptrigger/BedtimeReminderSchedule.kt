package com.sleep.snore.sleeptrigger

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal fun nextBedtimeReminderDelayMillis(
    nowMillis: Long,
    minuteOfDay: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val safeMinuteOfDay = minuteOfDay.coerceIn(0, 23 * 60 + 59)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    val reminderTime = LocalDate.from(now)
        .atStartOfDay(zoneId)
        .plusMinutes(safeMinuteOfDay.toLong())
    val nextReminderTime = if (reminderTime.toInstant().isAfter(now.toInstant())) {
        reminderTime
    } else {
        reminderTime.plusDays(1)
    }
    return ChronoUnit.MILLIS.between(now.toInstant(), nextReminderTime.toInstant())
        .coerceAtLeast(MINIMUM_WORK_DELAY_MILLIS)
}

private const val MINIMUM_WORK_DELAY_MILLIS = 60_000L
