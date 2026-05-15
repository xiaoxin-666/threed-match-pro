package com.caluad.match3d.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goods_id") val goodsId: String,
    @ColumnInfo(name = "product_name") val productName: String = "",
    @ColumnInfo(name = "interval_ms") val intervalMs: Long = 2000L,
    @ColumnInfo(name = "turbo_mode") val turboMode: Boolean = false,
    @ColumnInfo(name = "total_count") val totalCount: Int = 1,
    @ColumnInfo(name = "completed_count") val completedCount: Int = 0,
    @ColumnInfo(name = "status") val status: String = TaskStatus.PENDING,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

object TaskStatus {
    const val PENDING = "PENDING"
    const val RUNNING = "RUNNING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val ERROR = "ERROR"
    const val CIRCUIT_BROKEN = "CIRCUIT_BROKEN"
}
