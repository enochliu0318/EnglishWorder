package com.englishworder.data.remote.youdao

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface YoudaoJsonApi {

    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
    @GET("jsonapi")
    suspend fun lookup(
        @Query("q") word: String,
        @Query("le") lang: String = "en"
    ): ResponseBody
}
