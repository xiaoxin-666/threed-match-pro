package com.caluad.match3d.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.atomic.AtomicReference

object ConnectionTracker {
    private val proxyInfo = AtomicReference("直连")
    private val exitIp = AtomicReference<String?>(null)

    fun setProxyInfo(info: String) {
        proxyInfo.set(info)
    }

    fun setExitIp(ip: String?) {
        exitIp.set(ip)
    }

    fun getConnectionInfo(): String {
        val info = proxyInfo.get()
        val ip = exitIp.get()
        return if (ip != null) {
            "$info, 出口IP: $ip"
        } else {
            info
        }
    }

    suspend fun lookupExitIp(client: OkHttpClient) {
        try {
            val request = Request.Builder().url("https://api.ipify.org").build()
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val ip = response.body?.string()?.trim() ?: ""
            response.close()
            if (ip.isNotBlank()) setExitIp(ip)
        } catch (_: Exception) { }
    }
}
