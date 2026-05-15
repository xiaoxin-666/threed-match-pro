package com.caluad.match3d.data.remote

data class ProbeResponse(
    val status: Int = 0,
    val info: String = "",
    val msg: String = "",
    val data: Any? = null
)
