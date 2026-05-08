package com.example.demo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "level") val level: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "task_id") val taskId: Long? = null
)

object LogLevel {
    const val INFO = "INFO"
    const val SUCCESS = "SUCCESS"
    const val ERROR = "ERROR"
}
