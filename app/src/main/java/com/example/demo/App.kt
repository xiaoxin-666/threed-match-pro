package com.example.demo

import android.app.Application
import com.example.demo.data.local.AppDatabase
import com.example.demo.data.local.ProxyManager
import com.example.demo.data.remote.AdoreApi
import com.example.demo.data.remote.ConnectionTracker
import com.example.demo.data.remote.ProxyConfig
import com.example.demo.data.remote.createAdoreApi
import com.example.demo.data.remote.createOkHttpClient
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
    val proxyManager: ProxyManager by lazy { ProxyManager(this) }

    @Volatile
    lateinit var adoreApi: AdoreApi

    val executionEngine: ExecutionEngine by lazy {
        ExecutionEngine(taskRepository, logRepository)
    }

    fun recreateAdoreApi(config: ProxyConfig) {
        adoreApi = createAdoreApi(config)
        proxyManager.saveConfig(config)
        if (config.enabled) {
            executionEngine.emitInfoLog("代理配置已更新: ${config.type}://${config.host}:${config.port}")
        } else {
            executionEngine.emitInfoLog("代理已关闭，使用直连")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        adoreApi = createAdoreApi(proxyManager.loadConfig())
        CoroutineScope(Dispatchers.IO).launch {
            taskRepository.resetRunningToPaused()
            ConnectionTracker.lookupExitIp(createOkHttpClient(proxyManager.loadConfig()))
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
