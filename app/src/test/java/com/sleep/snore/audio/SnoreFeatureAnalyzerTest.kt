package com.sleep.snore.audio

import com.sleep.snore.data.model.SnoreType
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoreFeatureAnalyzerTest {

    @Test
    fun periodicity_highForPeriodicSine() {
        val pcm = amSinePcm(carrierHz = 180, burstPeriodMs = 1000, totalMs = 4000, amplitude = 12_000)

        val periodicity = SnoreFeatureAnalyzer.calculatePeriodicity(pcm)

        assertTrue("periodicity=$periodicity should be > 0.3", periodicity > 0.3f)
    }

    @Test
    fun periodicity_lowForContinuousTone() {
        val pcm = sinePcm(frequencyHz = 180, durationMs = 4000, amplitude = 12_000)

        val periodicity = SnoreFeatureAnalyzer.calculatePeriodicity(pcm)

        assertTrue("periodicity=$periodicity should be < 0.3", periodicity < 0.3f)
    }

    @Test
    fun spectralEntropy_lowForPureTone() {
        val samples = DoubleArray(512) { i ->
            sin(2.0 * PI * 180.0 * i / AudioConfig.SAMPLE_RATE) * 10_000.0
        }

        val entropy = SnoreFeatureAnalyzer.calculateSpectralEntropy(samples)

        assertTrue("entropy=$entropy should be < 0.3", entropy < 0.3f)
    }

    @Test
    fun spectralEntropy_highForWhiteNoise() {
        val random = Random(42)
        val samples = DoubleArray(512) { random.nextDouble() * 20_000 - 10_000 }

        val entropy = SnoreFeatureAnalyzer.calculateSpectralEntropy(samples)

        assertTrue("entropy=$entropy should be > 0.7", entropy > 0.7f)
    }

    @Test
    fun classifyType_mixedForHighEntropy() {
        val pcm = noisePcm(totalMs = 4000, amplitude = 12_000)

        val features = SnoreFeatureAnalyzer.analyze(pcm, peakDb = -8.0, durationMs = 4_000)

        assertEquals(SnoreType.MIXED, features.snoreType)
    }

    @Test
    fun analyze_lowPeriodicity_addsLabel() {
        val pcm = sinePcm(frequencyHz = 180, durationMs = 4000, amplitude = 12_000)

        val features = SnoreFeatureAnalyzer.analyze(pcm, peakDb = -8.0, durationMs = 4_000)

        assertTrue("label='${features.aiTypeLabel}' should contain 低节律性", features.aiTypeLabel.contains("低节律性"))
    }

    @Test
    fun analyze_shortPcm_returnsZeroFreq() {
        val pcm = ByteArray(512)

        val features = SnoreFeatureAnalyzer.analyze(pcm, peakDb = -8.0, durationMs = 100)

        assertEquals(0f, features.dominantFreq, 0.001f)
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

    private fun amSinePcm(carrierHz: Int, burstPeriodMs: Int, totalMs: Int, amplitude: Int): ByteArray {
        val sampleCount = AudioConfig.SAMPLE_RATE * totalMs / 1000
        val periodSamples = AudioConfig.SAMPLE_RATE * burstPeriodMs / 1000
        val burstSamples = periodSamples / 2
        val bytes = ByteArray(sampleCount * 2)
        for (index in 0 until sampleCount) {
            val phase = index % periodSamples
            val envelope = if (phase < burstSamples) 1.0 else 0.0
            val sample = (sin(2.0 * PI * carrierHz * index / AudioConfig.SAMPLE_RATE) * amplitude * envelope)
                .toInt()
                .toShort()
            val offset = index * 2
            bytes[offset] = (sample.toInt() and 0xFF).toByte()
            bytes[offset + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun noisePcm(totalMs: Int, amplitude: Int, seed: Long = 42L): ByteArray {
        val sampleCount = AudioConfig.SAMPLE_RATE * totalMs / 1000
        val bytes = ByteArray(sampleCount * 2)
        val random = Random(seed)
        for (index in 0 until sampleCount) {
            val sample = ((random.nextDouble() * 2 - 1) * amplitude).toInt().toShort()
            val offset = index * 2
            bytes[offset] = (sample.toInt() and 0xFF).toByte()
            bytes[offset + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
