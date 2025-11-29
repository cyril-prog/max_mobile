package com.max.aiassistant.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Service API pour le géocodage (conversion nom de ville → coordonnées)
 * Utilise l'API Open-Meteo Geocoding
 * Documentation: https://open-meteo.com/en/docs/geocoding-api
 */
interface GeocodingApiService {

    /**
     * Recherche une ville par son nom
     *
     * @param name Nom de la ville à rechercher
     * @param count Nombre de résultats maximum (défaut: 10)
     * @param language Langue des résultats (défaut: fr)
     */
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "fr"
    ): GeocodingApiResponse

    companion object {
        private const val BASE_URL = "https://geocoding-api.open-meteo.com/"

        /**
         * Crée une instance du service API de géocodage
         */
        fun create(): GeocodingApiService {
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

            return retrofit.create(GeocodingApiService::class.java)
        }
    }
}

/**
 * Réponse de l'API de géocodage
 */
data class GeocodingApiResponse(
    val results: List<CityResult>? = null
)

/**
 * Résultat de recherche de ville
 */
data class CityResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val admin1: String? = null,  // Région/État
    val admin2: String? = null,  // Département
    val admin3: String? = null,  // Arrondissement
    val admin4: String? = null   // Canton
) {
    /**
     * Retourne le nom complet de la ville avec région et pays
     */
    fun getFullName(): String {
        val parts = mutableListOf(name)
        admin1?.let { parts.add(it) }
        parts.add(country)
        return parts.joinToString(", ")
    }
}
