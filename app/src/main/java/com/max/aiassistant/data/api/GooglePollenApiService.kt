package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GooglePollenApiService {

    @GET("v1/forecast:lookup")
    suspend fun lookupForecast(
        @Query("key") apiKey: String,
        @Query("location.latitude") latitude: Double,
        @Query("location.longitude") longitude: Double,
        @Query("days") days: Int = 1,
        @Query("languageCode") languageCode: String = "fr",
        @Query("plantsDescription") plantsDescription: Boolean = false
    ): GooglePollenForecastResponse

    companion object {
        private const val BASE_URL = "https://pollen.googleapis.com/"

        fun create(): GooglePollenApiService {
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

            return retrofit.create(GooglePollenApiService::class.java)
        }
    }
}

data class GooglePollenForecastResponse(
    val regionCode: String? = null,
    val dailyInfo: List<GooglePollenDayInfo> = emptyList()
)

data class GooglePollenDayInfo(
    val plantInfo: List<GooglePollenPlantInfo> = emptyList(),
    val pollenTypeInfo: List<GooglePollenTypeInfo> = emptyList()
)

data class GooglePollenTypeInfo(
    val code: String,
    val displayName: String? = null,
    val inSeason: Boolean? = null,
    val indexInfo: GooglePollenIndexInfo? = null
)

data class GooglePollenPlantInfo(
    val code: String,
    val displayName: String? = null,
    val inSeason: Boolean? = null,
    val indexInfo: GooglePollenIndexInfo? = null,
    val plantDescription: GooglePollenPlantDescription? = null
)

data class GooglePollenIndexInfo(
    val value: Int? = null,
    val category: String? = null
)

data class GooglePollenPlantDescription(
    @SerializedName("type")
    val pollenType: String? = null
)

fun GooglePollenForecastResponse.toCurrentPollenData(): CurrentPollenData? {
    val currentDay = dailyInfo.firstOrNull() ?: return null
    val plants = currentDay.plantInfo.map { plant ->
        PollenPlantData(
            code = plant.code,
            displayName = plant.displayName ?: plant.code,
            plantType = plant.plantDescription?.pollenType ?: plant.code.toGooglePlantType(),
            indexValue = plant.indexInfo?.value,
            category = plant.indexInfo?.category,
            inSeason = plant.inSeason
        )
    }

    return CurrentPollenData(
        grassPollen = currentDay.plantInfo.findPlantScore("GRAMINALES"),
        birchPollen = currentDay.plantInfo.findPlantScore("BIRCH"),
        alderPollen = currentDay.plantInfo.findPlantScore("ALDER"),
        olivePollen = currentDay.plantInfo.findPlantScore("OLIVE"),
        mugwortPollen = currentDay.plantInfo.findPlantScore("MUGWORT"),
        ragweedPollen = currentDay.plantInfo.findPlantScore("RAGWEED"),
        pollenTypes = currentDay.pollenTypeInfo.map { type ->
            PollenTypeData(
                code = type.code,
                displayName = type.displayName ?: type.code,
                indexValue = type.indexInfo?.value,
                category = type.indexInfo?.category,
                inSeason = type.inSeason
            )
        },
        pollenPlants = plants,
        pollenSource = "google_pollen"
    )
}

private fun List<GooglePollenPlantInfo>.findPlantScore(code: String): Double? {
    return firstOrNull { it.code == code }?.indexInfo?.value?.toDouble()
}

private fun String.toGooglePlantType(): String {
    return when (this) {
        "GRAMINALES" -> "GRASS"
        "RAGWEED", "MUGWORT" -> "WEED"
        else -> "TREE"
    }
}
