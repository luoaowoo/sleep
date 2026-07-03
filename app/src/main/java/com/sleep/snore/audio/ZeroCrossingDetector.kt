package com.sleep.snore.audio

/**
 * 过零率 (Zero-Crossing Rate) 检测器
 *
 * 鼾声的过零率集中在较低范围 (50-300 Hz)，
 * 明显区别于语音（分布更广）和高频环境音。
 * 作为鼾声检测的第二层过滤器。
 */
class ZeroCrossingDetector(
    /** 鼾声 ZCR 下限 (50Hz 对应) */
    private val snoreZcrMin: Double = 0.006,
    /** 鼾声 ZCR 上限 (300Hz 对应) */
    private val snoreZcrMax: Double = 0.038
) {
    /** 当前帧过零率 */
    var zcr: Double = 0.0
        private set

    /** 是否为疑似鼾声 (基于 ZCR) */
    val isPotentialSnore: Boolean
        get() = zcr in snoreZcrMin..snoreZcrMax

    /**
     * 处理一帧 PCM 数据，计算过零率
     *
     * ZCR = (符号变化次数) / (总采样点数)
     *
     * @param pcmFrame 16bit PCM 字节数组
     */
    fun process(pcmFrame: ByteArray) {
        if (pcmFrame.size < 4) {
            zcr = 0.0
            return
        }

        val sampleCount = pcmFrame.size / 2
        var crossings = 0

        var prevSample = readSample(pcmFrame, 0)
        for (i in 1 until sampleCount) {
            val currentSample = readSample(pcmFrame, i)
            if (prevSample * currentSample < 0) {
                crossings++
            }
            prevSample = currentSample
        }

        zcr = crossings.toDouble() / sampleCount.toDouble()
    }

    /** 从字节数组中读取指定位置的 16bit 采样值 */
    private fun readSample(buffer: ByteArray, index: Int): Short {
        val offset = index * 2
        return ((buffer[offset + 1].toInt() shl 8) or (buffer[offset].toInt() and 0xFF)).toShort()
    }
}
