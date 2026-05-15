package com.caluad.match3d.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caluad.match3d.data.local.entity.TaskEntity
import com.caluad.match3d.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = :status")
    suspend fun getByStatus(status: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("UPDATE tasks SET status = :status, updated_at = :now WHERE status = 'RUNNING'")
    suspend fun resetRunningTasks(status: String = TaskStatus.PAUSED, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET completed_count = :count, status = :status, updated_at = :now WHERE id = :id")
    suspend fun updateProgress(id: Long, count: Int, status: String, now: Long = System.currentTimeMillis())

    @Query("SELECT SUM(completed_count) FROM tasks")
    fun observeTotalLikes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'RUNNING'")
    fun observeRunningCount(): Flow<Int>
}
