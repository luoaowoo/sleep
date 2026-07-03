package com.sleep.snore.audio

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 音量能量检测器
 *
 * 基于 RMS (Root Mean Square) 计算每帧的分贝值，
 * 用于快速区分"有声音"和"静音"，作为鼾声检测的第一层过滤器。
 */
class EnergyDetector(
    silenceThresholdDb: Double = -40.0
) {
    /** 静音阈值 (dB)，低于此值视为静音 */
    var silenceThresholdDb: Double = silenceThresholdDb
        private set

    /** 当前帧的分贝值 */
    var currentDb: Double = Double.NEGATIVE_INFINITY
        private set

    /** 当前帧是否有声音 */
    val hasSound: Boolean get() = currentDb > silenceThresholdDb

    /**
     * 处理一帧 PCM 数据，计算分贝值
     * @param pcmFrame 16bit PCM 字节数组
     */
    fun process(pcmFrame: ByteArray) {
        currentDb = calculateDb(pcmFrame)
    }

    /**
     * 计算 PCM 帧的 RMS 分贝值
     *
     * 公式：RMS = sqrt(mean(sample^2))
     *       dB  = 20 * log10(RMS / 32768)
     *
     * @return 分贝值 (dB FS)，范围约 -90 到 0
     */
    fun calculateDb(pcmFrame: ByteArray): Double {
        if (pcmFrame.size < 2) return Double.NEGATIVE_INFINITY

        var sumSquares = 0.0
        var sampleCount = 0
        for (i in 0 until pcmFrame.size - 1 step 2) {
            val sample = ((pcmFrame[i + 1].toInt() shl 8) or (pcmFrame[i].toInt() and 0xFF))
                .toShort()
                .toDouble()
            sumSquares += sample * sample
            sampleCount++
        }
        if (sampleCount == 0) return Double.NEGATIVE_INFINITY

        val rms = sqrt(sumSquares / sampleCount)
        if (rms <= 0.0) return Double.NEGATIVE_INFINITY

        return 20.0 * log10(rms / 32768.0)
    }

    /** 更新静音阈值 */
    fun setThreshold(db: Double) {
        silenceThresholdDb = db
    }
}
