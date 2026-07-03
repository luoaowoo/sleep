package com.sleep.snore.audio

import com.sleep.snore.data.model.SnoreType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class SnoreAudioFeatures(
    val avgDb: Float,
    val peakDb: Float,
    val dominantFreq: Float,
    val snoreType: SnoreType,
    val confidence: Float,
    val aiTypeLabel: String
)

object SnoreFeatureAnalyzer {

    fun analyze(pcmData: ByteArray, peakDb: Double, durationMs: Long): SnoreAudioFeatures {
        val avgDb = EnergyDetector().calculateDb(pcmData).toFloat()
        val dominantFreq = estimateDominantFrequency(pcmData)
        val snoreType = classifyType(dominantFreq, avgDb, durationMs)
        val confidence = confidenceFor(dominantFreq, avgDb, durationMs)
        val label = "${snoreType.label} · 置信度 ${(confidence * 100).roundToInt()}%"
        return SnoreAudioFeatures(
            avgDb = avgDb,
            peakDb = peakDb.toFloat(),
            dominantFreq = dominantFreq,
            snoreType = snoreType,
            confidence = confidence,
            aiTypeLabel = label
        )
    }

    private fun estimateDominantFrequency(pcmData: ByteArray): Float {
        val samples = readAnalysisWindow(pcmData)
        if (samples.size < MIN_WINDOW_SAMPLES) return 0f

        var bestFrequency = 0
        var bestPower = 0.0
        for (frequency in 50..500 step 10) {
            val power = goertzelPower(samples, frequency)
            if (power > bestPower) {
                bestPower = power
                bestFrequency = frequency
            }
        }
        return bestFrequency.toFloat()
    }

    private fun readAnalysisWindow(pcmData: ByteArray): DoubleArray {
        val totalSamples = pcmData.size / 2
        if (totalSamples <= 0) return DoubleArray(0)

        val windowSize = minOf(MAX_WINDOW_SAMPLES, totalSamples)
        val startSample = findLoudestWindowStart(pcmData, totalSamples, windowSize)
        val samples = DoubleArray(windowSize)
        for (i in 0 until windowSize) {
            val offset = (startSample + i) * 2
            val sample = ((pcmData[offset + 1].toInt() shl 8) or (pcmData[offset].toInt() and 0xFF)).toShort()
            val window = 0.5 - 0.5 * cos(2.0 * PI * i / (windowSize - 1).coerceAtLeast(1))
            samples[i] = sample.toDouble() * window
        }
        return samples
    }

    private fun findLoudestWindowStart(pcmData: ByteArray, totalSamples: Int, windowSize: Int): Int {
        if (totalSamples <= windowSize) return 0

        var bestStart = 0
        var bestEnergy = 0.0
        val step = maxOf(windowSize / 2, 1)
        var start = 0
        while (start + windowSize <= totalSamples) {
            var energy = 0.0
            var i = start
            while (i < start + windowSize) {
                val offset = i * 2
                val sample = ((pcmData[offset + 1].toInt() shl 8) or (pcmData[offset].toInt() and 0xFF)).toShort()
                energy += sample.toDouble() * sample.toDouble()
                i++
            }
            if (energy > bestEnergy) {
                bestEnergy = energy
                bestStart = start
            }
            start += step
        }
        return bestStart
    }

    private fun goertzelPower(samples: DoubleArray, targetFrequency: Int): Double {
        val normalizedFrequency = targetFrequency.toDouble() / AudioConfig.SAMPLE_RATE.toDouble()
        val coefficient = 2.0 * cos(2.0 * PI * normalizedFrequency)
        var previous = 0.0
        var previous2 = 0.0
        for (sample in samples) {
            val current = sample + coefficient * previous - previous2
            previous2 = previous
            previous = current
        }
        val real = previous - previous2 * cos(2.0 * PI * normalizedFrequency)
        val imaginary = previous2 * sin(2.0 * PI * normalizedFrequency)
        return real * real + imaginary * imaginary
    }

    private fun classifyType(dominantFreq: Float, avgDb: Float, durationMs: Long): SnoreType {
        if (dominantFreq <= 0f || avgDb < -65f) return SnoreType.UNKNOWN
        return when {
            durationMs >= 8_000 && dominantFreq in 90f..260f -> SnoreType.MIXED
            dominantFreq < 130f -> SnoreType.SOFT_PALATE
            dominantFreq < 260f -> SnoreType.TONGUE_ROOT
            dominantFreq <= 500f -> SnoreType.EPIGLOTTIS
            else -> SnoreType.UNKNOWN
        }
    }

    private fun confidenceFor(dominantFreq: Float, avgDb: Float, durationMs: Long): Float {
        if (dominantFreq <= 0f) return 0f
        val loudnessConfidence = ((avgDb + 60f) / 35f).coerceIn(0f, 1f)
        val durationConfidence = (durationMs / 3_000f).coerceIn(0f, 1f)
        val frequencyConfidence = (1f - abs(dominantFreq - 180f) / 420f).coerceIn(0.35f, 1f)
        return sqrt(loudnessConfidence * durationConfidence * frequencyConfidence).coerceIn(0.1f, 0.95f)
    }

    private const val MIN_WINDOW_SAMPLES = 512
    private const val MAX_WINDOW_SAMPLES = 4096
}
