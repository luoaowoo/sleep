package com.sleep.snore.audio

import android.media.AudioFormat

/** 音频录制配置常量 */
object AudioConfig {
    /** 采样率: 16kHz (鼾声频率 50-500Hz, 16k 足够捕获谐波) */
    const val SAMPLE_RATE = 16000

    /** 位深: 16bit PCM */
    const val BITS_PER_SAMPLE = AudioFormat.ENCODING_PCM_16BIT

    /** 单声道 */
    const val CHANNELS = AudioFormat.CHANNEL_IN_MONO

    /** 分析帧大小: 800 samples = 50ms @ 16kHz */
    const val FRAME_SIZE = 800

    /** 分析帧时长毫秒 */
    const val FRAME_DURATION_MS = 50

    /** 每帧字节数: 800 * 2 = 1600 bytes */
    const val FRAME_BYTES = FRAME_SIZE * 2
}
