package com.sleep.snore.audio

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ZeroCrossingDetector] 的 JVM 单元测试，覆盖鼾声 ZCR 区间的边界值与 ZCR 计算。
 *
 * 被测区间为闭区间 [snoreZcrMin=0.006, snoreZcrMax=0.038]，
 * 由于 [ZeroCrossingDetector.zcr] 为私有 setter，只能通过 [ZeroCrossingDetector.process]
 * 喂入 PCM 帧来产生指定 ZCR。这里用 [pcmFrameWithCrossings] 合成精确过零次数的帧，
 * 使 zcr = crossings / sampleCount 恰好等于边界值。
 */
class ZeroCrossingDetectorTest {

    @Test
    fun zcrAtLowerBoundIsSnoreLike() {
        // 下界 0.006 应命中鼾声区间（闭区间）
        val pcm = pcmFrameWithCrossings(sampleCount = 1000, crossings = 6)
        val detector = ZeroCrossingDetector()
        detector.process(pcm)

        assertEquals(0.006, detector.zcr, 1e-9)
        assertEquals(true, detector.isPotentialSnore)
    }

    @Test
    fun zcrAtUpperBoundIsSnoreLike() {
        // 上界 0.038 应命中鼾声区间（闭区间）
        val pcm = pcmFrameWithCrossings(sampleCount = 1000, crossings = 38)
        val detector = ZeroCrossingDetector()
        detector.process(pcm)

        assertEquals(0.038, detector.zcr, 1e-9)
        assertEquals(true, detector.isPotentialSnore)
    }

    @Test
    fun zcrBelowLowerBoundIsNotSnoreLike() {
        // 低于下界 0.005 不命中
        val pcm = pcmFrameWithCrossings(sampleCount = 1000, crossings = 5)
        val detector = ZeroCrossingDetector()
        detector.process(pcm)

        assertEquals(0.005, detector.zcr, 1e-9)
        assertEquals(false, detector.isPotentialSnore)
    }

    @Test
    fun zcrAboveUpperBoundIsNotSnoreLike() {
        // 高于上界 0.039 不命中
        val pcm = pcmFrameWithCrossings(sampleCount = 1000, crossings = 39)
        val detector = ZeroCrossingDetector()
        detector.process(pcm)

        assertEquals(0.039, detector.zcr, 1e-9)
        assertEquals(false, detector.isPotentialSnore)
    }

    @Test
    fun sineWaveZcrMatchesTwoFreqOverSampleRate() {
        // 纯正弦波每个周期过零两次，ZCR ≈ 2f/sr。
        // 选用 151Hz 而非 150Hz：150Hz @ 16kHz 的半周期 = 160/3 个样本，每 3 个过零就有
        // 1 个恰好落在整数采样点上（sample=0），而 process() 用严格 <0 判定会漏掉这些，
        // 导致 zcr 明显偏低。151Hz 与 8000 互质，整秒内仅中点 i=8000 处有 1 个零样本，
        // 影响可忽略（< 0.0002），故 zcr ≈ 2f/sr = 0.018875，落在鼾声区间内。
        val frequencyHz = 151
        val pcm = sinePcm(
            frequencyHz = frequencyHz,
            durationMs = 1_000,
            amplitude = Short.MAX_VALUE.toInt()
        )

        val detector = ZeroCrossingDetector()
        detector.process(pcm)

        val expectedZcr = 2.0 * frequencyHz / AudioConfig.SAMPLE_RATE
        assertEquals(expectedZcr, detector.zcr, 0.001)
        assertTrue(detector.isPotentialSnore)
    }

    /**
     * 构造包含指定过零次数的 PCM 帧（16bit 小端）。
     *
     * 前 [crossings] 个相邻采样点之间发生符号反转，之后保持同号，
     * 从而精确控制过零率 = crossings / sampleCount。
     */
    private fun pcmFrameWithCrossings(sampleCount: Int, crossings: Int): ByteArray {
        val bytes = ByteArray(sampleCount * 2)
        val amplitude: Short = 1000
        var sign = 1
        var producedCrossings = 0

        fun writeSample(index: Int, value: Short) {
            val offset = index * 2
            bytes[offset] = (value.toInt() and 0xFF).toByte()
            bytes[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }

        writeSample(0, amplitude) // 起始样本为正
        for (i in 1 until sampleCount) {
            if (producedCrossings < crossings) {
                sign = -sign
                producedCrossings++
            }
            writeSample(i, (sign * amplitude).toShort())
        }
        return bytes
    }

    /** 生成纯正弦波 PCM（16bit 小端），与 [AudioAnalysisTest] 中的辅助函数风格一致。 */
    private fun sinePcm(frequencyHz: Int, durationMs: Int, amplitude: Int): ByteArray {
        val sampleCount = AudioConfig.SAMPLE_RATE * durationMs / 1000
        val bytes = ByteArray(sampleCount * 2)
        for (index in 0 until sampleCount) {
            val sample = (sin(2.0 * PI * frequencyHz * index / AudioConfig.SAMPLE_RATE) * amplitude)
                .toInt()
                .toShort()
            val offset = index * 2
            bytes[offset] = (sample.toInt() and 0xFF).toByte()
            bytes[offset + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
