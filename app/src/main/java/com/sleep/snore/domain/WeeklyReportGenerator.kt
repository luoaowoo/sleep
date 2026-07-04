package com.sleep.snore.domain

import com.sleep.snore.data.db.entity.SleepRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class WeeklyReport(
    val title: String,
    val localSummary: String,
    val aiSummary: String,
    val prompt: String,
    val recordCount: Int,
    val averageScore: Int,
    val totalSleepMinutes: Int,
    val totalSnoreMinutes: Int,
    val averageAhi: Float,
    val maxDb: Float
)

class WeeklyReportGenerator @Inject constructor() {

    fun generate(records: List<SleepRecordEntity>, customInfo: String = "", aiSummary: String = ""): WeeklyReport {
        val sortedRecords = records.sortedBy { it.startTime }
        if (sortedRecords.isEmpty()) {
            val emptySummary = "本周还没有睡眠记录。完成几晚录音后，我会汇总鼾声时长、AHI 估算和趋势变化。"
            return WeeklyReport(
                title = "本周睡眠总结",
                localSummary = emptySummary,
                aiSummary = aiSummary.ifBlank { emptySummary },
                prompt = buildPrompt(emptyList(), customInfo),
                recordCount = 0,
                averageScore = 0,
                totalSleepMinutes = 0,
                totalSnoreMinutes = 0,
                averageAhi = 0f,
                maxDb = 0f
            )
        }

        val averageScore = sortedRecords.map { it.snoreScore }.average().roundToInt()
        val totalSleepMinutes = sortedRecords.sumOf { it.sleepDurationMin }
        val totalSnoreMinutes = sortedRecords.sumOf { it.snoreDurationMin }
        val averageAhi = sortedRecords.map { it.estAHI }.average().toFloat()
        val maxDb = sortedRecords.maxOf { it.maxDb }
        val trendText = buildTrendText(sortedRecords)
        val localSummary = buildString {
            append("本周记录 ${sortedRecords.size} 晚，平均 SnoreScore $averageScore。")
            append("累计睡眠 ${formatMinutes(totalSleepMinutes)}，鼾声约 ${formatMinutes(totalSnoreMinutes)}，平均 AHI ${"%.1f".format(Locale.getDefault(), averageAhi)}。")
            append(trendText)
            if (maxDb >= 70f || averageAhi >= 15f) {
                append(" 这一周存在较明显鼾声/呼吸暂停风险，若伴随白天嗜睡、憋醒或高血压，建议咨询医生。")
            } else {
                append(" 整体风险暂未达到高危，但仍建议保持规律作息和侧卧睡姿。")
            }
        }

        return WeeklyReport(
            title = buildTitle(sortedRecords),
            localSummary = localSummary,
            aiSummary = aiSummary.ifBlank { localSummary },
            prompt = buildPrompt(sortedRecords, customInfo),
            recordCount = sortedRecords.size,
            averageScore = averageScore,
            totalSleepMinutes = totalSleepMinutes,
            totalSnoreMinutes = totalSnoreMinutes,
            averageAhi = averageAhi,
            maxDb = maxDb
        )
    }

    fun buildPrompt(records: List<SleepRecordEntity>, customInfo: String): String {
        val lines = records.sortedBy { it.startTime }.joinToString("\n") { record ->
            "- ${record.startTime.toDateLabel()}: 睡眠${record.sleepDurationMin}分钟, 鼾声${record.snoreDurationMin}分钟, SnoreScore=${record.snoreScore}, AHI=${"%.1f".format(Locale.US, record.estAHI)}, 峰值=${"%.0f".format(Locale.US, record.maxDb)}dB, 事件=${record.snoreEventCount}, 最长静默=${record.longestApneaSec}秒"
        }
        return """
            你是睡眠鼾声健康分析助手。请基于以下 7 天内睡眠鼾声记录，生成中文周总结。
            要求：1）不做诊断；2）给出趋势、风险提示、3条可执行建议；3）不超过220字；4）如果数据不足，请说明样本有限。

            用户自定义信息：
            ${customInfo.ifBlank { "未提供" }}

            记录：
            ${lines.ifBlank { "暂无记录" }}
        """.trimIndent()
    }

    private fun buildTitle(records: List<SleepRecordEntity>): String {
        val first = records.first().startTime.toDateLabel()
        val last = records.last().startTime.toDateLabel()
        return "$first - $last 周总结"
    }

    private fun buildTrendText(records: List<SleepRecordEntity>): String {
        if (records.size < 2) return " 样本还偏少，趋势仅供参考。"
        val firstHalf = records.take(records.size / 2).map { it.snoreScore }.average()
        val secondHalf = records.drop(records.size / 2).map { it.snoreScore }.average()
        val diff = (secondHalf - firstHalf).roundToInt()
        return when {
            diff <= -5 -> " 后半周较前半周改善约 ${kotlin.math.abs(diff)} 分。"
            diff >= 5 -> " 后半周较前半周加重约 $diff 分。"
            else -> " 前后半周整体较平稳。"
        }
    }

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
    }

    private fun Long.toDateLabel(): String {
        return SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(this))
    }
}
