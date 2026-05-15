package com.caluad.match3d.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caluad.match3d.App
import com.caluad.match3d.data.local.entity.TaskEntity
import com.caluad.match3d.data.local.entity.TaskStatus
import com.caluad.match3d.engine.EngineState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {
    private val taskRepo = App.instance.taskRepository
    private val engine = App.instance.executionEngine

    val tasks: StateFlow<List<TaskEntity>> = taskRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineState: StateFlow<EngineState> = engine.engineState

    fun startTask(id: Long) {
        engine.startTask(id)
    }

    fun stopTask(id: Long) {
        engine.stopTask(id)
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            engine.stopTask(id)
            taskRepo.deleteTask(id)
        }
    }

    fun toggleTask(id: Long) {
        viewModelScope.launch {
            val task = taskRepo.getById(id) ?: return@launch
            when (task.status) {
                TaskStatus.RUNNING -> engine.stopTask(id)
                TaskStatus.PENDING, TaskStatus.PAUSED, TaskStatus.ERROR, TaskStatus.CIRCUIT_BROKEN ->
                    engine.startTask(id)
                TaskStatus.COMPLETED -> { /* cannot restart completed, user should edit first */ }
            }
        }
    }

    fun createTask(
        goodsId: String,
        productName: String,
        intervalMs: Long,
        turboMode: Boolean,
        totalCount: Int
    ) {
        viewModelScope.launch {
            taskRepo.createTask(goodsId, productName, intervalMs, turboMode, totalCount)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepo.updateTask(task)
        }
    }
}
