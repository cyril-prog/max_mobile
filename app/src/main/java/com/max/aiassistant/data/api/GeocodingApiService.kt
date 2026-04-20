package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Service API pour le géocodage (conversion nom de ville -> coordonnées)
 * Utilise l'API Open-Meteo Geocoding
 * Documentation: https://open-meteo.com/en/docs/geocoding-api
 */
interface GeocodingApiService {

    /**
     * Recherche une ville par son nom.
     */
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "fr"
    ): GeocodingApiResponse

    companion object {
        private const val BASE_URL = "https://geocoding-api.open-meteo.com/"

        fun create(): GeocodingApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(GeocodingApiService::class.java)
        }
    }
}

data class GeocodingApiResponse(
    val results: List<CityResult>? = null
)

data class CityResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    @SerializedName("country_code")
    val countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val admin3: String? = null,
    val admin4: String? = null
) {
    fun getFullName(): String {
        val parts = mutableListOf(name)
        admin1?.let { parts.add(it) }
        parts.add(country)
        return parts.joinToString(", ")
    }
}
