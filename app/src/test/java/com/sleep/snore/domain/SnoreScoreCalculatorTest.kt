package com.sleep.snore.domain

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.SnoreType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoreScoreCalculatorTest {

    @Test
    fun scoreIncreasesForRiskierNight() {
        val calmScore = SnoreScoreCalculator.calculate(
            record = record(snoreRatio = 0.03f, avgDb = -25f, maxDb = -15f, estAhi = 0f, durationMin = 480),
            events = listOf(event(durationMs = 1_000, peakDb = -15f))
        )
        val riskyScore = SnoreScoreCalculator.calculate(
            record = record(snoreRatio = 0.45f, avgDb = -15f, maxDb = -5f, estAhi = 22f, durationMin = 360),
            events = List(80) { event(durationMs = 20_000, peakDb = -5f) }
        )

        assertTrue(riskyScore > calmScore)
        assertTrue(riskyScore >= 60)
    }

    @Test
    fun loudnessScore_nonZeroForNegativeDbFS() {
        val score = SnoreScoreCalculator.calculateLoudnessScore(-25f)
        assertEquals(58.33f, score, 0.01f)
    }

    @Test
    fun loudnessScore_boundaryAtLowerLimit() {
        val score = SnoreScoreCalculator.calculateLoudnessScore(-60f)
        assertEquals(0f, score, 0f)
    }

    @Test
    fun loudnessScore_boundaryAtUpperLimit() {
        val score = SnoreScoreCalculator.calculateLoudnessScore(0f)
        assertEquals(100f, score, 0f)
    }

    @Test
    fun loudnessScore_belowLowerLimitClampedToZero() {
        val score = SnoreScoreCalculator.calculateLoudnessScore(-70f)
        assertEquals(0f, score, 0f)
    }

    private fun record(
        snoreRatio: Float,
        avgDb: Float,
        maxDb: Float,
        estAhi: Float,
        durationMin: Int
    ) = SleepRecordEntity(
        startTime = 0L,
        endTime = durationMin * 60_000L,
        sleepDurationMin = durationMin,
        snoreScore = 0,
        severity = Severity.GOOD.name,
        estAHI = estAhi,
        snoreDurationMin = (durationMin * snoreRatio).toInt(),
        snoreRatio = snoreRatio,
        avgDb = avgDb,
        maxDb = maxDb,
        snoreEventCount = 0,
        apneaEventCount = 0,
        longestApneaSec = 0,
        snoreTypeDistribution = "{}",
        hourlyDistribution = "[]",
        aiSummary = "",
        aiEvaluation = "",
        aiSuggestions = "[]"
    )

    private fun event(durationMs: Int, peakDb: Float) = SnoreEventEntity(
        recordId = 1L,
        startTimestamp = 0L,
        durationMs = durationMs,
        peakDb = peakDb,
        avgDb = peakDb - 8f,
        dominantFreq = 180f,
        snoreType = SnoreType.TONGUE_ROOT.name,
        audioFilePath = "",
        audioFileSizeBytes = 0L,
        aiTypeLabel = "舌根型 · 置信度 80%"
    )
}
