package com.example.demo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.App
import com.example.demo.data.local.entity.TaskEntity
import com.example.demo.data.remote.GoodsInfo
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
    private val goodsInfoApi = App.instance.goodsInfoApi

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

    // Query state
    val queryGoodsId = MutableStateFlow("")
    private val _queryResult = MutableStateFlow<GoodsInfo?>(null)
    val queryResult: StateFlow<GoodsInfo?> = _queryResult
    private val _queryLoading = MutableStateFlow(false)
    val queryLoading: StateFlow<Boolean> = _queryLoading
    private val _queryError = MutableStateFlow<String?>(null)
    val queryError: StateFlow<String?> = _queryError

    fun updateQueryGoodsId(id: String) {
        queryGoodsId.value = id
        if (_queryError.value != null) _queryError.value = null
    }

    fun queryGoods() {
        val id = queryGoodsId.value.trim()
        if (id.isEmpty()) {
            _queryError.value = "请输入商品ID"
            return
        }
        viewModelScope.launch {
            _queryLoading.value = true
            _queryError.value = null
            _queryResult.value = null
            val result = goodsInfoApi.fetchGoodsInfo(id)
            result.onSuccess { info ->
                _queryResult.value = info
            }.onFailure { e ->
                _queryError.value = e.message ?: "查询失败，请检查网络或ID"
            }
            _queryLoading.value = false
        }
    }

    fun clearQueryResult() {
        _queryResult.value = null
    }

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
