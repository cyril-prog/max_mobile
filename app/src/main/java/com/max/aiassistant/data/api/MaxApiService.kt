package com.max.aiassistant.data.api

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Service API pour communiquer avec le webhook Max Mobile
 */
interface MaxApiService {

    @POST("webhook/max_mobile")
    suspend fun sendMessage(@Body request: ChatMessageRequest): Response<ResponseBody>

    @GET("webhook/get_tasks")
    suspend fun getTasks(): TasksApiResponse

    @GET("webhook/get_calendar")
    suspend fun getCalendarEvents(): CalendarApiResponse

    @GET("webhook/get_recent_messages")
    suspend fun getRecentMessages(): MessagesApiResponse

    @POST("webhook/save_conv")
    suspend fun saveConversation(@Body payload: List<MessageData>): Response<ResponseBody>

    @GET("webhook/get_memory")
    suspend fun getMemory(): List<MemoryItem>

    @GET("webhook/del_task")
    suspend fun deleteTask(@Query("id") taskId: String): Response<ResponseBody>

    @POST("webhook/upd_task")
    suspend fun updateTask(@Body task: TaskUpdateRequest): Response<ResponseBody>

    @POST("webhook/create_task")
    suspend fun createTask(@Body task: TaskCreateRequest): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://n8n.srv1086212.hstgr.cloud/"

        /**
         * Crée une instance de l'API service
         */
        fun create(): MaxApiService {
            // Logging interceptor pour déboguer les requêtes
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Configuration du client HTTP
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            // Configuration de Retrofit
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(MaxApiService::class.java)
        }
    }
}

/**
 * Requête pour envoyer un message au chat
 * @param text Le texte du message
 * @param image L'image encodée en Base64 (optionnel, null si pas d'image)
 */
data class ChatMessageRequest(
    val text: String,
    val image: String? = null
)
