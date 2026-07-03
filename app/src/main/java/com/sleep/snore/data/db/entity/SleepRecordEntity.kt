package com.sleep.snore.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_records",
    indices = [
        Index("start_time"),
        Index("created_at")
    ]
)
data class SleepRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "start_time")
    val startTime: Long,            // epoch ms

    @ColumnInfo(name = "end_time")
    val endTime: Long,

    @ColumnInfo(name = "sleep_duration_min")
    val sleepDurationMin: Int,      // 实际睡眠时长(分)

    @ColumnInfo(name = "snore_score")
    val snoreScore: Int,            // 0-100

    @ColumnInfo(name = "severity")
    val severity: String,           // Severity.name

    @ColumnInfo(name = "est_ahi")
    val estAHI: Float,              // 估算AHI

    @ColumnInfo(name = "snore_duration_min")
    val snoreDurationMin: Int,      // 累积打鼾时长(分)

    @ColumnInfo(name = "snore_ratio")
    val snoreRatio: Float,          // 打鼾占比 0.0-1.0

    @ColumnInfo(name = "avg_db")
    val avgDb: Float,

    @ColumnInfo(name = "max_db")
    val maxDb: Float,

    @ColumnInfo(name = "snore_event_count")
    val snoreEventCount: Int,

    @ColumnInfo(name = "apnea_event_count")
    val apneaEventCount: Int,       // 沉默≥10秒事件

    @ColumnInfo(name = "longest_apnea_sec")
    val longestApneaSec: Int,

    @ColumnInfo(name = "snore_type_distribution")
    val snoreTypeDistribution: String,  // JSON: {"SOFT_PALATE":0.6,...}

    @ColumnInfo(name = "hourly_distribution")
    val hourlyDistribution: String,     // JSON: [5,12,8,...]

    @ColumnInfo(name = "ai_summary")
    val aiSummary: String,              // AI一句话总结

    @ColumnInfo(name = "ai_evaluation")
    val aiEvaluation: String,           // AI完整评价

    @ColumnInfo(name = "ai_suggestions")
    val aiSuggestions: String,          // JSON数组

    @ColumnInfo(name = "is_favorited")
    val isFavorited: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
