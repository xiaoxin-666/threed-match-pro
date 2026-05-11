package com.example.demo.data.local

import android.content.Context
import com.example.demo.data.remote.ProxyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProxyManager(context: Context) {
    private val prefs = context.getSharedPreferences("proxy_config", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ProxyConfig> = _config.asStateFlow()

    fun saveConfig(config: ProxyConfig) {
        prefs.edit()
            .putBoolean("enabled", config.enabled)
            .putString("type", config.type)
            .putString("host", config.host)
            .putInt("port", config.port)
            .putString("username", config.username)
            .putString("password", config.password)
            .apply()
        _config.value = config
    }

    fun loadConfig(): ProxyConfig {
        return ProxyConfig(
            enabled = prefs.getBoolean("enabled", false),
            type = prefs.getString("type", "HTTP") ?: "HTTP",
            host = prefs.getString("host", "") ?: "",
            port = prefs.getInt("port", 0),
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: ""
        )
    }
}
