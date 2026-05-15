package com.caluad.match3d.data.remote

data class ProxyConfig(
    val enabled: Boolean = false,
    val type: String = "HTTP",
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = ""
)
