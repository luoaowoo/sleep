package com.sleep.snore.audio

import android.util.Log
import com.sleep.snore.data.model.Sensitivity
import java.io.Closeable

/**
 * 鼾声检测器 — 三级渐进式过滤
 *
 * 第1层: RMS 能量检测 → 过滤静音
 * 第2层: ZCR (过零率) → 快速区分鼾声 vs 语音/环境音
 * 第3层: 基于能量+ZCR+Goertzel 主频的规则型单层分类
 *
 * 同时管理录音触发器:
 * - 检测到疑似鼾声时通过 [SnoreCallback] 通知
 * - 静音超过阈值时通知结束
 */
class SnoreDetector(
    private val callback: SnoreCallback,
    silenceThresholdDb: Double = -40.0,
    maxSegmentDurationSec: Int = DEFAULT_MAX_SEGMENT_DURATION_SEC,
    sensitivity: Sensitivity = Sensitivity.MEDIUM,
    private val clockNowMs: () -> Long = System::currentTimeMillis
) : Closeable {

    private val audioRecorder: AudioRecorder
    private val energyDetector = EnergyDetector(silenceThresholdDb)

    /** 灵敏度预设：ZCR 区间 / 触发帧数 / 结束静音帧数 */
    private val preset = when (sensitivity) {
        Sensitivity.LOW -> DetectorPreset(
            zcrMin = 0.008,
            zcrMax = 0.032,
            triggerFrameCount = 6,
            endSilenceFrames = 60
        )
        Sensitivity.MEDIUM -> DetectorPreset(
            zcrMin = 0.006,
            zcrMax = 0.038,
            triggerFrameCount = 4,
            endSilenceFrames = 48
        )
        Sensitivity.HIGH -> DetectorPreset(
            zcrMin = 0.005,
            zcrMax = 0.045,
            triggerFrameCount = 3,
            endSilenceFrames = 30
        )
    }

    private val zcrDetector = ZeroCrossingDetector(
        snoreZcrMin = preset.zcrMin,
        snoreZcrMax = preset.zcrMax
    )

    /** 当前是否处于鼾声片段中 */
    @Volatile
    private var inSnoreSegment = false

    /** 鼾声片段开始时间 */
    private var segmentStartTime = 0L

    /** 鼾声片段累积的 PCM 数据 */
    private val segmentBuffer = mutableListOf<ByteArray>()

    /** 连续静音帧计数 (用于检测鼾声结束) */
    private var silenceFrameCount = 0

    /** 连续鼾声帧计数 (用于减少误触发) */
    private var snoreFrameCount = 0

    /** 是否已经达到触发阈值，只有触发后才保存片段 */
    private var hasTriggeredSegment = false

    /** 触发鼾声录音所需连续帧数 */
    private val triggerFrameCount = preset.triggerFrameCount

    /** 结束鼾声片段所需连续静音帧数 */
    private val endSilenceFrames = preset.endSilenceFrames

    /** 单个片段最大帧数，避免长噪声导致内存持续增长 */
    private val maxSegmentFrames = ((maxSegmentDurationSec.coerceIn(15, 120) * 1000) / AudioConfig.FRAME_DURATION_MS)
        .coerceAtLeast(triggerFrameCount + endSilenceFrames)

    init {
        audioRecorder = AudioRecorder(object : AudioRecorder.FrameCallback {
            override fun onFrame(pcmFrame: ByteArray) {
                processFrame(pcmFrame)
            }

            override fun onReadError(errorCode: Int, consecutiveErrors: Int) {
                callback.onRecorderError(errorCode, consecutiveErrors)
            }
        })
    }

    /** 开始监听 (不开始录音片段，仅分析) */
    fun startListening() {
        audioRecorder.start()
        Log.i(TAG, "开始鼾声监听")
    }

    /** 停止监听 */
    fun stopListening() {
        audioRecorder.stop()
        flushCurrentSegment()
        Log.i(TAG, "停止鼾声监听")
    }

    override fun close() {
        stopListening()
    }

    internal fun processFrame(pcmFrame: ByteArray) {
        // 第1层: 能量检测
        energyDetector.process(pcmFrame)

        if (!energyDetector.hasSound) {
            // 静音
            if (inSnoreSegment) {
                silenceFrameCount++
                addSegmentFrame(pcmFrame) // 保留缓冲
                if (silenceFrameCount >= endSilenceFrames) {
                    flushCurrentSegment()
                }
            }
            return
        }

        // 有声音
        // 第2层: ZCR 检测
        zcrDetector.process(pcmFrame)

        if (zcrDetector.isPotentialSnore) {
            // 疑似鼾声
            snoreFrameCount++
            silenceFrameCount = 0

            if (!inSnoreSegment) {
                segmentStartTime = clockNowMs()
                inSnoreSegment = true
                segmentBuffer.clear()
            }
            addSegmentFrame(pcmFrame)

            // 触发达标通知
            if (snoreFrameCount == triggerFrameCount) {
                hasTriggeredSegment = true
                callback.onSnoreStarted(
                    timestamp = segmentStartTime,
                    db = energyDetector.currentDb
                )
            }
        } else {
            // 有声音但非鼾声(语音/环境音)
            if (inSnoreSegment) {
                if (hasTriggeredSegment) {
                    silenceFrameCount++
                    addSegmentFrame(pcmFrame)
                    if (silenceFrameCount >= endSilenceFrames) {
                        flushCurrentSegment()
                    }
                } else {
                    resetCurrentSegment()
                }
            } else {
                snoreFrameCount = 0
            }
        }
    }

    private fun addSegmentFrame(pcmFrame: ByteArray) {
        segmentBuffer.add(pcmFrame)
        if (segmentBuffer.size >= maxSegmentFrames) {
            flushCurrentSegment()
        }
    }

    private fun flushCurrentSegment() {
        if (!inSnoreSegment) return

        val durationMs = if (segmentStartTime > 0) {
            clockNowMs() - segmentStartTime
        } else 0L

        if (!hasTriggeredSegment || durationMs < 500) {
            // 太短，可能是误触发，丢弃
            Log.d(TAG, "丢弃过短片段: ${durationMs}ms")
        } else {
            val pcmData = PcmBuffer.concatenate(segmentBuffer)
            callback.onSnoreEnded(
                startTimestamp = segmentStartTime,
                durationMs = durationMs,
                pcmData = pcmData,
                peakDb = calculatePeakDb()
            )
        }

        resetCurrentSegment()
    }

    private fun resetCurrentSegment() {
        inSnoreSegment = false
        hasTriggeredSegment = false
        segmentBuffer.clear()
        snoreFrameCount = 0
        silenceFrameCount = 0
    }

    /** 从 segmentBuffer 计算 peak dB */
    private fun calculatePeakDb(): Double {
        return segmentBuffer.maxOfOrNull { energyDetector.calculateDb(it) }
            ?: Double.NEGATIVE_INFINITY
    }

    interface SnoreCallback {
        /** 鼾声片段开始 */
        fun onSnoreStarted(timestamp: Long, db: Double)

        /** 鼾声片段结束，包含完整 PCM 数据 */
        fun onSnoreEnded(startTimestamp: Long, durationMs: Long, pcmData: ByteArray, peakDb: Double)

        fun onRecorderError(errorCode: Int, consecutiveErrors: Int) = Unit
    }

    companion object {
        private const val TAG = "SnoreDetector"
        private const val DEFAULT_MAX_SEGMENT_DURATION_SEC = 60
    }

    private data class DetectorPreset(
        val zcrMin: Double,
        val zcrMax: Double,
        val triggerFrameCount: Int,
        val endSilenceFrames: Int
    )
}
