package com.example.demo.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://3dds.3ddl.net/"

fun createRetrofit(): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

fun createAdoreApi(): AdoreApi {
    return createRetrofit().create(AdoreApi::class.java)
}
