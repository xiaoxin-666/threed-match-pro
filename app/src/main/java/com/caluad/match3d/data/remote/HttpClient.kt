package com.caluad.match3d.data.remote

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.Authenticator
import java.net.Authenticator.RequestorType
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.concurrent.TimeUnit

fun createOkHttpClient(proxyConfig: ProxyConfig = ProxyConfig()): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val hasProxy = proxyConfig.enabled && proxyConfig.host.isNotBlank() && proxyConfig.port > 0
    val proxyType = if (proxyConfig.type.uppercase() == "SOCKS5") Proxy.Type.SOCKS else Proxy.Type.HTTP
    val hasAuth = proxyConfig.username.isNotBlank()

    // Preemptive auth for HTTP proxy only — SOCKS5 authenticator callbacks
    // run on native socket threads where uncaught exceptions crash the app.
    if (hasProxy && hasAuth && proxyType == Proxy.Type.HTTP) {
        val user = proxyConfig.username
        val pass = proxyConfig.password.toCharArray()
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                try {
                    if (requestorType == RequestorType.PROXY) {
                        return PasswordAuthentication(user, pass)
                    }
                } catch (_: Exception) { }
                return super.getPasswordAuthentication()
            }
        })
    }

    return OkHttpClient.Builder()
        .apply {
            if (hasProxy) {
                proxy(Proxy(proxyType, InetSocketAddress(proxyConfig.host, proxyConfig.port)))

                if (hasAuth && proxyType == Proxy.Type.HTTP) {
                    val credential = Credentials.basic(proxyConfig.username, proxyConfig.password)
                    proxyAuthenticator { _, response ->
                        if (response.request.header("Proxy-Authorization") != null) {
                            return@proxyAuthenticator null
                        }
                        response.request.newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build()
                    }
                }
            }
        }
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", UserAgentPool.random())
                .build()
            val response = chain.proceed(request)
            ConnectionTracker.setProxyInfo(
                if (hasProxy) {
                    "代理 ${proxyConfig.type}://${proxyConfig.host}:${proxyConfig.port}"
                } else {
                    "直连"
                }
            )
            response
        }
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
