package com.sleep.snore.recording

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.severityFromScore
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.repository.SleepRepository
import com.sleep.snore.domain.SnoreEvaluator
import com.sleep.snore.domain.SnoreScoreCalculator
import com.sleep.snore.service.recordingDurationSummary
import com.sleep.snore.service.safeRecordingEndTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.flow.first

@Singleton
class ActiveRecordingFinalizer @Inject constructor(
    private val repository: SleepRepository,
    private val settingsRepository: SettingsPreferencesRepository
) {

    suspend fun finalizeIfActive(
        expectedTriggerSource: String? = null,
        expectedActiveRecordingStartMillis: Long? = null,
        endTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val activeRecord = repository.getActiveRecordingRecord() ?: return false
        if (
            expectedActiveRecordingStartMillis != null &&
            expectedActiveRecordingStartMillis > 0L &&
            activeRecord.startTime != expectedActiveRecordingStartMillis
        ) {
            return false
        }
        if (!expectedTriggerSource.isNullOrBlank() &&
            settingsRepository.getActiveRecordingTriggerSource() != expectedTriggerSource
        ) {
            return false
        }

        val events = repository.getEventsSnapshotByRecordId(activeRecord.id)
        repository.updateRecord(buildFinalRecord(activeRecord, endTimeMillis, events))
        settingsRepository.clearActiveRecordingTriggerSource()
        return true
    }

    private suspend fun buildFinalRecord(
        activeRecord: SleepRecordEntity,
        endTimeMillis: Long,
        events: List<SnoreEventEntity>
    ): SleepRecordEntity {
        val safeEndTime = safeRecordingEndTime(activeRecord.startTime, endTimeMillis)
        val durationMs = max(1L, safeEndTime - activeRecord.startTime)
        val snoreDurationMs = events.sumOf { it.durationMs.toLong() }
        val durationSummary = recordingDurationSummary(durationMs, snoreDurationMs)
        val apneaStats = estimateApneaStats(events, durationMs)
        val base = activeRecord.copy(
            endTime = safeEndTime,
            sleepDurationMin = durationSummary.sleepDurationMin,
            snoreScore = 0,
            severity = Severity.GOOD.name,
            estAHI = apneaStats.ahi,
            snoreDurationMin = durationSummary.snoreDurationMin,
            snoreRatio = (snoreDurationMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
            avgDb = if (events.isEmpty()) 0f else events.map { it.avgDb }.average().toFloat(),
            maxDb = events.maxOfOrNull { it.peakDb } ?: 0f,
            snoreEventCount = events.size,
            apneaEventCount = apneaStats.eventCount,
            longestApneaSec = apneaStats.longestSec,
            snoreTypeDistribution = buildTypeDistribution(events),
            hourlyDistribution = buildHourlyDistribution(events),
            aiSummary = "",
            aiEvaluation = "",
            aiSuggestions = "[]"
        )
        val score = SnoreScoreCalculator.calculate(base, events)
        val scored = base.copy(
            snoreScore = score,
            severity = severityFromScore(score).name
        )
        val history = repository.getAllRecords().first().filter { it.id != activeRecord.id }
        val evaluation = SnoreEvaluator.evaluate(scored, listOf(scored) + history)
        return scored.copy(
            aiSummary = evaluation.summary,
            aiEvaluation = evaluation.evaluation,
            aiSuggestions = evaluation.suggestions.joinToString(prefix = "[", postfix = "]") {
                "\"${it.replace("\"", "'")}\""
            }
        )
    }

    private fun buildTypeDistribution(events: List<SnoreEventEntity>): String {
        if (events.isEmpty()) return "{}"
        return events.groupingBy { it.snoreType }
            .eachCount()
            .entries
            .joinToString(prefix = "{", postfix = "}") { (type, count) ->
                "\"$type\":${count.toFloat() / events.size}"
            }
    }

    private fun buildHourlyDistribution(events: List<SnoreEventEntity>): String {
        val buckets = IntArray(24)
        events.forEach { event ->
            val hour = Calendar.getInstance().apply {
                timeInMillis = event.startTimestamp
            }.get(Calendar.HOUR_OF_DAY)
            buckets[hour]++
        }
        return buckets.joinToString(prefix = "[", postfix = "]")
    }

    private fun estimateApneaStats(events: List<SnoreEventEntity>, durationMs: Long): ApneaStats {
        val sortedEvents = events.sortedBy { it.startTimestamp }
        if (sortedEvents.size < 2) {
            return ApneaStats(eventCount = 0, longestSec = 0, ahi = 0f)
        }

        var eventCount = 0
        var longestSec = 0
        sortedEvents.forEachIndexed { index, current ->
            if (index == 0) return@forEachIndexed
            val previous = sortedEvents[index - 1]
            val previousEnd = previous.startTimestamp + previous.durationMs
            val gapMs = current.startTimestamp - previousEnd
            if (gapMs < APNEA_GAP_MS) return@forEachIndexed
            eventCount++
            longestSec = max(longestSec, (gapMs / 1000L).toInt())
        }

        val durationHours = (durationMs / 3_600_000f).coerceAtLeast(1f / 60f)
        return ApneaStats(
            eventCount = eventCount,
            longestSec = longestSec,
            ahi = (eventCount / durationHours).coerceAtMost(120f)
        )
    }

    private data class ApneaStats(
        val eventCount: Int,
        val longestSec: Int,
        val ahi: Float
    )

    private companion object {
        const val APNEA_GAP_MS = 10_000L
    }
}
