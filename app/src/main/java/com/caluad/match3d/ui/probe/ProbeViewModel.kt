package com.caluad.match3d.ui.probe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caluad.match3d.App
import com.caluad.match3d.data.local.entity.LogLevel
import com.caluad.match3d.data.remote.ProbeApi
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

enum class ProbeMode(val label: String, val actionLabel: String) {
    ADMIRE("点赞探测", "点赞"),
    CANCEL_ADMIRE("取消点赞", "取消点赞")
}

data class ProbeResult(
    val index: Int,
    val success: Boolean,
    val msg: String,
    val info: String,
    val elapsedMs: Long,
    val body: String = ""
)

data class ProbeState(
    val mode: ProbeMode = ProbeMode.ADMIRE,
    val goodsId: String = "",
    val concurrency: Int = 3,
    val isRunning: Boolean = false,
    val sentCount: Int = 0,
    val successCount: Int = 0,
    val totalCount: Int = 0,
    val results: List<ProbeResult> = emptyList(),
    val completed: Boolean = false
)

private const val SEQUENTIAL_DELAY_MS = 1000L

class ProbeViewModel : ViewModel() {
    private val probeApi = ProbeApi.create()
    private val gson = Gson()

    private val _state = MutableStateFlow(ProbeState())
    val state: StateFlow<ProbeState> = _state

    private val sentCounter = AtomicInteger(0)
    private val successCounter = AtomicInteger(0)

    fun toggleMode() {
        val next = if (_state.value.mode == ProbeMode.ADMIRE) ProbeMode.CANCEL_ADMIRE else ProbeMode.ADMIRE
        _state.value = _state.value.copy(mode = next)
    }

    fun updateGoodsId(id: String) {
        _state.value = _state.value.copy(goodsId = id)
    }

    fun updateConcurrency(count: Int) {
        _state.value = _state.value.copy(concurrency = count.coerceIn(1, 5))
    }

    fun setInitialGoodsId(id: String) {
        if (_state.value.goodsId.isEmpty() && id.isNotEmpty()) {
            _state.value = _state.value.copy(goodsId = id)
        }
    }

    fun executeProbe() {
        val goodsId = _state.value.goodsId.trim()
        if (goodsId.isEmpty() || _state.value.isRunning) return

        val concurrency = _state.value.concurrency
        val mode = _state.value.mode
        sentCounter.set(0)
        successCounter.set(0)

        _state.value = _state.value.copy(
            isRunning = true,
            sentCount = 0,
            successCount = 0,
            totalCount = concurrency,
            results = emptyList(),
            completed = false
        )

        viewModelScope.launch {
            val engine = App.instance.executionEngine
            val isConcurrent = mode == ProbeMode.ADMIRE
            engine.emitLog(LogLevel.INFO, "[${mode.label}] 开始${mode.actionLabel} goods_id=$goodsId, 次数=$concurrency")

            val results = if (isConcurrent) {
                List(concurrency) { index ->
                    async(Dispatchers.IO) {
                        executeOneRequest(mode, goodsId, index)
                    }
                }.awaitAll()
            } else {
                List(concurrency) { index ->
                    if (index > 0) delay(SEQUENTIAL_DELAY_MS)
                    executeOneRequest(mode, goodsId, index)
                }
            }

            val sc = successCounter.get()
            results.forEach { r ->
                val level = if (r.success) LogLevel.SUCCESS else LogLevel.ERROR
                engine.emitLog(level, r.body)
            }
            engine.emitLog(LogLevel.INFO, "[${mode.label}] ${mode.actionLabel}结束: 成功 $sc/$concurrency")

            _state.value = _state.value.copy(
                isRunning = false,
                completed = true,
                results = results
            )
        }
    }

    private suspend fun executeOneRequest(mode: ProbeMode, goodsId: String, index: Int): ProbeResult {
        val startTime = System.currentTimeMillis()
        val order = sentCounter.incrementAndGet()
        _state.value = _state.value.copy(sentCount = order)

        return try {
            val response = when (mode) {
                ProbeMode.ADMIRE -> probeApi.probeAdmire(goodsId)
                ProbeMode.CANCEL_ADMIRE -> probeApi.cancelAdmire(goodsId)
            }
            val elapsed = System.currentTimeMillis() - startTime
            val body = formatProbeBody(response)
            val isSuccess = response.msg.equals("success", ignoreCase = true) ||
                    response.info.contains("成功", ignoreCase = true)
            if (isSuccess) {
                val sc = successCounter.incrementAndGet()
                _state.value = _state.value.copy(successCount = sc)
            }
            ProbeResult(
                index = index + 1,
                success = isSuccess,
                msg = response.msg.ifEmpty { response.info },
                info = response.info,
                elapsedMs = elapsed,
                body = body
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            ProbeResult(
                index = index + 1,
                success = false,
                msg = "请求失败",
                info = e.message ?: "未知错误",
                elapsedMs = elapsed
            )
        }
    }

    private fun formatProbeBody(response: com.caluad.match3d.data.remote.ProbeResponse): String {
        val dataStr = when (val data = response.data) {
            null -> "null"
            is String -> data
            else -> gson.toJson(data)
        }
        return buildString {
            append("status=${response.status}")
            if (response.info.isNotEmpty()) append(", info=${response.info}")
            if (response.msg.isNotEmpty()) append(", msg=${response.msg}")
            append(", data=$dataStr")
        }
    }
}
