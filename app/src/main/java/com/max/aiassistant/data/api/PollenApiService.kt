package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

interface PollenApiService {

    @GET("v1/air-quality")
    suspend fun getPollenForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen",
        @Query("timezone") timezone: String = "Europe/Paris",
        @Query("forecast_days") forecastDays: Int = 3
    ): PollenApiResponse

    companion object {
        private const val BASE_URL = "https://air-quality-api.open-meteo.com/"

        fun create(): PollenApiService {
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

            return retrofit.create(PollenApiService::class.java)
        }
    }
}

data class PollenApiResponse(
    val timezone: String,
    @SerializedName("hourly_units")
    val hourlyUnits: PollenHourlyUnits,
    val hourly: PollenHourlyData
)

data class PollenHourlyUnits(
    val time: String,
    @SerializedName("alder_pollen")
    val alderPollen: String? = null,
    @SerializedName("birch_pollen")
    val birchPollen: String? = null,
    @SerializedName("grass_pollen")
    val grassPollen: String? = null,
    @SerializedName("mugwort_pollen")
    val mugwortPollen: String? = null,
    @SerializedName("olive_pollen")
    val olivePollen: String? = null,
    @SerializedName("ragweed_pollen")
    val ragweedPollen: String? = null
)

data class PollenHourlyData(
    val time: List<String>,
    @SerializedName("alder_pollen")
    val alderPollen: List<Double?> = emptyList(),
    @SerializedName("birch_pollen")
    val birchPollen: List<Double?> = emptyList(),
    @SerializedName("grass_pollen")
    val grassPollen: List<Double?> = emptyList(),
    @SerializedName("mugwort_pollen")
    val mugwortPollen: List<Double?> = emptyList(),
    @SerializedName("olive_pollen")
    val olivePollen: List<Double?> = emptyList(),
    @SerializedName("ragweed_pollen")
    val ragweedPollen: List<Double?> = emptyList()
)

fun PollenApiResponse.toCurrentPollen(
    referenceTime: LocalDateTime = LocalDateTime.now(ZoneId.of(timezone))
): CurrentPollenData {
    val currentHour = referenceTime.truncatedTo(ChronoUnit.HOURS)
    val index = hourly.indexFor(currentHour)

    return CurrentPollenData(
        grassPollen = hourly.grassPollen.getOrNull(index),
        birchPollen = hourly.birchPollen.getOrNull(index),
        alderPollen = hourly.alderPollen.getOrNull(index),
        olivePollen = hourly.olivePollen.getOrNull(index),
        mugwortPollen = hourly.mugwortPollen.getOrNull(index),
        ragweedPollen = hourly.ragweedPollen.getOrNull(index)
    )
}

private fun PollenHourlyData.indexFor(referenceTime: LocalDateTime): Int {
    val exactIndex = time.indexOf(referenceTime.toString())
    if (exactIndex >= 0) {
        return exactIndex
    }

    val closestPastIndex = time.indexOfLast { entry ->
        runCatching { !LocalDateTime.parse(entry).isAfter(referenceTime) }.getOrDefault(false)
    }

    return if (closestPastIndex >= 0) closestPastIndex else 0
}
