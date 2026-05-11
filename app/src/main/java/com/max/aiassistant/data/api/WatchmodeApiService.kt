package com.max.aiassistant.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Service API Watchmode.
 */
interface WatchmodeApiService {

    @GET("v1/list-titles/")
    suspend fun listTitles(
        @Query("apiKey") apiKey: String,
        @Query("source_ids") sourceIds: String,
        @Query("regions") regions: String = "FR",
        @Query("types") types: String = "movie,tv_series",
        @Query("sort_by") sortBy: String = "release_date_desc",
        @Query("release_date_start") releaseDateStart: String,
        @Query("release_date_end") releaseDateEnd: String,
        @Query("page") page: Int = 1
    ): WatchmodeListTitlesResponse

    @GET("v1/title/{id}/details/")
    suspend fun getTitleDetails(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String
    ): WatchmodeTitleDetails

    companion object {
        private const val BASE_URL = "https://api.watchmode.com/"

        fun create(): WatchmodeApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(WatchmodeApiService::class.java)
        }
    }
}
