package com.sleep.snore.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "factor_logs")
data class FactorLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "record_id")
    val recordId: Long?,            // 关联 sleep_record，可为空

    @ColumnInfo(name = "date")
    val date: String,               // yyyy-MM-dd

    @ColumnInfo(name = "alcohol_drinks")
    val alcoholDrinks: Int = 0,

    @ColumnInfo(name = "sleep_position")
    val sleepPosition: String = "unknown", // supine|left_side|right_side|prone

    @ColumnInfo(name = "weight_kg")
    val weightKg: Float? = null,

    @ColumnInfo(name = "nasal_congestion")
    val nasalCongestion: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
