package com.example.demo

import android.app.Application
import com.example.demo.data.local.AppDatabase
import com.example.demo.data.remote.AdoreApi
import com.example.demo.data.remote.createAdoreApi
import com.example.demo.data.repository.LogRepository
import com.example.demo.data.repository.TaskRepository
import com.example.demo.engine.ExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val logRepository: LogRepository by lazy { LogRepository(database.logEntryDao()) }
    val adoreApi: AdoreApi by lazy { createAdoreApi() }
    val executionEngine: ExecutionEngine by lazy {
        ExecutionEngine(taskRepository, logRepository, adoreApi)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CoroutineScope(Dispatchers.IO).launch {
            taskRepository.resetRunningToPaused()
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
