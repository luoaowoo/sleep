package com.sleep.snore.domain

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.model.Severity
import com.sleep.snore.data.model.severityFromScore

data class SnoreEvaluation(
    val severity: Severity,
    val summary: String,
    val evaluation: String,
    val suggestions: List<String>
)

object SnoreEvaluator {

    fun evaluate(record: SleepRecordEntity, history: List<SleepRecordEntity>): SnoreEvaluation {
        val severity = severityFromScore(record.snoreScore)
        val summary = buildSummary(record, severity, history)
        val evaluation = buildFullEvaluation(record, severity, history)
        val suggestions = buildSuggestions(record, history)
        return SnoreEvaluation(severity, summary, evaluation, suggestions)
    }

    private fun buildSummary(record: SleepRecordEntity, severity: Severity, history: List<SleepRecordEntity>): String {
        val trendPart = if (history.size >= 2) {
            val prev = history[1]
            val diff = record.snoreScore - prev.snoreScore
            when {
                diff <= -10 -> "比上次改善${-diff}%"
                diff >= 10 -> "比上次加重${diff}%"
                else -> "与上次接近"
            }
        } else ""

        return "昨晚打鼾${severity.label}，SnoreScore ${record.snoreScore}$trendPart"
    }

    private fun buildFullEvaluation(record: SleepRecordEntity, severity: Severity, history: List<SleepRecordEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("【总体评价】打鼾程度${severity.label}，SnoreScore ${record.snoreScore}/100")
        sb.appendLine()

        val ahiText = when {
            record.estAHI < 5 -> "AHI 估算 ${String.format("%.1f", record.estAHI)}，在正常范围内 (< 5)"
            record.estAHI < 15 -> "AHI 估算 ${String.format("%.1f", record.estAHI)}，轻度异常，建议关注"
            record.estAHI < 30 -> "AHI 估算 ${String.format("%.1f", record.estAHI)}，中度异常，建议就医评估"
            else -> "AHI 估算 ${String.format("%.1f", record.estAHI)}，重度异常，强烈建议就医"
        }
        sb.appendLine("【呼吸暂停】$ahiText")
        sb.appendLine()

        sb.appendLine("【鼾声统计】打鼾 ${record.snoreDurationMin} 分钟，占总睡眠 ${(record.snoreRatio * 100).toInt()}%，共 ${record.snoreEventCount} 次")
        sb.appendLine()

        sb.appendLine("【响度分析】平均 ${String.format("%.0f", record.avgDb)}dB，峰值 ${String.format("%.0f", record.maxDb)}dB")
        sb.appendLine()

        if (record.longestApneaSec >= 10) {
            sb.appendLine("【最长暂停】${record.longestApneaSec} 秒")
        }
        sb.appendLine()

        if (history.size >= 3) {
            val recentAvg = history.take(3).map { it.snoreScore }.average().toInt()
            val trend = if (record.snoreScore < recentAvg) "下降趋势，继续加油！" else "上升趋势，需注意"
            sb.appendLine("【趋势分析】近3天 SnoreScore: $recentAvg, $trend")
        }

        return sb.toString().trimEnd()
    }

    private fun buildSuggestions(record: SleepRecordEntity, history: List<SleepRecordEntity>): List<String> {
        val list = mutableListOf<String>()
        if (record.estAHI >= 15) list.add("AHI 偏高，建议尽快咨询睡眠科医生")
        if (record.snoreRatio > 0.3) list.add("打鼾时间占比较高，建议尝试侧卧睡眠")
        if (record.avgDb > 55) list.add("鼾声响度较大，避免睡前饮酒可减轻")
        if (record.longestApneaSec >= 30) list.add("出现较长呼吸暂停，建议做多导睡眠监测 (PSG)")
        if (list.isEmpty()) list.add("整体良好，保持健康作息即可")
        return list
    }
}
