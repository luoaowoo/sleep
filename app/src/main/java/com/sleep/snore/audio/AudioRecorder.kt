package com.sleep.snore.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.Closeable

/**
 * 低层级 PCM 录音引擎
 *
 * 使用 AudioRecord + UNPROCESSED 源，16kHz/16bit/Mono
 * 在独立线程中持续读取，通过 [FrameCallback] 将每帧数据分发给分析器
 */
class AudioRecorder(
    private val callback: FrameCallback
) : Closeable {

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false

    /** 是否正在录音 */
    val isActive: Boolean get() = isRecording

    /**
     * 启动录音。需要在拥有 RECORD_AUDIO 权限后调用。
     * @throws IllegalStateException 如果设备不支持此配置
     */
    fun start() {
        if (isRecording) return

        val minBuf = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNELS,
            AudioConfig.BITS_PER_SAMPLE
        )
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("AudioRecord: 不支持的音频配置")
        }

        val bufferSize = maxOf(minBuf * 2, AudioConfig.FRAME_BYTES * 4)

        recorder = createRecorder(bufferSize)

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord 初始化失败")
        }

        recorder?.startRecording()
        isRecording = true

        recordingThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val buffer = ByteArray(AudioConfig.FRAME_BYTES)
            while (isRecording) {
                val bytesRead = recorder?.read(buffer, 0, buffer.size) ?: -1
                if (bytesRead > 0) {
                    callback.onFrame(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    Log.w(TAG, "AudioRecord read error: $bytesRead")
                }
            }
        }, "AudioRecorder-Thread").apply { start() }

        Log.i(TAG, "录音已启动 (${AudioConfig.SAMPLE_RATE}Hz, ${AudioConfig.BITS_PER_SAMPLE}bit, Mono)")
    }

    private fun createRecorder(bufferSize: Int): AudioRecord {
        val sources = listOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        var lastError: Throwable? = null
        for (source in sources) {
            try {
                val candidate = AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(AudioConfig.SAMPLE_RATE)
                            .setEncoding(AudioConfig.BITS_PER_SAMPLE)
                            .setChannelMask(AudioConfig.CHANNELS)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    Log.i(TAG, "AudioRecord source selected: $source")
                    return candidate
                }
                candidate.release()
            } catch (error: Throwable) {
                lastError = error
                Log.w(TAG, "AudioRecord source failed: $source", error)
            }
        }

        throw IllegalStateException("AudioRecord 初始化失败", lastError)
    }

    /**
     * 停止录音，释放资源
     */
    fun stop() {
        isRecording = false
        recordingThread?.let {
            it.interrupt()
            it.join(2000)
        }
        recordingThread = null

        recorder?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        recorder = null

        Log.i(TAG, "录音已停止")
    }

    override fun close() {
        stop()
    }

    interface FrameCallback {
        /** 每 50ms 触发一次，携带 1600 字节原始 PCM 帧 */
        fun onFrame(pcmFrame: ByteArray)
    }

    companion object {
        private const val TAG = "AudioRecorder"
    }
}
