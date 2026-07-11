package com.sleep.snore.service

import kotlin.math.max

internal fun safeRecordingEndTime(startTimeMillis: Long, endTimeMillis: Long): Long {
    return max(startTimeMillis + 1L, endTimeMillis)
}
