package com.sleep.snore.audio

import android.util.Log
import java.io.Closeable

/**
 * 鼾声检测器 — 三级渐进式过滤
 *
 * 第1层: RMS 能量检测 → 过滤静音
 * 第2层: ZCR (过零率) → 快速区分鼾声 vs 语音/环境音
 * 第3层: (预留) MFCC + TFLite → 精确二分类
 *
 * 同时管理录音触发器:
 * - 检测到疑似鼾声时通过 [SnoreCallback] 通知
 * - 静音超过阈值时通知结束
 */
class SnoreDetector(
    private val callback: SnoreCallback
) : Closeable {

    private val audioRecorder: AudioRecorder
    private val energyDetector = EnergyDetector()
    private val zcrDetector = ZeroCrossingDetector()

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

    /** 触发鼾声录音所需连续帧数 (约 200ms = 4帧) */
    private val triggerFrameCount = 4

    /** 结束鼾声片段所需连续静音帧数 (约 1秒 = 20帧) */
    private val endSilenceFrames = 20

    init {
        audioRecorder = AudioRecorder(object : AudioRecorder.FrameCallback {
            override fun onFrame(pcmFrame: ByteArray) {
                processFrame(pcmFrame)
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

    private fun processFrame(pcmFrame: ByteArray) {
        // 第1层: 能量检测
        energyDetector.process(pcmFrame)

        if (!energyDetector.hasSound) {
            // 静音
            if (inSnoreSegment) {
                silenceFrameCount++
                segmentBuffer.add(pcmFrame) // 保留缓冲
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
                segmentStartTime = System.currentTimeMillis()
                inSnoreSegment = true
                segmentBuffer.clear()
            }
            segmentBuffer.add(pcmFrame)

            // 触发达标通知
            if (snoreFrameCount == triggerFrameCount) {
                callback.onSnoreStarted(
                    timestamp = segmentStartTime,
                    db = energyDetector.currentDb
                )
            }
        } else {
            // 有声音但非鼾声(语音/环境音)
            if (inSnoreSegment) {
                silenceFrameCount++
                segmentBuffer.add(pcmFrame)
                if (silenceFrameCount >= endSilenceFrames) {
                    flushCurrentSegment()
                }
            } else {
                // 非鼾声且不在片段中，丢弃
            }
        }
    }

    private fun flushCurrentSegment() {
        if (!inSnoreSegment) return

        val durationMs = if (segmentStartTime > 0) {
            System.currentTimeMillis() - segmentStartTime
        } else 0L

        if (durationMs < 500) {
            // 太短，可能是误触发，丢弃
            Log.d(TAG, "丢弃过短片段: ${durationMs}ms")
        } else {
            val pcmData = segmentBuffer.flatMap { it.toList() }.toByteArray()
            callback.onSnoreEnded(
                startTimestamp = segmentStartTime,
                durationMs = durationMs,
                pcmData = pcmData,
                peakDb = calculatePeakDb()
            )
        }

        inSnoreSegment = false
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
    }

    companion object {
        private const val TAG = "SnoreDetector"
    }
}
