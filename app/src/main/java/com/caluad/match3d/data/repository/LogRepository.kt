package com.caluad.match3d.data.repository

import com.caluad.match3d.data.local.dao.LogEntryDao
import com.caluad.match3d.data.local.entity.LogEntryEntity
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logEntryDao: LogEntryDao) {

    fun observeRecent(): Flow<List<LogEntryEntity>> = logEntryDao.observeRecent()

    suspend fun addLog(level: String, message: String, taskId: Long? = null) {
        logEntryDao.insert(
            LogEntryEntity(
                level = level,
                message = message,
                taskId = taskId
            )
        )
    }

    suspend fun clearAll() = logEntryDao.deleteAll()
}
