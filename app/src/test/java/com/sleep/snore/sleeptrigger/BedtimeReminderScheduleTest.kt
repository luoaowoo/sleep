package com.sleep.snore.sleeptrigger

import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class BedtimeReminderScheduleTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun nextBedtimeReminderDelayMillis_returnsSameDayDelayWhenTimeIsFuture() {
        val now = LocalDateTime.of(2026, 7, 11, 21, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val delay = nextBedtimeReminderDelayMillis(
            nowMillis = now,
            minuteOfDay = 22 * 60 + 30,
            zoneId = zoneId
        )

        assertThat(delay).isEqualTo(90 * 60 * 1000L)
    }

    @Test
    fun nextBedtimeReminderDelayMillis_rollsToTomorrowWhenTimePassed() {
        val now = LocalDateTime.of(2026, 7, 11, 23, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val delay = nextBedtimeReminderDelayMillis(
            nowMillis = now,
            minuteOfDay = 22 * 60 + 30,
            zoneId = zoneId
        )

        assertThat(delay).isEqualTo(23 * 60 * 60 * 1000L + 30 * 60 * 1000L)
    }

    @Test
    fun nextBedtimeReminderDelayMillis_usesMinimumDelayAtExactReminderTime() {
        val now = LocalDateTime.of(2026, 7, 11, 22, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val delay = nextBedtimeReminderDelayMillis(
            nowMillis = now,
            minuteOfDay = 22 * 60 + 30,
            zoneId = zoneId
        )

        assertThat(delay).isEqualTo(24 * 60 * 60 * 1000L)
    }
}
