package com.sleep.snore.domain

import android.content.Context
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

internal fun csv(value: Any?): String {
    val text = value?.toString().orEmpty()
    val sanitized = if (text.firstOrNull() in CSV_FORMULA_PREFIXES) "'$text" else text
    return "\"${sanitized.replace("\"", "\"\"")}\""
}

private val CSV_FORMULA_PREFIXES = setOf('=', '+', '-', '@')

@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SleepRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 导出所有睡眠记录汇总为 CSV
     * @return 导出文件，失败返回 null
     */
    suspend fun exportRecordsCsv(): File? = withContext(Dispatchers.IO) {
        try {
            val records = repository.getAllRecords().first()
            if (records.isEmpty()) return@withContext null

            val file = File(context.cacheDir, "sleep_records_${System.currentTimeMillis()}.csv")
            file.utf8Writer().use { writer ->
                // BOM for Excel
                writer.write("\uFEFF")
                // Header
                writer.write("日期,开始时间,结束时间,睡眠时长(分),SnoreScore,严重程度,AHI估算,打鼾时长(分),打鼾占比,平均dB,峰值dB,鼾声次数,最长呼吸暂停(秒)\n")

                records.forEach { r ->
                    writer.writeCsvRow(
                        formatDate(r.startTime),
                        formatDate(r.startTime),
                        formatDate(r.endTime),
                        r.sleepDurationMin,
                        r.snoreScore,
                        r.severity,
                        r.estAHI,
                        r.snoreDurationMin,
                        r.snoreRatio,
                        r.avgDb,
                        r.maxDb,
                        r.snoreEventCount,
                        r.longestApneaSec
                    )
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 导出全部鼾声事件详情 CSV。
     */
    suspend fun exportAllEventsCsv(): File? = withContext(Dispatchers.IO) {
        try {
            val events = repository.getAllEvents().first()
            if (events.isEmpty()) return@withContext null

            val file = File(context.cacheDir, "snore_events_${System.currentTimeMillis()}.csv")
            file.utf8Writer().use { writer ->
                writer.write("\uFEFF")
                writer.write("记录ID,时间,持续(ms),峰值dB,平均dB,主导频率Hz,类型,AI标签,音频路径,文件大小(bytes)\n")
                events.forEach { e ->
                    writer.writeCsvRow(
                        e.recordId,
                        formatDate(e.startTimestamp),
                        e.durationMs,
                        e.peakDb,
                        e.avgDb,
                        e.dominantFreq,
                        e.snoreType,
                        e.aiTypeLabel,
                        e.audioFilePath,
                        e.audioFileSizeBytes
                    )
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    private fun OutputStreamWriter.writeCsvRow(vararg values: Any?) {
        write(values.joinToString(",") { csv(it) })
        write("\n")
    }

    private fun csv(value: Any?): String {
        val text = value?.toString().orEmpty()
        val sanitized = if (text.firstOrNull() in CSV_FORMULA_PREFIXES) "'$text" else text
        return "\"${sanitized.replace("\"", "\"\"")}\""
    }

    private fun File.utf8Writer(): OutputStreamWriter {
        return OutputStreamWriter(outputStream(), StandardCharsets.UTF_8)
    }

    private companion object {
        val CSV_FORMULA_PREFIXES = setOf('=', '+', '-', '@')
    }
}
