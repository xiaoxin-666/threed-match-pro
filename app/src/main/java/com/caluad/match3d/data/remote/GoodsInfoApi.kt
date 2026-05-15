package com.caluad.match3d.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GoodsInfoApi(private val httpClient: OkHttpClient) {

    private val goodsBaseUrl = "https://3dds.3ddl.net"

    suspend fun fetchGoodsInfo(goodsId: String): Result<GoodsInfo> = withContext(Dispatchers.IO) {
        try {
            // Fetch JSON from the goods endpoint
            val jsonUrl = "$goodsBaseUrl/index.php?ctl=Goods_Goods&met=goods&type=goods&gid=$goodsId&typ=json"
            val request = Request.Builder()
                .url(jsonUrl)
                .header("User-Agent", UserAgentPool.random())
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("响应为空"))

            val root = JsonParser.parseString(body).asJsonObject
            val setObj = findSetObject(root)
            if (setObj == null) {
                return@withContext Result.failure(Exception("未找到推荐数据 (set)"))
            }

            val setMap = setObj.asJsonObject.entrySet().associate {
                it.key to it.value.asString
            }

            val productName = extractProductName(root, goodsId)
            val thumbnailUrl = findThumbnail(root)

            Result.success(
                GoodsInfo(
                    goodsId = goodsId,
                    productName = productName,
                    totalRecommend = pick(setMap, "all_admire", "all", "total", "zonghe"),
                    memberRecommend = pick(setMap, "user_admire", "user", "member", "putong"),
                    expertRecommend = pick(setMap, "expert_admire", "expert", "zhuanjia"),
                    certRecommend = pick(setMap, "cert_admire", "certified_admire", "cert", "renzheng", "vip1"),
                    thumbnailUrl = thumbnailUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Recursively search the JSON tree for a key named "set" */
    private fun findSetObject(element: JsonElement): JsonElement? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("set")) return obj.get("set")
            for ((_, value) in obj.entrySet()) {
                val found = findSetObject(value)
                if (found != null) return found
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                val found = findSetObject(item)
                if (found != null) return found
            }
        }
        return null
    }

    /** Pick the first value whose key contains one of the candidates */
    private fun pick(map: Map<String, String>, vararg candidates: String): String {
        for (c in candidates) {
            for ((k, v) in map) {
                if (k.contains(c, ignoreCase = true) || c.contains(k, ignoreCase = true)) {
                    return v
                }
            }
        }
        // Fallback: if there are exactly 4 entries in the map, use positional matching
        val entries = map.entries.toList()
        if (entries.size >= 4) {
            val idx = when (candidates.first()) {
                "总推荐指数", "综合推荐", "总推荐" -> 0
                "普通会员推荐指数", "普通会员", "普通推荐" -> 1
                "专家推荐指数", "专家推荐", "专家" -> 2
                "数字认证推荐指数", "数字认证", "认证推荐" -> 3
                else -> -1
            }
            if (idx in entries.indices) return entries[idx].value
        }
        return ""
    }

    /** Try to find a product name from the JSON */
    private fun extractProductName(root: JsonElement, goodsId: String): String {
        fun findName(element: JsonElement): String? {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                for (key in listOf("goods_name", "name", "title", "goodsName", "product_name")) {
                    if (obj.has(key)) {
                        val v = obj.get(key)
                        if (v.isJsonPrimitive) return v.asString
                    }
                }
                for ((_, value) in obj.entrySet()) {
                    val found = findName(value)
                    if (found != null) return found
                }
            }
            return null
        }
        return findName(root) ?: "商品 $goodsId"
    }

    /** Try to find a thumbnail URL from the JSON */
    private fun findThumbnail(root: JsonElement): String {
        fun findImage(element: JsonElement): String? {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                for (key in listOf("goods_image", "image", "thumb", "thumbnail", "pic", "img", "logo")) {
                    if (obj.has(key)) {
                        val v = obj.get(key)
                        if (v.isJsonPrimitive) {
                            val s = v.asString
                            if (s.startsWith("http")) return s
                        }
                    }
                }
                for ((_, value) in obj.entrySet()) {
                    val found = findImage(value)
                    if (found != null) return found
                }
            }
            return null
        }
        return findImage(root) ?: ""
    }

    companion object {
        fun create(): GoodsInfoApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
            return GoodsInfoApi(client)
        }
    }
}
