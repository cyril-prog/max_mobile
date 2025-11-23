package com.max.aiassistant.data.realtime

import com.google.gson.annotations.SerializedName

/**
 * Modèles de données pour l'API Realtime d'OpenAI
 *
 * L'API Realtime fonctionne via WebSocket et échange des événements JSON
 * pour la communication audio bidirectionnelle en temps réel
 */

/**
 * Configuration de la session Realtime
 */
data class SessionConfig(
    @SerializedName("modalities")
    val modalities: List<String> = listOf("text", "audio"),

    @SerializedName("instructions")
    val instructions: String = "",

    @SerializedName("voice")
    val voice: String = "alloy",

    @SerializedName("input_audio_format")
    val inputAudioFormat: String = "pcm16",

    @SerializedName("output_audio_format")
    val outputAudioFormat: String = "pcm16",

    @SerializedName("input_audio_transcription")
    val inputAudioTranscription: InputAudioTranscription? = null,

    @SerializedName("turn_detection")
    val turnDetection: TurnDetection? = TurnDetection(type = "server_vad"),

    @SerializedName("tools")
    val tools: List<Any> = emptyList(),

    @SerializedName("tool_choice")
    val toolChoice: String = "auto",

    @SerializedName("temperature")
    val temperature: Double = 0.8,

    @SerializedName("max_response_output_tokens")
    val maxResponseOutputTokens: String = "inf"
)

data class InputAudioTranscription(
    @SerializedName("model")
    val model: String = "whisper-1"
)

data class TurnDetection(
    @SerializedName("type")
    val type: String,

    @SerializedName("threshold")
    val threshold: Double = 0.5,

    @SerializedName("prefix_padding_ms")
    val prefixPaddingMs: Int = 300,

    @SerializedName("silence_duration_ms")
    val silenceDurationMs: Int = 200
)

/**
 * Événements envoyés au serveur
 */
sealed class RealtimeClientEvent(val type: String) {

    /**
     * Mise à jour de la configuration de session
     */
    data class SessionUpdate(
        val session: SessionConfig
    ) : RealtimeClientEvent("session.update")

    /**
     * Envoi d'audio au serveur (Base64)
     */
    data class InputAudioBufferAppend(
        val audio: String // Base64 encoded audio
    ) : RealtimeClientEvent("input_audio_buffer.append")

    /**
     * Commit du buffer audio (déclenche la transcription et la réponse)
     */
    object InputAudioBufferCommit : RealtimeClientEvent("input_audio_buffer.commit")

    /**
     * Suppression du buffer audio
     */
    object InputAudioBufferClear : RealtimeClientEvent("input_audio_buffer.clear")

    /**
     * Création d'une réponse (sans audio, pour du texte)
     */
    data class ResponseCreate(
        val response: ResponseConfig? = null
    ) : RealtimeClientEvent("response.create")

    /**
     * Annulation de la réponse en cours
     */
    object ResponseCancel : RealtimeClientEvent("response.cancel")
}

data class ResponseConfig(
    @SerializedName("modalities")
    val modalities: List<String> = listOf("text", "audio"),

    @SerializedName("instructions")
    val instructions: String? = null,

    @SerializedName("voice")
    val voice: String? = null,

    @SerializedName("output_audio_format")
    val outputAudioFormat: String? = null,

    @SerializedName("tools")
    val tools: List<Any>? = null,

    @SerializedName("tool_choice")
    val toolChoice: String? = null,

    @SerializedName("temperature")
    val temperature: Double? = null,

    @SerializedName("max_output_tokens")
    val maxOutputTokens: Any? = null
)

/**
 * Événements reçus du serveur
 */
sealed class RealtimeServerEvent {

    /**
     * Erreur du serveur
     */
    data class Error(
        val error: ErrorDetail
    ) : RealtimeServerEvent()

    /**
     * Session créée
     */
    data class SessionCreated(
        val session: SessionConfig
    ) : RealtimeServerEvent()

    /**
     * Session mise à jour
     */
    data class SessionUpdated(
        val session: SessionConfig
    ) : RealtimeServerEvent()

    /**
     * Conversation créée
     */
    data class ConversationCreated(
        val conversation: Conversation
    ) : RealtimeServerEvent()

    /**
     * Buffer audio en cours de transcription
     */
    data class InputAudioBufferSpeechStarted(
        val audioStartMs: Int,
        val itemId: String
    ) : RealtimeServerEvent()

    /**
     * Fin de détection de parole
     */
    data class InputAudioBufferSpeechStopped(
        val audioEndMs: Int,
        val itemId: String
    ) : RealtimeServerEvent()

    /**
     * Buffer audio committé
     */
    data class InputAudioBufferCommitted(
        val previousItemId: String?,
        val itemId: String
    ) : RealtimeServerEvent()

    /**
     * Réponse créée
     */
    data class ResponseCreated(
        val response: Response
    ) : RealtimeServerEvent()

    /**
     * Réponse en cours
     */
    data class ResponseDone(
        val response: Response
    ) : RealtimeServerEvent()

    /**
     * Delta d'audio de sortie (Base64)
     */
    data class ResponseAudioDelta(
        val responseId: String,
        val itemId: String,
        val outputIndex: Int,
        val contentIndex: Int,
        val delta: String // Base64 audio chunk
    ) : RealtimeServerEvent()

    /**
     * Audio de sortie terminé
     */
    data class ResponseAudioDone(
        val responseId: String,
        val itemId: String,
        val outputIndex: Int,
        val contentIndex: Int
    ) : RealtimeServerEvent()

    /**
     * Transcription audio en entrée terminée
     */
    data class ResponseAudioTranscriptDone(
        val responseId: String,
        val itemId: String,
        val outputIndex: Int,
        val contentIndex: Int,
        val transcript: String
    ) : RealtimeServerEvent()

    /**
     * Delta de texte de sortie
     */
    data class ResponseTextDelta(
        val responseId: String,
        val itemId: String,
        val outputIndex: Int,
        val contentIndex: Int,
        val delta: String
    ) : RealtimeServerEvent()

    /**
     * Texte de sortie terminé
     */
    data class ResponseTextDone(
        val responseId: String,
        val itemId: String,
        val outputIndex: Int,
        val contentIndex: Int,
        val text: String
    ) : RealtimeServerEvent()

    /**
     * Événement non géré (fallback)
     */
    data class Unknown(
        val type: String,
        val rawJson: String
    ) : RealtimeServerEvent()
}

data class ErrorDetail(
    @SerializedName("type")
    val type: String,

    @SerializedName("code")
    val code: String?,

    @SerializedName("message")
    val message: String,

    @SerializedName("param")
    val param: String?
)

data class Conversation(
    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val objectType: String
)

data class Response(
    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val objectType: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("status_details")
    val statusDetails: Any?,

    @SerializedName("output")
    val output: List<Any>,

    @SerializedName("usage")
    val usage: Any?
)
