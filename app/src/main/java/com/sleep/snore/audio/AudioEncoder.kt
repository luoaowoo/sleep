package com.sleep.snore.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * OPUS 音频编码器
 *
 * 将 PCM 原始数据编码为 .ogg (OPUS) 格式，
 * 大幅减小存储体积 (约 1/10)。
 */
class AudioEncoder {

    private var codec: MediaCodec? = null
    private var outputStream: FileOutputStream? = null

    /**
     * 将 PCM 数据编码为 OPUS 并写入文件
     *
     * @param pcmData 16bit PCM 原始数据
     * @param outputDir 输出目录
     * @param fileName 文件名 (不含扩展名)
     * @return 输出文件，失败返回 null
     */
    fun encodeToOpus(
        pcmData: ByteArray,
        outputDir: File,
        fileName: String
    ): File? {
        if (pcmData.isEmpty()) return null

        try {
            // 查找 OPUS 编码器
            val mime = "audio/opus"
            val codecName = findEncoderForMime(mime)
                ?: run {
                    Log.w(TAG, "OPUS 编码器不可用，使用 WAV 回退")
                    return saveAsWav(pcmData, outputDir, fileName)
                }

            val format = MediaFormat.createAudioFormat(mime, AudioConfig.SAMPLE_RATE, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 16000) // 16kbps
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, pcmData.size)
                setInteger(MediaFormat.KEY_PCM_ENCODING, AudioConfig.BITS_PER_SAMPLE)
            }

            codec = MediaCodec.createByCodecName(codecName)
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val outputFile = File(outputDir, "$fileName.ogg")
            outputStream = FileOutputStream(outputFile)

            codec?.start()

            // 写入 OGG Header (简化版)
            val oggHeader = createOggHeader()
            outputStream?.write(oggHeader)

            // 编码数据
            val inputBuffers = codec?.inputBuffers
            val outputBuffers = codec?.outputBuffers
            val bufferInfo = MediaCodec.BufferInfo()

            var inputIndex = codec?.dequeueInputBuffer(10_000) ?: -1
            if (inputIndex >= 0 && inputBuffers != null) {
                val inputBuffer = inputBuffers[inputIndex]
                inputBuffer.clear()
                inputBuffer.put(pcmData)
                codec?.queueInputBuffer(inputIndex, 0, pcmData.size, 0, 0)
            }

            // 发送 EOS
            inputIndex = codec?.dequeueInputBuffer(10_000) ?: -1
            if (inputIndex >= 0) {
                codec?.queueInputBuffer(
                    inputIndex, 0, 0, 0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            // 读取编码输出
            var outputDone = false
            while (!outputDone) {
                val outputIndex = codec?.dequeueOutputBuffer(bufferInfo, 10_000) ?: -1
                when {
                    outputIndex >= 0 && outputBuffers != null -> {
                        val outputBuffer = outputBuffers[outputIndex]
                        if (bufferInfo.size > 0) {
                            val chunk = ByteArray(bufferInfo.size)
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            outputBuffer.get(chunk)
                            outputStream?.write(createOggPage(chunk))
                        }
                        codec?.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                }
            }

            outputStream?.flush()
            Log.d(TAG, "编码完成: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "OPUS 编码失败: ${e.message}", e)
            return saveAsWav(pcmData, outputDir, fileName)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            outputStream = null
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            codec = null
        }
    }

    /**
     * WAV 回退方案
     */
    private fun saveAsWav(pcmData: ByteArray, outputDir: File, fileName: String): File? {
        return try {
            val file = File(outputDir, "$fileName.wav")
            FileOutputStream(file).use { fos ->
                // WAV header
                val header = ByteArray(44)
                // RIFF header
                "RIFF".toByteArray().copyInto(header, 0)
                writeIntLE(header, 4, 36 + pcmData.size)
                "WAVE".toByteArray().copyInto(header, 8)
                // fmt chunk
                "fmt ".toByteArray().copyInto(header, 12)
                writeIntLE(header, 16, 16) // chunk size
                writeShortLE(header, 20, 1) // PCM
                writeShortLE(header, 22, 1) // mono
                writeIntLE(header, 24, AudioConfig.SAMPLE_RATE)
                writeIntLE(header, 28, AudioConfig.SAMPLE_RATE * 2) // byte rate
                writeShortLE(header, 32, 2) // block align
                writeShortLE(header, 34, 16) // bits per sample
                // data chunk
                "data".toByteArray().copyInto(header, 36)
                writeIntLE(header, 40, pcmData.size)

                fos.write(header)
                fos.write(pcmData)
            }
            Log.d(TAG, "WAV 回退保存: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "WAV 保存失败: ${e.message}", e)
            null
        }
    }

    private fun findEncoderForMime(mime: String): String? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            for (type in codecInfo.supportedTypes) {
                if (type.equals(mime, ignoreCase = true)) {
                    return codecInfo.name
                }
            }
        }
        return null
    }

    /** 简化的 OGG 页头 */
    private fun createOggHeader(): ByteArray {
        return byteArrayOf(0x4F, 0x67, 0x67, 0x53, // "OggS"
            0, 2, // version
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // ...简化header
            1, // page segments
            0x13  // segment table (19 bytes of OpusHead)
        ) + ByteArray(19) { 0x4F.toString().toByte() } // placeholder OpusHead
    }

    /** 简化的 OGG 数据页 */
    private fun createOggPage(data: ByteArray): ByteArray {
        return byteArrayOf(0x4F, 0x67, 0x67, 0x53, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            data.size.toByte()
        ) + data
    }

    private fun writeIntLE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    companion object {
        private const val TAG = "AudioEncoder"
    }
}
