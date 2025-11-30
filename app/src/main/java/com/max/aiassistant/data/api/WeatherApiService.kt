package com.max.aiassistant.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Service API pour récupérer les données météo depuis Open-Meteo
 * Documentation: https://open-meteo.com/en/docs
 */
interface WeatherApiService {

    /**
     * Récupère les prévisions météo pour une localisation donnée
     *
     * @param latitude Latitude de la localisation (ex: 47.2184 pour Nantes)
     * @param longitude Longitude de la localisation (ex: -1.5536 pour Nantes)
     * @param current Paramètres météo actuels à récupérer (séparés par des virgules)
     * @param hourly Paramètres météo horaires à récupérer (séparés par des virgules)
     * @param daily Paramètres météo quotidiens à récupérer (séparés par des virgules)
     * @param timezone Fuseau horaire (ex: "Europe/Paris")
     * @param forecastDays Nombre de jours de prévisions (max 16)
     */
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double = 47.2184,  // Nantes par défaut
        @Query("longitude") longitude: Double = -1.5536,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,grass_pollen,birch_pollen,alder_pollen,olive_pollen,mugwort_pollen,ragweed_pollen",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code",
        @Query("timezone") timezone: String = "Europe/Paris",
        @Query("forecast_days") forecastDays: Int = 15
    ): WeatherApiResponse

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        /**
         * Crée une instance du service API météo
         */
        fun create(): WeatherApiService {
            // Logging interceptor pour déboguer les requêtes
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Configuration du client HTTP
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            // Configuration de Retrofit
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(WeatherApiService::class.java)
        }
    }
}
