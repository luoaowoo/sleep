package com.sleep.snore.audio

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEncoderTest {

    @Test
    fun encodeToOpus_writesPlayableWavContainer() {
        val outputDir = createTempDir(prefix = "snore_audio_test")
        val pcmData = byteArrayOf(1, 0, 2, 0, 3, 0, 4, 0)

        try {
            val file = AudioEncoder().encodeToOpus(pcmData, outputDir, "clip")

            assertNotNull(file)
            file!!
            assertTrue(file.exists())
            assertEquals("clip.wav", file.name)

            val bytes = file.readBytes()
            assertEquals(44 + pcmData.size, bytes.size)
            assertEquals("RIFF", bytes.ascii(0, 4))
            assertEquals(36 + pcmData.size, bytes.intLe(4))
            assertEquals("WAVE", bytes.ascii(8, 4))
            assertEquals("fmt ", bytes.ascii(12, 4))
            assertEquals(16, bytes.intLe(16))
            assertEquals(1, bytes.shortLe(20))
            assertEquals(1, bytes.shortLe(22))
            assertEquals(AudioConfig.SAMPLE_RATE, bytes.intLe(24))
            assertEquals(AudioConfig.SAMPLE_RATE * 2, bytes.intLe(28))
            assertEquals(2, bytes.shortLe(32))
            assertEquals(16, bytes.shortLe(34))
            assertEquals("data", bytes.ascii(36, 4))
            assertEquals(pcmData.size, bytes.intLe(40))
            assertArrayEquals(pcmData, bytes.copyOfRange(44, bytes.size))
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun encodeToOpus_emptyPcm_returnsNullAndDoesNotCreateFile() {
        val outputDir = createTempDir(prefix = "snore_audio_empty_test")

        try {
            val file = AudioEncoder().encodeToOpus(ByteArray(0), outputDir, "empty")

            assertEquals(null, file)
            assertTrue(outputDir.listFiles()?.isEmpty() ?: true)
        } finally {
            outputDir.deleteRecursively()
        }
    }

    private fun ByteArray.ascii(offset: Int, length: Int): String {
        return copyOfRange(offset, offset + length).decodeToString()
    }

    private fun ByteArray.intLe(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun ByteArray.shortLe(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)
    }
}
