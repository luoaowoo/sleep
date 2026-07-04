package com.sleep.snore.domain

import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.model.Severity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SnoreEvaluator 的 JVM 单元测试。
 *
 * 被测对象仅依赖 Room 数据类（SleepRecordEntity）与 JDK（java.util.Locale），
 * 不依赖 Android Framework，可在纯 JVM 环境下运行。
 */
class SnoreEvaluatorTest {

    /** 用例 1：重度评分（snoreScore=85, estAHI=20）→ 评价文本含"就医"或"咨询"字样 */
    @Test
    fun severeScoreEvaluationContainsMedicalAdvice() {
        val record = record(snoreScore = 85, estAhi = 20f)
        val result = SnoreEvaluator.evaluate(record, history = emptyList())

        val text = result.evaluation
        assertTrue(
            "重度评分且 AHI 偏高时，评价文本应包含'就医'或'咨询'",
            text.contains("就医") || text.contains("咨询")
        )
    }

    /** 用例 2：良好评分（snoreScore=20, estAHI=2）→ 评价文本不含"就医"字样 */
    @Test
    fun goodScoreEvaluationDoesNotContainMedicalAdvice() {
        val record = record(snoreScore = 20, estAhi = 2f)
        val result = SnoreEvaluator.evaluate(record, history = emptyList())

        val text = result.evaluation
        assertFalse(
            "良好评分且 AHI 正常时，评价文本不应包含'就医'",
            text.contains("就医")
        )
    }

    /** 用例 3：历史趋势改善（当前 score=40，历史 [60, 70]）→ 摘要文本含"改善"字样 */
    @Test
    fun trendImprovementSummaryContainsImprovementKeyword() {
        val current = record(snoreScore = 40, estAhi = 5f)
        // history[1] 作为"上次"记录，score=70，diff = 40 - 70 = -30 ≤ -10 → 触发"改善"
        val history = listOf(
            record(snoreScore = 60, estAhi = 8f),
            record(snoreScore = 70, estAhi = 10f)
        )
        val result = SnoreEvaluator.evaluate(current, history = history)

        // 趋势对比文本（"比上次改善X%"）出现在 summary 字段
        val text = result.summary
        assertTrue(
            "趋势改善时摘要文本应包含'改善'",
            text.contains("改善")
        )
    }

    /** 用例 4：AHI>5（estAHI=8）→ 评价文本含"风险"或"暂停"或"关注"字样 */
    @Test
    fun elevatedAhiEvaluationContainsRiskKeyword() {
        val record = record(snoreScore = 50, estAhi = 8f)
        val result = SnoreEvaluator.evaluate(record, history = emptyList())

        val text = result.evaluation
        assertTrue(
            "AHI>5 时评价文本应包含'风险'、'暂停'或'关注'",
            text.contains("风险") || text.contains("暂停") || text.contains("关注")
        )
    }

    /**
     * 构造一个具备合理默认值的 SleepRecordEntity，仅暴露测试需要变化的字段。
     */
    private fun record(
        snoreScore: Int,
        estAhi: Float,
    ) = SleepRecordEntity(
        startTime = 0L,
        endTime = 8 * 60_000L,
        sleepDurationMin = 480,
        snoreScore = snoreScore,
        severity = Severity.GOOD.name,
        estAHI = estAhi,
        snoreDurationMin = 30,
        snoreRatio = 0.1f,
        avgDb = 45f,
        maxDb = 60f,
        snoreEventCount = 20,
        apneaEventCount = 5,
        longestApneaSec = 15,
        snoreTypeDistribution = "{}",
        hourlyDistribution = "[]",
        aiSummary = "",
        aiEvaluation = "",
        aiSuggestions = "[]"
    )
}
