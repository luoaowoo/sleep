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
        val suggestions = buildSuggestions(record)
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
            record.estAHI < 5 -> "疑似 AHI ${String.format("%.1f", record.estAHI)}，未见明显长静音聚集"
            record.estAHI < 15 -> "疑似 AHI ${String.format("%.1f", record.estAHI)}，有轻度长静音迹象，建议持续观察"
            record.estAHI < 30 -> "疑似 AHI ${String.format("%.1f", record.estAHI)}，长静音偏多，建议就医评估"
            else -> "疑似 AHI ${String.format("%.1f", record.estAHI)}，长静音明显偏多，强烈建议就医"
        }
        sb.appendLine("【呼吸暂停】$ahiText；该值由鼾声片段间 ≥10 秒静音估算，不能替代 PSG")
        sb.appendLine()

        sb.appendLine("【鼾声统计】打鼾 ${record.snoreDurationMin} 分钟，占总睡眠 ${(record.snoreRatio * 100).toInt()}%，共 ${record.snoreEventCount} 次")
        sb.appendLine()

        sb.appendLine("【响度分析】平均 ${String.format("%.0f", record.avgDb)}dB，峰值 ${String.format("%.0f", record.maxDb)}dB")
        sb.appendLine()

        if (record.longestApneaSec >= 10) {
            sb.appendLine("【最长长静音】${record.longestApneaSec} 秒，共 ${record.apneaEventCount} 次疑似事件")
        }
        sb.appendLine()

        if (history.size >= 3) {
            val recentAvg = history.take(3).map { it.snoreScore }.average().toInt()
            val trend = if (record.snoreScore < recentAvg) "下降趋势，继续加油！" else "上升趋势，需注意"
            sb.appendLine("【趋势分析】近3天 SnoreScore: $recentAvg, $trend")
        }

        return sb.toString().trimEnd()
    }

    private fun buildSuggestions(record: SleepRecordEntity): List<String> {
        val list = mutableListOf<String>()
        if (record.estAHI >= 15) list.add("疑似 AHI 偏高，建议尽快咨询睡眠科医生")
        if (record.snoreRatio > 0.3) list.add("打鼾时间占比较高，建议尝试侧卧睡眠")
        if (record.avgDb > 55) list.add("鼾声响度较大，避免睡前饮酒可减轻")
        if (record.longestApneaSec >= 30) list.add("出现较长静音间隔，建议做多导睡眠监测 (PSG)")
        if (list.isEmpty()) list.add("整体良好，保持健康作息即可")
        return list
    }
}
