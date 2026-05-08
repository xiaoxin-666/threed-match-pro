package com.example.demo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.demo.data.local.entity.LogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<LogEntryEntity>>

    @Insert
    suspend fun insert(entry: LogEntryEntity)

    @Query("DELETE FROM log_entries")
    suspend fun deleteAll()
}
