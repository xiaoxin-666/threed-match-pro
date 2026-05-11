package com.example.demo.engine

import com.example.demo.App
import com.example.demo.data.local.entity.LogLevel
import com.example.demo.data.remote.ConnectionTracker
import com.example.demo.data.remote.createOkHttpClient
import com.example.demo.data.local.entity.TaskStatus
import com.example.demo.data.remote.AdoreApi
import com.example.demo.data.remote.AdoreResponse
import com.example.demo.data.repository.LogRepository
import com.example.demo.data.repository.TaskRepository
import com.example.demo.util.JitterUtil
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

const val REQUEST_K =
    "AHICJwJoVSVRAwBuUDUCbgRlBDQ1JMwdiVWRRMVEmU2FXOAk%2BD2lSPlYEB2laP1o2BWYGJ1QkVmIDa1dzU11RaQBjAmkCPQ%3D%3D"
const val REQUEST_U = "279964"

data class EngineState(
    val isRunning: Boolean = false,
    val activeTaskIds: Set<Long> = emptySet(),
    val currentProgress: Map<Long, Int> = emptyMap()
)

data class LogEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val message: String,
    val taskId: Long? = null
)

class ExecutionEngine(
    private val taskRepository: TaskRepository,
    private val logRepository: LogRepository
) {
    private val adoreApi: AdoreApi get() = App.instance.adoreApi
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val circuitBreaker = CircuitBreaker()
    private val progress = ConcurrentHashMap<Long, Int>()
    private val gson = Gson()

    private val _engineState = MutableStateFlow(EngineState())
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _logEvents =
        MutableSharedFlow<LogEvent>(replay = 0, extraBufferCapacity = 256)
    val logEvents: SharedFlow<LogEvent> = _logEvents.asSharedFlow()

    private val _logClearEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 4)
    val logClearEvents: SharedFlow<Unit> = _logClearEvents.asSharedFlow()

    fun startTask(taskId: Long) {
        if (jobs.containsKey(taskId)) return
        val job = scope.launch {
            executeTask(taskId)
        }
        jobs[taskId] = job
    }

    fun stopTask(taskId: Long) {
        scope.launch {
            val job = jobs.remove(taskId)
            if (job != null) {
                job.cancelAndJoin()
                progress.remove(taskId)
                val task = taskRepository.getById(taskId)
                if (task != null && task.status == TaskStatus.RUNNING) {
                    taskRepository.updateProgress(taskId, task.completedCount, TaskStatus.PAUSED)
                }
                updateState()
            }
        }
    }

    fun startAll() {
        scope.launch {
            val tasks = taskRepository.getByStatus(TaskStatus.PENDING) +
                    taskRepository.getByStatus(TaskStatus.PAUSED) +
                    taskRepository.getByStatus(TaskStatus.ERROR) +
                    taskRepository.getByStatus(TaskStatus.CIRCUIT_BROKEN)
            tasks.forEach { task -> startTask(task.id) }
        }
    }

    fun stopAll() {
        scope.launch {
            val allJobs = jobs.entries.toList()
            jobs.clear()
            allJobs.forEach { (_, job) -> job.cancelAndJoin() }
            progress.clear()
            taskRepository.resetRunningToPaused()
            updateState()
        }
    }

    fun getCircuitBreakerState() = circuitBreaker.state

    fun emitInfoLog(message: String) {
        scope.launch {
            emitLog(LogLevel.INFO, message)
        }
    }

    fun notifyLogsCleared() {
        scope.launch {
            _logClearEvents.emit(Unit)
        }
    }

    private suspend fun executeTask(taskId: Long) {
        val task = taskRepository.getById(taskId) ?: return

        try {
            taskRepository.updateProgress(taskId, task.completedCount, TaskStatus.RUNNING)
            progress[taskId] = task.completedCount
            updateState()
            emitLog(LogLevel.INFO, "任务启动: goods_id=${task.goodsId}", taskId)

            for (i in task.completedCount until task.totalCount) {
                circuitBreaker.waitIfOpen()

                try {
                    val response = adoreApi.admireGoods(
                        k = REQUEST_K,
                        u = REQUEST_U,
                        goodsId = task.goodsId
                    )

                    if (response.info.contains("操作频繁")) {
                        circuitBreaker.recordFailure()
                        val body = formatResponseBody(response)
                        emitLog(LogLevel.ERROR, "操作频繁: 服务器拒绝请求\n$body", taskId)
                        break
                    }

                    if (response.status != 1 && response.info.isNotEmpty() && !response.info.contains(
                            "成功",
                            ignoreCase = true
                        )
                    ) {
                        circuitBreaker.recordFailure()
                        val body = formatResponseBody(response)
                        emitLog(LogLevel.ERROR, "服务器错误: status=${response.status}\n$body", taskId)
                        break
                    }

                    circuitBreaker.recordSuccess()
                    val newCount = i + 1
                    taskRepository.updateProgress(taskId, newCount, TaskStatus.RUNNING)
                    progress[taskId] = newCount
                    val body = formatResponseBody(response)
                    // Lookup fresh exit IP for this specific request
                    try {
                        ConnectionTracker.lookupExitIp(
                            createOkHttpClient(App.instance.proxyManager.loadConfig())
                        )
                    } catch (_: Exception) {}
                    val connInfo = ConnectionTracker.getConnectionInfo()
                    emitLog(
                        LogLevel.SUCCESS,
                        "第${newCount}/${task.totalCount}次: goods_id=${task.goodsId} [$connInfo]\n$body",
                        taskId
                    )
                    updateState()
                } catch (e: HttpException) {
                    circuitBreaker.recordFailure()
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    if (e.code() == 404) {
                        emitLog(LogLevel.ERROR, "HTTP 404: 页面不存在\n$errorBody", taskId)
                        break
                    }
                    emitLog(LogLevel.ERROR, "HTTP ${e.code()}: ${e.message}\n$errorBody", taskId)
                } catch (e: java.io.IOException) {
                    circuitBreaker.recordFailure()
                    emitLog(LogLevel.ERROR, "网络错误: ${e.message}", taskId)
                }

                if (!task.turboMode) {
                    val delayMs = JitterUtil.applyJitter(task.intervalMs)
                    delay(delayMs)
                }
            }

            val updated = taskRepository.getById(taskId)
            if (updated != null) {
                val finalStatus = if (updated.completedCount >= updated.totalCount) {
                    emitLog(LogLevel.INFO, "任务完成: goods_id=${updated.goodsId}", taskId)
                    TaskStatus.COMPLETED
                } else if (circuitBreaker.state.value == CircuitState.OPEN) {
                    TaskStatus.CIRCUIT_BROKEN
                } else {
                    TaskStatus.ERROR
                }
                taskRepository.updateProgress(taskId, updated.completedCount, finalStatus)
            }
        } catch (e: CancellationException) {
            val current = taskRepository.getById(taskId)
            if (current != null) {
                taskRepository.updateProgress(taskId, current.completedCount, TaskStatus.PAUSED)
                emitLog(LogLevel.INFO, "任务已暂停: goods_id=${current.goodsId}", taskId)
            }
        } finally {
            jobs.remove(taskId)
            progress.remove(taskId)
            updateState()
        }
    }

    private fun formatResponseBody(response: AdoreResponse): String {
        val dataStr = when (val data = response.data) {
            null -> "null"
            is String -> data
            else -> gson.toJson(data)
        }
        return buildString {
            append("status=${response.status}")
            if (response.info.isNotEmpty()) append(", info=${response.info}")
            append(", data=$dataStr")
        }
    }

    private suspend fun emitLog(level: String, message: String, taskId: Long? = null) {
        val event = LogEvent(level = level, message = message, taskId = taskId)
        _logEvents.emit(event)
        logRepository.addLog(level, message, taskId)
    }

    private fun updateState() {
        _engineState.value = EngineState(
            isRunning = jobs.isNotEmpty(),
            activeTaskIds = jobs.keys.toSet(),
            currentProgress = progress.toMap()
        )
    }
}
