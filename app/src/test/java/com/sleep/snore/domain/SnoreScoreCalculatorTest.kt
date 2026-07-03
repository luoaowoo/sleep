package com.sleep.snore.domain

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.SnoreType
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoreScoreCalculatorTest {

    @Test
    fun scoreIncreasesForRiskierNight() {
        val calmScore = SnoreScoreCalculator.calculate(
            record = record(snoreRatio = 0.03f, avgDb = 32f, maxDb = 40f, estAhi = 0f, durationMin = 480),
            events = listOf(event(durationMs = 1_000, peakDb = 40f))
        )
        val riskyScore = SnoreScoreCalculator.calculate(
            record = record(snoreRatio = 0.45f, avgDb = 62f, maxDb = 78f, estAhi = 22f, durationMin = 360),
            events = List(80) { event(durationMs = 20_000, peakDb = 78f) }
        )

        assertTrue(riskyScore > calmScore)
        assertTrue(riskyScore >= 60)
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
