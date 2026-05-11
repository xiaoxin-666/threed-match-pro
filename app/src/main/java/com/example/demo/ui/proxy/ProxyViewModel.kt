package com.example.demo.ui.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.App
import com.example.demo.data.remote.AdoreApi
import com.example.demo.data.remote.ConnectionTracker
import com.example.demo.data.remote.ProxyConfig
import com.example.demo.data.remote.createAdoreApi
import com.example.demo.data.remote.createOkHttpClient
import com.example.demo.engine.REQUEST_K
import com.example.demo.engine.REQUEST_U
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxyViewModel : ViewModel() {
    private val proxyManager = App.instance.proxyManager

    val config: StateFlow<ProxyConfig> = proxyManager.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxyConfig())

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    fun saveConfig(
        enabled: Boolean,
        type: String,
        host: String,
        port: Int,
        username: String,
        password: String
    ) {
        val config = ProxyConfig(enabled, type, host, port, username, password)
        App.instance.recreateAdoreApi(config)
        lookupExitIp(config)
    }

    fun saveEnabled(enabled: Boolean) {
        val current = proxyManager.loadConfig()
        val updated = current.copy(enabled = enabled)
        App.instance.recreateAdoreApi(updated)
        lookupExitIp(updated)
    }

    fun testConnection(
        host: String,
        port: Int,
        type: String,
        username: String,
        password: String
    ) {
        _testing.value = true
        _testResult.value = null
        viewModelScope.launch {
            try {
                val testConfig = ProxyConfig(
                    enabled = true,
                    type = type,
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )
                val api: AdoreApi = createAdoreApi(testConfig)
                val response = withContext(Dispatchers.IO) {
                    api.admireGoods(k = REQUEST_K, u = REQUEST_U, goodsId = "279964")
                }
                _testResult.value = "连接成功! status=${response.status}, info=${response.info}"
                // Also lookup exit IP after successful test
                lookupExitIp(testConfig)
            } catch (e: Exception) {
                _testResult.value = "连接失败: ${e.message}"
            } finally {
                _testing.value = false
            }
        }
    }

    private fun lookupExitIp(config: ProxyConfig) {
        viewModelScope.launch {
            ConnectionTracker.lookupExitIp(createOkHttpClient(config))
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }
}
