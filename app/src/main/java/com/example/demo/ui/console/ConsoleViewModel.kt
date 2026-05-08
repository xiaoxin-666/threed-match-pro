package com.example.demo.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.App
import com.example.demo.engine.LogEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsoleViewModel : ViewModel() {
    private val logRepo = App.instance.logRepository
    private val engine = App.instance.executionEngine

    private val _logs = MutableStateFlow<List<LogEvent>>(emptyList())
    val logs: StateFlow<List<LogEvent>> = _logs.asStateFlow()

    private val _autoScroll = MutableStateFlow(true)
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    init {
        viewModelScope.launch {
            logRepo.observeRecent().collect { entities ->
                if (_logs.value.isEmpty() && entities.isNotEmpty()) {
                    _logs.value = entities.reversed().map {
                        LogEvent(it.timestamp, it.level, it.message, it.taskId)
                    }
                }
            }
        }

        viewModelScope.launch {
            engine.logEvents.collect { event ->
                val list = _logs.value.toMutableList()
                list.add(0, event)
                _logs.value = if (list.size > 500) list.take(500) else list
            }
        }

        viewModelScope.launch {
            engine.logClearEvents.collect {
                _logs.value = emptyList()
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepo.clearAll()
            _logs.value = emptyList()
            engine.notifyLogsCleared()
        }
    }

    fun toggleAutoScroll() {
        _autoScroll.value = !_autoScroll.value
    }
}
