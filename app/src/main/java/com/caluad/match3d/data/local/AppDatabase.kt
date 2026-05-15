package com.caluad.match3d.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.caluad.match3d.data.local.dao.LogEntryDao
import com.caluad.match3d.data.local.dao.TaskDao
import com.caluad.match3d.data.local.entity.LogEntryEntity
import com.caluad.match3d.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, LogEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun logEntryDao(): LogEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "threedmatch.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
