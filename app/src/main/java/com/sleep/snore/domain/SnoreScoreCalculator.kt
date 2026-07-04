package com.sleep.snore.domain

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.severityFromScore

object SnoreScoreCalculator {

    fun calculate(record: SleepRecordEntity, events: List<SnoreEventEntity>): Int {
        val snoreRatioScore = calculateSnoreRatioScore(record.snoreRatio)
        val loudnessScore = calculateLoudnessScore(record.avgDb)
        val peakScore = calculateLoudnessScore(record.maxDb)
        val ahiScore = calculateAHIScore(record.estAHI)
        val densityScore = calculateEventDensityScore(events.size, record.sleepDurationMin)
        val longestEventScore = calculateLongestEventScore(events)

        val score = (
            0.25 * snoreRatioScore +
                0.20 * loudnessScore +
                0.15 * peakScore +
                0.20 * ahiScore +
                0.12 * densityScore +
                0.08 * longestEventScore
            )
            .toInt()
            .coerceIn(0, 100)
        return score
    }

    private fun calculateSnoreRatioScore(ratio: Float): Float {
        return (ratio * 100).coerceAtMost(100f)
    }

    internal fun calculateLoudnessScore(avgDb: Float): Float {
        // 输入为 dB FS（负值，约 -90~0），转换为 0-100 分数
        // -60 dB FS → 0 分（极轻），0 dB FS → 100 分（满量程）
        return ((avgDb + 60) / 60f * 100f).coerceIn(0f, 100f)
    }

    private fun calculateAHIScore(ahi: Float): Float {
        return when {
            ahi <= 5 -> ahi / 5 * 30
            ahi <= 15 -> 30 + (ahi - 5) / 10 * 30
            ahi <= 30 -> 60 + (ahi - 15) / 15 * 30
            else -> 100f
        }
    }

    private fun calculateEventDensityScore(eventCount: Int, durationMin: Int): Float {
        if (durationMin <= 0) return 0f
        val eventsPerHour = eventCount.toFloat() / (durationMin / 60f)
        return when {
            eventsPerHour <= 10 -> 0f
            eventsPerHour >= 200 -> 100f
            else -> (eventsPerHour / 200) * 100
        }
    }

    private fun calculateLongestEventScore(events: List<SnoreEventEntity>): Float {
        val longestMs = events.maxOfOrNull { it.durationMs } ?: return 0f
        return when {
            longestMs <= 3_000 -> 0f
            longestMs >= 30_000 -> 100f
            else -> ((longestMs - 3_000) / 27_000f * 100f)
        }
    }
}
