package com.caluad.match3d.data.repository

import com.caluad.match3d.data.local.dao.TaskDao
import com.caluad.match3d.data.local.entity.TaskEntity
import com.caluad.match3d.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()

    suspend fun getById(id: Long): TaskEntity? = taskDao.getById(id)

    suspend fun getByStatus(status: String): List<TaskEntity> = taskDao.getByStatus(status)

    suspend fun createTask(
        goodsId: String,
        productName: String = "",
        intervalMs: Long = 2000L,
        turboMode: Boolean = false,
        totalCount: Int = 1
    ): Long {
        val task = TaskEntity(
            goodsId = goodsId,
            productName = productName,
            intervalMs = intervalMs,
            turboMode = turboMode,
            totalCount = totalCount
        )
        return taskDao.insert(task)
    }

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    suspend fun deleteTask(id: Long) {
        taskDao.getById(id)?.let { taskDao.delete(it) }
    }

    suspend fun deleteAll() = taskDao.deleteAll()

    suspend fun updateProgress(id: Long, completedCount: Int, status: String) {
        taskDao.updateProgress(id, completedCount, status)
    }

    suspend fun resetRunningToPaused() {
        taskDao.resetRunningTasks(TaskStatus.PAUSED)
    }

    suspend fun getRunningTasks(): List<TaskEntity> = taskDao.getByStatus(TaskStatus.RUNNING)

    fun observeTotalLikes(): Flow<Int?> = taskDao.observeTotalLikes()

    fun observeRunningCount(): Flow<Int> = taskDao.observeRunningCount()

    fun observeAllTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()
}
