package com.sleep.snore.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordingTimeBoundsTest {

    @Test
    fun safeRecordingEndTime_keepsEndAfterStart() {
        assertThat(safeRecordingEndTime(startTimeMillis = 1_000L, endTimeMillis = 2_000L))
            .isEqualTo(2_000L)
    }

    @Test
    fun safeRecordingEndTime_clampsEndEqualToStart() {
        assertThat(safeRecordingEndTime(startTimeMillis = 1_000L, endTimeMillis = 1_000L))
            .isEqualTo(1_001L)
    }

    @Test
    fun safeRecordingEndTime_clampsEndBeforeStart() {
        assertThat(safeRecordingEndTime(startTimeMillis = 1_000L, endTimeMillis = 500L))
            .isEqualTo(1_001L)
    }
}
