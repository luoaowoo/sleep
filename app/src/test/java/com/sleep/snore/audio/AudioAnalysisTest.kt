package com.sleep.snore.audio

import com.sleep.snore.data.model.SnoreType
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAnalysisTest {

    @Test
    fun energyDetectorHandlesOddLengthPcm() {
        val db = EnergyDetector().calculateDb(byteArrayOf(0, 1, 2))

        assertTrue(db.isFinite())
    }

    @Test
    fun snoreFeatureAnalyzerFindsDominantFrequency() {
        val pcm = sinePcm(frequencyHz = 180, durationMs = 1_000, amplitude = 12_000)

        val features = SnoreFeatureAnalyzer.analyze(pcm, peakDb = -8.0, durationMs = 1_000)

        assertTrue(features.dominantFreq in 170f..190f)
        assertEquals(SnoreType.TONGUE_ROOT, features.snoreType)
        assertTrue(features.confidence > 0.3f)
    }

    @Test
    fun snoreFeatureAnalyzerClassifiesLongMidBandAsMixed() {
        val pcm = sinePcm(frequencyHz = 180, durationMs = 1_000, amplitude = 12_000)

        val features = SnoreFeatureAnalyzer.analyze(pcm, peakDb = -8.0, durationMs = 9_000)

        assertEquals(SnoreType.MIXED, features.snoreType)
    }

    private fun sinePcm(frequencyHz: Int, durationMs: Int, amplitude: Int): ByteArray {
        val sampleCount = AudioConfig.SAMPLE_RATE * durationMs / 1000
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
}
