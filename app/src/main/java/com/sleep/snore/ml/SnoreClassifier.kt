package com.sleep.snore.ml

import android.content.Context
import android.util.Log
import com.sleep.snore.data.model.SnoreType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnoreClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var classifier: AudioClassifier? = null
    private var isLoaded = false

    fun load(): Boolean {
        if (isLoaded) return true
        return try {
            val modelFile = copyModelToCache("yamnet_snore.tflite")
            classifier = AudioClassifier.createFromFile(context, modelFile.absolutePath)
            isLoaded = true
            Log.i(TAG, "TFLite 模型加载成功")
            true
        } catch (e: Exception) {
            Log.w(TAG, "TFLite 模型未找到: ${e.message}. 将使用规则引擎回退。")
            false
        }
    }

    fun classify(pcmData: ByteArray): ClassificationResult {
        if (!isLoaded || classifier == null) {
            return ClassificationResult(SnoreType.UNKNOWN, 0f, emptyList())
        }

        return try {
            val tensorAudio = TensorAudio.create()
            tensorAudio.load(pcmData)
            val results: List<Classifications> = classifier!!.classify(tensorAudio)
            parseResults(results)
        } catch (e: Exception) {
            Log.e(TAG, "分类失败: ${e.message}")
            ClassificationResult(SnoreType.UNKNOWN, 0f, emptyList())
        }
    }

    private fun parseResults(classifications: List<Classifications>): ClassificationResult {
        if (classifications.isEmpty()) return ClassificationResult(SnoreType.UNKNOWN, 0f, emptyList())
        val categories = classifications[0].categories.sortedByDescending { it.score }
        val top = categories.firstOrNull()
        val snoreType = mapLabelToSnoreType(top?.label ?: "")
        val labelList = categories.map { LabelScore(it.label, it.score) }
        return ClassificationResult(snoreType, top?.score ?: 0f, labelList)
    }

    private fun mapLabelToSnoreType(label: String): SnoreType = when {
        label.contains("soft", ignoreCase = true) || label.contains("palate", ignoreCase = true) -> SnoreType.SOFT_PALATE
        label.contains("tongue", ignoreCase = true) || label.contains("root", ignoreCase = true) -> SnoreType.TONGUE_ROOT
        label.contains("epiglot", ignoreCase = true) -> SnoreType.EPIGLOTTIS
        label.contains("mixed", ignoreCase = true) -> SnoreType.MIXED
        else -> SnoreType.UNKNOWN
    }

    private fun copyModelToCache(modelName: String): File {
        val cacheFile = File(context.cacheDir, modelName)
        if (!cacheFile.exists()) {
            try {
                context.assets.open(modelName).use { input ->
                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) { }
        }
        return cacheFile
    }

    fun close() {
        classifier?.close()
        classifier = null
        isLoaded = false
    }

    companion object {
        private const val TAG = "SnoreClassifier"
    }
}

data class ClassificationResult(
    val snoreType: SnoreType,
    val confidence: Float,
    val allLabels: List<LabelScore>
)

data class LabelScore(
    val label: String,
    val score: Float
)
