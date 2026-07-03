package com.sleep.snore.audio

import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * 鼾声音频片段保存器
 *
 * 目前优先使用 WAV，确保系统 MediaPlayer 能稳定回放。
 * 后续可替换为合法 Ogg/Opus muxer 来进一步压缩体积。
 */
class AudioEncoder {

    /**
     * 将 PCM 数据保存为可回放音频文件。
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
        return saveAsWav(pcmData, outputDir, fileName)
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
