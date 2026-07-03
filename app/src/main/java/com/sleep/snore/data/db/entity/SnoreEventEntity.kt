package com.sleep.snore.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snore_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("record_id")]
)
data class SnoreEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "record_id")
    val recordId: Long,

    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long,       // epoch ms

    @ColumnInfo(name = "duration_ms")
    val durationMs: Int,            // 鼾声持续毫秒

    @ColumnInfo(name = "peak_db")
    val peakDb: Float,

    @ColumnInfo(name = "avg_db")
    val avgDb: Float,

    @ColumnInfo(name = "dominant_freq")
    val dominantFreq: Float,        // 主导频率 Hz

    @ColumnInfo(name = "snore_type")
    val snoreType: String,          // SnoreType.name

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String,      // .ogg 文件绝对路径

    @ColumnInfo(name = "audio_file_size_bytes")
    val audioFileSizeBytes: Long,

    @ColumnInfo(name = "ai_type_label")
    val aiTypeLabel: String,        // AI简短标签

    @ColumnInfo(name = "is_favorited")
    val isFavorited: Boolean = false
)
