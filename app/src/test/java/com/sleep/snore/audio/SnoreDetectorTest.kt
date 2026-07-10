package com.sleep.snore.audio

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.math.PI
import kotlin.math.sin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SnoreDetector 状态机单元测试
 *
 * 测试说明：
 * - SnoreDetector.processFrame() 为 internal，可直接调用以驱动纯 Kotlin 状态机逻辑
 *   (能量检测 → ZCR 过滤 → 连续帧计数 → 触发 / 复位)。
 * - 不调用 startListening()/stopListening()/close()，避免触发 AudioRecord 调用。
 * - SnoreDetector 通过测试时钟 (mockTimeMs)，按需推进模拟片段时长，
 *   无需 Thread.sleep。
 * - android.util.Log 通过 mockkStatic 桩控，规避 "not mocked" 异常，使丢弃分支可测。
 */
class SnoreDetectorTest {

    /** 模拟时钟，由测试用例按需推进 */
    private var mockTimeMs = 0L

    @Before
    fun setUp() {
        mockTimeMs = 0L
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun detector(callback: RecordingCallback): SnoreDetector {
        return SnoreDetector(
            callback = callback,
            silenceThresholdDb = -40.0,
            clockNowMs = { mockTimeMs }
        )
    }

    private fun feedFrame(detector: SnoreDetector, pcmFrame: ByteArray) {
        detector.processFrame(pcmFrame)
    }

    /** 生成正弦波 PCM 帧 (16bit little-endian, 800 samples = 1 帧) */
    private fun sineFrame(frequencyHz: Int, amplitude: Int): ByteArray {
        val sampleCount = AudioConfig.FRAME_SIZE
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

    /** 生成静音帧 (全零, RMS = -∞ → hasSound=false) */
    private fun silenceFrame(): ByteArray = ByteArray(AudioConfig.FRAME_BYTES)

    /** 记录回调事件的简单桩 */
    private class RecordingCallback : SnoreDetector.SnoreCallback {
        var startedCount = 0
        var endedCount = 0
        var lastStartedDb: Double = Double.NaN
        var lastEndedDurationMs: Long = -1L

        override fun onSnoreStarted(timestamp: Long, db: Double) {
            startedCount++
            lastStartedDb = db
        }

        override fun onSnoreEnded(startTimestamp: Long, durationMs: Long, pcmData: ByteArray, peakDb: Double) {
            endedCount++
            lastEndedDurationMs = durationMs
        }
    }

    @Test
    fun silenceFrames_doNotTriggerSnoreEvent() {
        val callback = RecordingCallback()
        val detector = detector(callback)

        // 传入 10 帧静音 (RMS 低于能量阈值 -40dB)
        repeat(10) {
            feedFrame(detector, silenceFrame())
        }

        // 不应触发任何鼾声事件
        assertEquals(0, callback.startedCount)
        assertEquals(0, callback.endedCount)
    }

    @Test
    fun shortSnoreBurst_doesNotTriggerEvent() {
        val callback = RecordingCallback()
        val detector = detector(callback)

        // 传入 3 帧疑似鼾声 (180Hz 正弦波: 高能量 + ZCR≈0.0225 命中 [0.006, 0.038])
        // triggerFrameCount = 4，3 帧不足以触达阈值
        repeat(3) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }

        assertEquals("3 帧不足以触发 onSnoreStarted", 0, callback.startedCount)

        // 传入 1 帧有声音但非鼾声 (2000Hz 高频: ZCR≈0.25 超出鼾声区间)
        // 走 else 分支 → resetCurrentSegment()，不调用 Log
        feedFrame(detector, sineFrame(frequencyHz = 2000, amplitude = 12_000))

        assertEquals("非鼾声帧后仍不应触发", 0, callback.startedCount)
        assertEquals("未触发片段不应产生结束事件", 0, callback.endedCount)
    }

    @Test
    fun validSnoreSequence_triggersStartedCallback() {
        val callback = RecordingCallback()
        val detector = detector(callback)

        // 连续传入 4 帧 180Hz 疑似鼾声 → 第 4 帧 snoreFrameCount==triggerFrameCount 触发 onSnoreStarted
        repeat(4) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }

        assertEquals("第 4 帧应触发一次 onSnoreStarted", 1, callback.startedCount)
        assertTrue("回调 dB 应为有限值", callback.lastStartedDb.isFinite())
        assertTrue("回调 dB 应高于静音阈值 -40dB", callback.lastStartedDb > -40.0)
    }

    @Test
    fun stateResetsAfterFlush_lowEnergyDoesNotRetrigger() {
        val callback = RecordingCallback()
        val detector = detector(callback)

        // 1) 触发鼾声事件
        mockTimeMs = 1_000L
        repeat(4) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }
        assertEquals(1, callback.startedCount)

        // 2) 推进模拟时钟 600ms，确保片段时长 >= 500ms，避免 flush 走 Log.d 丢弃分支
        mockTimeMs = 1_600L

        // 3) 传入 48 帧静音 (默认 MEDIUM 灵敏度 endSilenceFrames=48) → flushCurrentSegment → onSnoreEnded → resetCurrentSegment
        repeat(48) {
            feedFrame(detector, silenceFrame())
        }

        assertEquals("触发后应产生 1 次结束事件", 1, callback.endedCount)
        assertTrue("片段时长应 >= 500ms", callback.lastEndedDurationMs >= 500L)

        // 4) 状态已复位，后续低能量帧不应误触发
        repeat(10) {
            feedFrame(detector, silenceFrame())
        }

        assertEquals("复位后低能量帧不应触发开始事件", 1, callback.startedCount)
        assertEquals("复位后低能量帧不应触发结束事件", 1, callback.endedCount)

        // 5) 验证状态确实复位: 新的鼾声序列应能再次触发 onSnoreStarted
        repeat(4) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }

        assertEquals("复位后新序列应再次触发", 2, callback.startedCount)
    }

    /**
     * maxSegmentFrames 封顶触发 flush:
     * 连续喂入 maxSegmentFrames 个鼾声帧后，addSegmentFrame 触发 flushCurrentSegment。
     * 默认 maxSegmentDurationSec=60, FRAME_DURATION_MS=50 → maxSegmentFrames = 60000/50 = 1200。
     */
    @Test
    fun maxSegmentFrames_triggersFlush() {
        val callback = RecordingCallback()
        val detector = detector(callback)
        val snoreFrame = sineFrame(frequencyHz = 180, amplitude = 12_000)
        // 默认 maxSegmentDurationSec=60, FRAME_DURATION_MS=50 → maxSegmentFrames=1200
        val maxSegmentFrames = 1200

        // 片段开始时间
        mockTimeMs = 1_000L

        // 喂入 (maxSegmentFrames - 1) 帧鼾声，缓冲区累积但不 flush
        // 第 4 帧触发 onSnoreStarted
        repeat(maxSegmentFrames - 1) {
            feedFrame(detector, snoreFrame)
        }
        assertEquals("第 4 帧应触发 onSnoreStarted", 1, callback.startedCount)
        assertEquals("未达封顶不应 flush", 0, callback.endedCount)

        // 推进时钟确保 durationMs >= 500ms，避免 flush 走丢弃分支
        mockTimeMs = 1_600L

        // 喂入第 maxSegmentFrames 帧 → addSegmentFrame 触发 flush
        feedFrame(detector, snoreFrame)

        assertEquals("封顶应触发 1 次 onSnoreEnded", 1, callback.endedCount)
        assertTrue("片段时长应 >= 500ms", callback.lastEndedDurationMs >= 500L)
    }

    /**
     * durationMs < 500 丢弃分支:
     * 触发鼾声后立即 flush (durationMs < 500)，片段应被丢弃，不产生 onSnoreEnded。
     */
    @Test
    fun shortSegment_isDiscarded() {
        val callback = RecordingCallback()
        val detector = detector(callback)

        // 触发鼾声
        mockTimeMs = 1_000L
        repeat(4) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }
        assertEquals(1, callback.startedCount)

        // 仅推进 200ms (< 500ms 阈值)
        mockTimeMs = 1_200L

        // 传入 48 帧静音 → flush → durationMs=200 < 500 → 丢弃 (Log.d 已桩控)
        repeat(48) {
            feedFrame(detector, silenceFrame())
        }

        assertEquals("过短片段应被丢弃，不产生结束事件", 0, callback.endedCount)
    }

    /**
     * hasTriggeredSegment=false 时非鼾声帧走 reset:
     * 喂入 < triggerFrameCount 帧鼾声 (未触发) 后喂入非鼾声帧，应 resetCurrentSegment，
     * silenceFrameCount 不累加；后续新鼾声序列需从 0 重新计数才能触发。
     */
    @Test
    fun untriggeredSegment_nonSnoreFrame_resets() {
        val callback = RecordingCallback()
        val detector = detector(callback)

        // 喂入 3 帧鼾声 (< triggerFrameCount=4)，inSnoreSegment=true 但 hasTriggeredSegment=false
        mockTimeMs = 1_000L
        repeat(3) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }
        assertEquals("3 帧不应触发", 0, callback.startedCount)

        // 喂入非鼾声帧 (2000Hz: 有声音但 ZCR 超出鼾声区间) → else 分支 → resetCurrentSegment
        feedFrame(detector, sineFrame(frequencyHz = 2000, amplitude = 12_000))

        assertEquals("非鼾声帧不应触发开始事件", 0, callback.startedCount)
        assertEquals("reset 不应产生结束事件", 0, callback.endedCount)

        // 验证状态已复位: 新鼾声序列需 4 帧才能触发
        // (若未 reset，snoreFrameCount 仍为 3，1 帧即可触达 triggerFrameCount=4)
        mockTimeMs = 2_000L
        repeat(3) {
            feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        }
        assertEquals("reset 后 3 帧不应触发", 0, callback.startedCount)

        feedFrame(detector, sineFrame(frequencyHz = 180, amplitude = 12_000))
        assertEquals("reset 后第 4 帧应触发", 1, callback.startedCount)
    }

    /**
     * inSnoreSegment && hasTriggeredSegment 时非鼾声帧累加 silenceFrameCount:
     * 触发后喂入非鼾声帧 (有声音)，应累加 silenceFrameCount；达 endSilenceFrames 后 flush。
     */
    @Test
    fun triggeredSegment_nonSnoreFrame_accumulatesSilenceThenFlushes() {
        val callback = RecordingCallback()
        val detector = detector(callback)
        val snoreFrame = sineFrame(frequencyHz = 180, amplitude = 12_000)
        val nonSnoreFrame = sineFrame(frequencyHz = 2000, amplitude = 12_000)

        // 触发鼾声
        mockTimeMs = 1_000L
        repeat(4) {
            feedFrame(detector, snoreFrame)
        }
        assertEquals(1, callback.startedCount)

        // 喂入 (endSilenceFrames - 1) 帧非鼾声，silenceFrameCount 累加但不 flush
        // 默认 MEDIUM: endSilenceFrames=48
        repeat(47) {
            feedFrame(detector, nonSnoreFrame)
        }
        assertEquals("未达 endSilenceFrames 不应 flush", 0, callback.endedCount)

        // 推进时钟确保 durationMs >= 500ms
        mockTimeMs = 1_600L

        // 喂入第 48 帧非鼾声 → silenceFrameCount=48 >= endSilenceFrames → flush
        feedFrame(detector, nonSnoreFrame)

        assertEquals("达 endSilenceFrames 应 flush", 1, callback.endedCount)
        assertTrue("片段时长应 >= 500ms", callback.lastEndedDurationMs >= 500L)
    }
}
