package com.example.demo.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://3dds.3ddl.net/"

fun createRetrofit(proxyConfig: ProxyConfig = ProxyConfig()): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createOkHttpClient(proxyConfig))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

fun createAdoreApi(proxyConfig: ProxyConfig = ProxyConfig()): AdoreApi {
    return createRetrofit(proxyConfig).create(AdoreApi::class.java)
}
