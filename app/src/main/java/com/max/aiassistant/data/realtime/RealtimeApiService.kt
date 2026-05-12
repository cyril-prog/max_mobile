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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Service pour gerer la connexion WebSocket avec l'API Realtime d'OpenAI.
 */
class RealtimeApiService(
    private val apiKey: String
) {
    private val tag = "RealtimeApiService"
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var activeConnectionConfig: RealtimeConnectionConfig? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _serverEvents = MutableSharedFlow<RealtimeServerEvent>(replay = 0)
    val serverEvents: SharedFlow<RealtimeServerEvent> = _serverEvents.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(replay = 0)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    fun connect(connectionConfig: RealtimeConnectionConfig) {
        if (_isConnected.value) {
            Log.w(tag, "Deja connecte a l'API Realtime")
            return
        }

        val url = "wss://api.openai.com${connectionConfig.endpointPath}?model=${connectionConfig.model}"
        Log.d(tag, "Connexion a l'API Realtime: $url")
        activeConnectionConfig = connectionConfig

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d(tag, "WebSocket connecte avec succes")
                _isConnected.value = true

                val sessionConfig = connectionConfig.sessionConfig
                Log.d(tag, "========================================")
                Log.d(tag, "PROMPT SYSTEME VOICE COMPLET:")
                Log.d(tag, "========================================")
                Log.d(tag, sessionConfig.instructions.orEmpty())
                Log.d(tag, "========================================")
                Log.d(tag, "Longueur totale du prompt: ${sessionConfig.instructions.orEmpty().length} caracteres")
                Log.d(tag, "========================================")

                sendSessionUpdate(sessionConfig)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("response.audio.delta") || text.contains("response.output_audio.delta")) {
                    Log.d(tag, "Message recu: audio delta (audio chunk)")
                } else {
                    Log.d(tag, "Message recu: ${text.take(200)}...")
                }
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket en cours de fermeture: $code - $reason")
                webSocket.close(1000, null)
                _isConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket ferme: $code - $reason")
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(tag, "Erreur WebSocket: ${t.message}", t)
                _isConnected.value = false
                serviceScope.launch {
                    _errors.emit("Erreur de connexion: ${t.message}")
                }
            }
        })
    }

    fun disconnect() {
        Log.d(tag, "Deconnexion de l'API Realtime")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        activeConnectionConfig = null
        _isConnected.value = false
    }

    fun sendSessionUpdate(config: SessionConfig) {
        serviceScope.launch(Dispatchers.IO) {
            Log.d(
                tag,
                "Session.update VAD: type=${config.audio?.input?.turnDetection?.type}, " +
                    "create_response=${config.audio?.input?.turnDetection?.createResponse}, " +
                    "interrupt_response=${config.audio?.input?.turnDetection?.interruptResponse}"
            )
            sendEvent(
                mapOf(
                    "type" to "session.update",
                    "session" to config
                )
            )
        }
    }

    fun sendAudioChunk(base64Audio: String) {
        serviceScope.launch(Dispatchers.IO) {
            sendEvent(
                mapOf(
                    "type" to (activeConnectionConfig?.audioAppendEventType ?: "input_audio_buffer.append"),
                    "audio" to base64Audio
                )
            )
        }
    }

    fun commitAudioBuffer() {
        serviceScope.launch(Dispatchers.IO) {
            sendEvent(mapOf("type" to "input_audio_buffer.commit"))
        }
    }

    fun clearAudioBuffer() {
        serviceScope.launch(Dispatchers.IO) {
            sendEvent(mapOf("type" to (activeConnectionConfig?.audioClearEventType ?: "input_audio_buffer.clear")))
        }
    }

    fun cancelResponse() {
        serviceScope.launch(Dispatchers.IO) {
            sendEvent(mapOf("type" to "response.cancel"))
        }
    }

    fun sendFunctionCallOutput(
        callId: String,
        outputJson: String
    ) {
        serviceScope.launch(Dispatchers.IO) {
            sendEvent(
                mapOf(
                    "type" to "conversation.item.create",
                    "item" to mapOf(
                        "type" to "function_call_output",
                        "call_id" to callId,
                        "output" to outputJson
                    )
                )
            )
        }
    }

    fun requestModelResponse() {
        serviceScope.launch(Dispatchers.IO) {
            sendEvent(mapOf("type" to "response.create"))
        }
    }

    private fun sendEvent(event: Map<String, Any>) {
        val json = gson.toJson(event)
        val eventType = event["type"] as? String ?: "unknown"
        if (eventType == "input_audio_buffer.append") {
            Log.d(tag, "Envoi evenement: $eventType (audio chunk)")
        } else {
            Log.d(tag, "Envoi evenement: ${json.take(200)}...")
        }
        webSocket?.send(json)
    }

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

                "response.audio.delta", "response.output_audio.delta" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.ResponseAudioDelta(responseId, itemId, outputIndex, contentIndex, delta)
                }

                "response.audio.done", "response.output_audio.done" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    RealtimeServerEvent.ResponseAudioDone(responseId, itemId, outputIndex, contentIndex)
                }

                "response.audio_transcript.done", "response.output_audio_transcript.done" -> {
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

                "response.text.delta", "response.output_text.delta", "response.output_audio_transcript.delta" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.ResponseTextDelta(responseId, itemId, outputIndex, contentIndex, delta)
                }

                "response.text.done", "response.output_text.done" -> {
                    val responseId = jsonObject.get("response_id")?.asString ?: ""
                    val itemId = jsonObject.get("item_id")?.asString ?: ""
                    val outputIndex = jsonObject.get("output_index")?.asInt ?: 0
                    val contentIndex = jsonObject.get("content_index")?.asInt ?: 0
                    val text = jsonObject.get("text")?.asString ?: ""
                    RealtimeServerEvent.ResponseTextDone(responseId, itemId, outputIndex, contentIndex, text)
                }

                "session.output_audio.delta" -> {
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.TranslationOutputAudioDelta(delta)
                }

                "session.output_audio.done" -> {
                    RealtimeServerEvent.TranslationOutputAudioDone
                }

                "session.input_transcript.delta" -> {
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.TranslationInputTranscriptDelta(delta)
                }

                "session.output_transcript.delta" -> {
                    val delta = jsonObject.get("delta")?.asString ?: ""
                    RealtimeServerEvent.TranslationOutputTranscriptDelta(delta)
                }

                else -> {
                    Log.d(tag, "Evenement non gere: $type")
                    RealtimeServerEvent.Unknown(type, json)
                }
            }

            serviceScope.launch {
                _serverEvents.emit(event)
            }
        } catch (e: Exception) {
            Log.e(tag, "Erreur lors du traitement du message serveur", e)
            serviceScope.launch {
                _errors.emit("Erreur de traitement: ${e.message}")
            }
        }
    }

    fun cleanup() {
        disconnect()
        client.dispatcher.executorService.shutdown()
    }
}
