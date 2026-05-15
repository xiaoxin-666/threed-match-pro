package com.caluad.match3d.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

interface AdoreApi {
    @FormUrlEncoded
    @POST("/index.php")
    suspend fun admireGoods(
        @Query("ctl") ctl: String = "Goods_Goods",
        @Query("met") met: String = "admireGoods",
        @Query("typ") typ: String = "json",
        @Field("k") k: String,
        @Field("u") u: String,
        @Field("goods_id") goodsId: String
    ): AdoreResponse
}
