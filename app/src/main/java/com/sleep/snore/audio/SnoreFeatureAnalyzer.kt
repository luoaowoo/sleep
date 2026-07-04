package com.sleep.snore.audio

import com.sleep.snore.data.model.SnoreType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
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
        val samples = readAnalysisWindow(pcmData)
        val dominantFreq = estimateDominantFrequency(samples)
        val periodicityScore = calculatePeriodicity(pcmData)
        val snoreType = classifyType(dominantFreq, avgDb, samples)
        val confidence = confidenceFor(dominantFreq, avgDb, durationMs, periodicityScore)
        var label = "${snoreType.label} · 置信度 ${(confidence * 100).roundToInt()}%"
        if (periodicityScore < 0.3f && !label.contains("低节律性")) {
            label = "$label · 低节律性"
        }
        return SnoreAudioFeatures(
            avgDb = avgDb,
            peakDb = peakDb.toFloat(),
            dominantFreq = dominantFreq,
            snoreType = snoreType,
            confidence = confidence,
            aiTypeLabel = label
        )
    }

    fun calculatePeriodicity(pcmData: ByteArray): Float {
        val totalSamples = pcmData.size / 2
        val frameSize = AudioConfig.FRAME_SIZE
        val frameCount = totalSamples / frameSize
        if (frameCount < 10) return 0f

        val energy = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            var sumSquares = 0.0
            val start = frame * frameSize
            for (i in 0 until frameSize) {
                val offset = (start + i) * 2
                val sample = ((pcmData[offset + 1].toInt() shl 8) or (pcmData[offset].toInt() and 0xFF))
                    .toShort()
                    .toDouble()
                sumSquares += sample * sample
            }
            energy[frame] = sqrt(sumSquares / frameSize).toFloat()
        }

        val minLag = 10
        val maxLag = minOf(40, frameCount - 1)
        if (maxLag < minLag) return 0f

        val r = FloatArray(maxLag + 1)
        for (k in 0..maxLag) {
            var sum = 0.0
            for (i in 0 until frameCount - k) {
                sum += energy[i] * energy[i + k]
            }
            r[k] = sum.toFloat()
        }

        if (r[0] <= 0f) return 0f

        var peakValue = r[minLag]
        for (k in minLag..maxLag) {
            if (r[k] > peakValue) {
                peakValue = r[k]
            }
        }

        val baseline = r[minLag - 1]
        val periodicity = (peakValue - baseline) / r[0]
        return periodicity.coerceIn(0f, 1f)
    }

    fun calculateSpectralEntropy(samples: DoubleArray): Float {
        val targetN = 512
        if (samples.size < 2) return 0f
        val n = minOf(samples.size, targetN)
        val start = if (samples.size > targetN) (samples.size - targetN) / 2 else 0
        val input = DoubleArray(n)
        for (i in 0 until n) {
            val w = 0.5 - 0.5 * cos(2.0 * PI * i / (n - 1).coerceAtLeast(1))
            input[i] = samples[start + i] * w
        }
        val mag = dft(input)
        val half = mag.size
        var sum = 0.0
        for (v in mag) sum += v
        if (sum <= 0.0) return 0f
        var entropy = 0.0
        for (v in mag) {
            val p = v / sum
            if (p > 0.0) {
                entropy -= p * log2(p)
            }
        }
        val maxEntropy = log2(half.toDouble())
        if (maxEntropy <= 0.0) return 0f
        return (entropy / maxEntropy).toFloat().coerceIn(0f, 1f)
    }

    private fun dft(samples: DoubleArray): DoubleArray {
        val n = samples.size
        val half = n / 2
        val mag = DoubleArray(half)
        for (k in 0 until half) {
            var real = 0.0
            var imag = 0.0
            for (t in 0 until n) {
                val angle = -2.0 * PI * k * t / n
                real += samples[t] * cos(angle)
                imag += samples[t] * sin(angle)
            }
            mag[k] = sqrt(real * real + imag * imag)
        }
        return mag
    }

    private fun estimateDominantFrequency(samples: DoubleArray): Float {
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

    private fun classifyType(dominantFreq: Float, avgDb: Float, samples: DoubleArray): SnoreType {
        if (dominantFreq <= 0f || avgDb < -65f) return SnoreType.UNKNOWN
        val entropy = calculateSpectralEntropy(samples)
        return when {
            entropy > 0.7f -> SnoreType.MIXED
            dominantFreq < 130f -> SnoreType.SOFT_PALATE
            dominantFreq < 260f -> SnoreType.TONGUE_ROOT
            dominantFreq <= 500f -> SnoreType.EPIGLOTTIS
            else -> SnoreType.UNKNOWN
        }
    }

    private fun confidenceFor(dominantFreq: Float, avgDb: Float, durationMs: Long, periodicityScore: Float): Float {
        if (dominantFreq <= 0f) return 0f
        val loudnessConfidence = ((avgDb + 60f) / 35f).coerceIn(0f, 1f)
        val durationConfidence = (durationMs / 3_000f).coerceIn(0f, 1f)
        val frequencyConfidence = (1f - abs(dominantFreq - 180f) / 420f).coerceIn(0.35f, 1f)
        return sqrt(loudnessConfidence * durationConfidence * frequencyConfidence * periodicityScore).coerceIn(0.1f, 0.95f)
    }

    private const val MIN_WINDOW_SAMPLES = 512
    private const val MAX_WINDOW_SAMPLES = 4096
}
