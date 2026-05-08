package com.example.demo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.App
import com.example.demo.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val taskRepo = App.instance.taskRepository
    private val engine = App.instance.executionEngine
    private val logRepo = App.instance.logRepository

    val allTasks: StateFlow<List<TaskEntity>> = taskRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLikes: StateFlow<Int> = taskRepo.observeTotalLikes()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val runningTaskCount: StateFlow<Int> = taskRepo.observeRunningCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overallProgress: StateFlow<Float> = allTasks.map { tasks ->
        val total = tasks.sumOf { it.totalCount }
        val completed = tasks.sumOf { it.completedCount }
        if (total > 0) completed.toFloat() / total.toFloat() else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val engineState = engine.engineState

    fun startAllTasks() {
        engine.startAll()
    }

    fun stopAllTasks() {
        engine.stopAll()
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepo.clearAll()
            engine.notifyLogsCleared()
        }
    }
}
