package com.example.demo.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

private const val PROBE_BASE_URL = "https://ds.3ddl.net"

private const val PROBE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"

// Fixed k and u form values matching the captured mobile request (maomi/2)
private const val PROBE_K =
    "VnRXK1ZhVHABClU5VjBcMFFmU2ZaMAJqVWcHbVR2VDlWaQ9hBD8PN1NYUT0GYFM%2FBTACJgggUWgHbVR2VwwBZlZlV2VWNA%3D%3D"
private const val PROBE_U = "271978"

class ProbeApi(private val client: OkHttpClient) {

    private val gson = Gson()

    suspend fun probeAdmire(goodsId: String): ProbeResponse = executeRequest(
        met = "admireGoods",
        referer = "$PROBE_BASE_URL//tmpl/product_detail.html?goods_id=$goodsId&rec=",
        goodsId = goodsId
    )

    suspend fun cancelAdmire(goodsId: String): ProbeResponse = executeRequest(
        met = "canleAdmireGoods",
        referer = "https://mds.3ddl.net/tmpl/member/admires.html",
        goodsId = goodsId
    )

    private suspend fun executeRequest(met: String, referer: String, goodsId: String): ProbeResponse =
        withContext(Dispatchers.IO) {
            val url = "$PROBE_BASE_URL//index.php?ctl=Goods_Goods&met=$met&typ=json"
            val bodyString = "k=$PROBE_K&u=$PROBE_U&goods_id=$goodsId"
            val body = bodyString.toRequestBody(null)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .header("content-length", bodyString.toByteArray(Charsets.UTF_8).size.toString())
                .header("accept", "application/json")
                .header("origin", "https://mds.3ddl.net")
                .header("user-agent", PROBE_USER_AGENT)
                .header("content-type", "application/x-www-form-urlencoded")
                .header("referer", referer)
                .header("accept-encoding", "gzip, deflate")
                .header("accept-language", "zh-CN,en-US;q=0.9")
                .header("x-requested-with", "mark.via")

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            response.close()

            try {
                gson.fromJson(responseBody, ProbeResponse::class.java)
            } catch (e: Exception) {
                ProbeResponse(status = -1, info = "解析失败", msg = responseBody)
            }
        }

    companion object {
        fun create(): ProbeApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            return ProbeApi(client)
        }
    }
}
