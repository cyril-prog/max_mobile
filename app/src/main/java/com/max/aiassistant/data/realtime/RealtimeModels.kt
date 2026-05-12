package com.max.aiassistant.data.realtime

import com.google.gson.annotations.SerializedName

enum class RealtimeVoiceMode(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    CHAT(
        label = "Conversation avec l'IA",
        shortLabel = "Conversation",
        description = "Parler naturellement avec GPT-Realtime-2."
    ),
    TRANSLATE_TEXT(
        label = "Traduction ecrite",
        shortLabel = "Texte",
        description = "Ecouter une langue et afficher la traduction."
    ),
    TRANSLATE_SPEECH(
        label = "Traduction orale",
        shortLabel = "Oral",
        description = "Traduire vers du texte et une voix cible."
    );

    val isTranslation: Boolean
        get() = this != CHAT
}

data class RealtimeVoiceLanguage(
    val code: String,
    val label: String
)

data class RealtimeConnectionConfig(
    val endpointPath: String,
    val model: String,
    val audioAppendEventType: String = "input_audio_buffer.append",
    val audioClearEventType: String = "input_audio_buffer.clear",
    val sessionConfig: SessionConfig
)

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
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("model")
    val model: String? = null,

    @SerializedName("output_modalities")
    val outputModalities: List<String>? = null,

    @SerializedName("instructions")
    val instructions: String? = null,

    @SerializedName("audio")
    val audio: RealtimeAudioConfig? = null,

    @SerializedName("tools")
    val tools: List<Any>? = null,

    @SerializedName("tool_choice")
    val toolChoice: String? = null,

    @SerializedName("max_output_tokens")
    val maxResponseOutputTokens: String? = null
)

data class RealtimeAudioConfig(
    @SerializedName("input")
    val input: RealtimeAudioInputConfig? = null,

    @SerializedName("output")
    val output: RealtimeAudioOutputConfig? = null
)

data class RealtimeAudioInputConfig(
    @SerializedName("format")
    val format: RealtimeAudioFormat? = RealtimeAudioFormat(),

    @SerializedName("transcription")
    val transcription: InputAudioTranscription? = null,

    @SerializedName("turn_detection")
    val turnDetection: TurnDetection? = null
)

data class RealtimeAudioOutputConfig(
    @SerializedName("format")
    val format: RealtimeAudioFormat? = RealtimeAudioFormat(),

    @SerializedName("voice")
    val voice: String? = "echo",

    @SerializedName("language")
    val language: String? = null
)

data class RealtimeAudioFormat(
    @SerializedName("type")
    val type: String = "audio/pcm",

    @SerializedName("rate")
    val rate: Int = 24000
)

data class InputAudioTranscription(
    @SerializedName("model")
    val model: String = "whisper-1",

    @SerializedName("language")
    val language: String? = null  // Code langue ISO-639-1 (ex: "fr", "en")
)

data class TurnDetection(
    @SerializedName("type")
    val type: String,

    @SerializedName("threshold")
    val threshold: Double = 0.5,

    @SerializedName("prefix_padding_ms")
    val prefixPaddingMs: Int = 300,

    @SerializedName("silence_duration_ms")
    val silenceDurationMs: Int = 500,

    @SerializedName("create_response")
    val createResponse: Boolean = true,

    @SerializedName("interrupt_response")
    val interruptResponse: Boolean = true
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
     * Transcription de l'audio d'entrée utilisateur terminée
     */
    data class InputAudioTranscriptionCompleted(
        val itemId: String,
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
    data class TranslationInputTranscriptDelta(
        val delta: String
    ) : RealtimeServerEvent()

    data class TranslationOutputTranscriptDelta(
        val delta: String
    ) : RealtimeServerEvent()

    data class TranslationOutputAudioDelta(
        val delta: String
    ) : RealtimeServerEvent()

    object TranslationOutputAudioDone : RealtimeServerEvent()

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
    val output: List<ResponseOutputItem>,

    @SerializedName("usage")
    val usage: Any?
)

data class ResponseOutputItem(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("object")
    val objectType: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("call_id")
    val callId: String? = null,

    @SerializedName("arguments")
    val arguments: String? = null
)
