package com.example.demo.data.remote

data class ProbeResponse(
    val status: Int = 0,
    val info: String = "",
    val msg: String = "",
    val data: Any? = null
)
