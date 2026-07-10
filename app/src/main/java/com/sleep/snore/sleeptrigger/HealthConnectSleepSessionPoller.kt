package com.sleep.snore.sleeptrigger

import java.time.Instant

interface HealthConnectSleepSessionPoller {
    suspend fun pollLatestSleepSession(
        now: Instant,
        requireBackgroundRead: Boolean,
        ignoreEventsBefore: Instant? = null
    ): HealthConnectSleepTriggerSource.PollResult
}
