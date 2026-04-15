package com.max.aiassistant.data.realtime

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * Service pour gérer la connexion WebSocket avec l'API Realtime d'OpenAI
 *
 * Ce service permet :
 * - Connexion/déconnexion à l'API Realtime via WebSocket
 * - Envoi d'événements au serveur (audio, configuration)
 * - Réception d'événements du serveur (audio, transcriptions)
 * - Gestion automatique de la reconnexion
 */
class RealtimeApiService(
    private val apiKey: String,
    private val model: String = "gpt-realtime"
) {
    private val TAG = "RealtimeApiService"
    private val gson = Gson()

    // Client OkHttp pour WebSocket
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    // État de la connexion
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Flux d'événements reçus du serveur
    private val _serverEvents = MutableSharedFlow<RealtimeServerEvent>(replay = 0)
    val serverEvents: SharedFlow<RealtimeServerEvent> = _serverEvents.asSharedFlow()

    // Flux d'erreurs
    private val _errors = MutableSharedFlow<String>(replay = 0)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    // Scope pour les coroutines
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    /**
     * Connecte au serveur WebSocket de l'API Realtime
     * @param instructions Prompt systeme complet a utiliser pour la session
     */
    fun connect(instructions: String) {
        if (_isConnected.value) {
            Log.w(TAG, "Déjà connecté à l'API Realtime")
            return
        }

        val url = "wss://api.openai.com/v1/realtime?model=$model"
        Log.d(TAG, "Connexion à l'API Realtime: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "WebSocket connecté avec succès")
                _isConnected.value = true

                val enrichedInstructions = instructions
                Log.d(TAG, "========================================")
                Log.d(TAG, "PROMPT SYSTÈME VOICE-TO-VOICE COMPLET:")
                Log.d(TAG, "========================================")
                Log.d(TAG, enrichedInstructions)
                Log.d(TAG, "========================================")
                Log.d(TAG, "Longueur totale du prompt: ${enrichedInstructions.length} caractères")
                Log.d(TAG, "========================================")

                sendSessionUpdate(SessionConfig(
                    modalities = listOf("text", "audio"),
                    instructions = enrichedInstructions,
                    voice = "echo",
                    inputAudioFormat = "pcm16",
                    outputAudioFormat = "pcm16",
                    inputAudioTranscription = InputAudioTranscription(
                        model = "whisper-1",
                        language = "fr"  // Force la transcription en français
                    ),
                    turnDetection = TurnDetection(type = "server_vad"),
                    tools = listOf(buildStoreImportantMemoryTool()),
                    toolChoice = "auto",
                    temperature = 0.8
                ))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Log réduit pour éviter de saturer avec les événements audio
                if (text.contains("response.audio.delta")) {
                    Log.d(TAG, "Message reçu: response.audio.delta (audio chunk)")
                } else {
                    Log.d(TAG, "Message reçu: ${text.take(200)}...")
                }
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket en cours de fermeture: $code - $reason")
                webSocket.close(1000, null)
                _isConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket fermé: $code - $reason")
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "Erreur WebSocket: ${t.message}", t)
                _isConnected.value = false
                serviceScope.launch {
                    _errors.emit("Erreur de connexion: ${t.message}")
                }
            }
        })
    }

    /**
     * Déconnecte du serveur WebSocket
     */
    fun disconnect() {
        Log.d(TAG, "Déconnexion de l'API Realtime")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _isConnected.value = false
    }

    /**
     * Envoie une mise à jour de la configuration de session
     */
    fun sendSessionUpdate(config: SessionConfig) {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "session.update",
                "session" to config
            )
            sendEvent(event)
        }
    }

    /**
     * Envoie un chunk d'audio au serveur (Base64 encoded PCM16)
     * Exécuté sur un thread background pour éviter de bloquer le thread principal
     */
    fun sendAudioChunk(base64Audio: String) {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "input_audio_buffer.append",
                "audio" to base64Audio
            )
            sendEvent(event)
        }
    }

    /**
     * Commit le buffer audio (déclenche la transcription et la réponse)
     */
    fun commitAudioBuffer() {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "input_audio_buffer.commit"
            )
            sendEvent(event)
        }
    }

    /**
     * Supprime le buffer audio en cours
     */
    fun clearAudioBuffer() {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "input_audio_buffer.clear"
            )
            sendEvent(event)
        }
    }

    /**
     * Annule la réponse en cours
     */
    fun cancelResponse() {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "response.cancel"
            )
            sendEvent(event)
        }
    }

    fun sendFunctionCallOutput(
        callId: String,
        outputJson: String
    ) {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "conversation.item.create",
                "item" to mapOf(
                    "type" to "function_call_output",
                    "call_id" to callId,
                    "output" to outputJson
                )
            )
            sendEvent(event)
        }
    }

    fun requestModelResponse() {
        serviceScope.launch(Dispatchers.IO) {
            val event = mapOf(
                "type" to "response.create"
            )
            sendEvent(event)
        }
    }

    /**
     * Envoie un événement générique au serveur
     */
    private fun sendEvent(event: Map<String, Any>) {
        val json = gson.toJson(event)

        // Log réduit pour éviter de saturer avec les chunks audio
        val eventType = event["type"] as? String ?: "unknown"
        if (eventType == "input_audio_buffer.append") {
            Log.d(TAG, "Envoi événement: $eventType (audio chunk)")
        } else {
            Log.d(TAG, "Envoi événement: ${json.take(200)}...")
        }

        webSocket?.send(json)
    }

    /**
     * Traite un message JSON reçu du serveur
     */
    private fun handleServerMessage(json: String) {
        try {
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            val type = jsonObject.get("type")?.asString ?: "unknown"

            val event = when (type) {
                "error" -> {
                    val error = gson.fromJson(jsonObject.getAsJsonObject("error"), ErrorDetail::class.java)
                    RealtimeServerEvent.Error(error)
                }

                "session.created" -> {
                    val session = gson.fromJson(jsonObject.getAsJsonObject("session"), SessionConfig::class.java)
                    RealtimeServerEvent.SessionCreated(session)
                }

                "session.updated" -> {
                    val session = gson.fromJson(jsonObject.getAsJsonObject("session"), SessionConfig::class.java)
                    RealtimeServerEvent.SessionUpdated(session)
                }

                "conversation.created" -> {
                    val conversation = gson.fromJson(jsonObject.getAsJsonObject("conversation"), Conversation::class.java)
                    RealtimeServerEvent.ConversationCreated(conversation)
                }

                "input_audio_buffer.speech_started" -> {
                    val audioStartMs = jsonObject.get("audio_start_ms")?.asInt ?: 0
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    RealtimeServerEvent.InputAudioBufferSpeechStarted(audioStartMs, itemId)
                }

                "input_audio_buffer.speech_stopped" -> {
                    val audioEndMs = jsonObject.get("audio_end_ms")?.asInt ?: 0
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    RealtimeServerEvent.InputAudioBufferSpeechStopped(audioEndMs, itemId)
                }

                "input_audio_buffer.committed" -> {
                    val previousItemId = jsonObject.get("previous_item_id")?.takeIf { !it.isJsonNull }?.asString
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    RealtimeServerEvent.InputAudioBufferCommitted(previousItemId, itemId)
                }

                "response.created" -> {
                    val response = gson.fromJson(jsonObject.getAsJsonObject("response"), Response::class.java)
                    RealtimeServerEvent.ResponseCreated(response)
                }

                "response.done" -> {
                    val response = gson.fromJson(jsonObject.getAsJsonObject("response"), Response::class.java)
                    RealtimeServerEvent.ResponseDone(response)
                }

                "response.audio.delta" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.ResponseAudioDelta(responseId, itemId, outputIndex, contentIndex, delta)
                }

                "response.audio.done" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    RealtimeServerEvent.ResponseAudioDone(responseId, itemId, outputIndex, contentIndex)
                }

                "response.audio_transcript.done" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val transcript = jsonObject.get("transcript")?.asString ?: ""
                    RealtimeServerEvent.ResponseAudioTranscriptDone(responseId, itemId, outputIndex, contentIndex, transcript)
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val transcript = jsonObject.get("transcript")?.asString ?: ""
                    RealtimeServerEvent.InputAudioTranscriptionCompleted(itemId, contentIndex, transcript)
                }

                "response.text.delta" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.ResponseTextDelta(responseId, itemId, outputIndex, contentIndex, delta)
                }

                "response.text.done" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val text = jsonObject.get("text")?.asString ?: ""
                    RealtimeServerEvent.ResponseTextDone(responseId, itemId, outputIndex, contentIndex, text)
                }

                else -> {
                    Log.d(TAG, "Événement non géré: $type")
                    RealtimeServerEvent.Unknown(type, json)
                }
            }

            // Émet l'événement dans le flux
            serviceScope.launch {
                _serverEvents.emit(event)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du traitement du message serveur", e)
            serviceScope.launch {
                _errors.emit("Erreur de traitement: ${e.message}")
            }
        }
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        disconnect()
        client.dispatcher.executorService.shutdown()
    }

    private fun buildStoreImportantMemoryTool(): Map<String, Any> {
        return mapOf(
            "type" to "function",
            "name" to "store_important_memory",
            "description" to "A utiliser quand l'utilisateur donne un fait important, une preference, une relation entre entites, une decision ou toute information utile a long terme, afin de pouvoir la reutiliser dans une future conversation. Memorise en priorite les informations de profil durables comme le prenom, le nom, le surnom, les preferences stables, les relations et le materiel. Exemple: \"Mon prenom est Cyril\" doit etre memorise. Pour un fait direct sur l'utilisateur, entity_name peut etre omis dans facts et vaudra \"utilisateur\".",
            "parameters" to mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "entities" to mapOf(
                        "type" to "array",
                        "description" to "Entites importantes a memoriser.",
                        "items" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "name" to mapOf("type" to "string"),
                                "type" to mapOf("type" to "string"),
                                "canonical_name" to mapOf("type" to "string"),
                                "summary" to mapOf("type" to "string")
                            ),
                            "required" to listOf("name", "type")
                        )
                    ),
                    "relations" to mapOf(
                        "type" to "array",
                        "description" to "Relations stables entre entites.",
                        "items" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "from_entity_name" to mapOf("type" to "string"),
                                "relation_type" to mapOf("type" to "string"),
                                "to_entity_name" to mapOf("type" to "string"),
                                "confidence" to mapOf("type" to "number")
                            ),
                            "required" to listOf("from_entity_name", "relation_type", "to_entity_name")
                        )
                    ),
                    "facts" to mapOf(
                        "type" to "array",
                        "description" to "Faits durables rattaches a une entite.",
                        "items" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "entity_name" to mapOf("type" to "string"),
                                "fact_type" to mapOf("type" to "string"),
                                "value" to mapOf("type" to "string"),
                                "confidence" to mapOf("type" to "number")
                            ),
                            "required" to listOf("fact_type", "value")
                        )
                    )
                )
            )
        )
    }
}



