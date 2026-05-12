package com.max.aiassistant.data.realtime

data class VoiceLanguage(
    val code: String,
    val label: String
)

enum class VoiceMode(
    val uiLabel: String,
    val uiDescription: String,
    val endpointPath: String,
    val model: String,
    val outputModalities: List<String>,
    val audioAppendEventType: String,
    val audioClearEventType: String,
    val playsAudioResponse: Boolean
) {
    AI_CONVERSATION(
        uiLabel = "Conversation IA",
        uiDescription = "Dialogue vocal classique avec Max.",
        endpointPath = "/v1/realtime",
        model = "gpt-realtime-2",
        outputModalities = listOf("audio"),
        audioAppendEventType = "input_audio_buffer.append",
        audioClearEventType = "input_audio_buffer.clear",
        playsAudioResponse = true
    ),
    AUDIO_TO_TEXT_TRANSLATION(
        uiLabel = "Traduction ecrite",
        uiDescription = "Traduit l'audio en texte, sans lecture.",
        endpointPath = "/v1/realtime/translations",
        model = "gpt-realtime-translate",
        outputModalities = listOf("text"),
        audioAppendEventType = "session.input_audio_buffer.append",
        audioClearEventType = "session.input_audio_buffer.clear",
        playsAudioResponse = false
    ),
    AUDIO_TO_SPEECH_TRANSLATION(
        uiLabel = "Traduction orale",
        uiDescription = "Traduit l'audio en texte et en voix.",
        endpointPath = "/v1/realtime/translations",
        model = "gpt-realtime-translate",
        outputModalities = listOf("audio"),
        audioAppendEventType = "session.input_audio_buffer.append",
        audioClearEventType = "session.input_audio_buffer.clear",
        playsAudioResponse = true
    )
}

val SUPPORTED_VOICE_LANGUAGES = listOf(
    VoiceLanguage(code = "fr", label = "Francais"),
    VoiceLanguage(code = "en", label = "English"),
    VoiceLanguage(code = "es", label = "Espanol"),
    VoiceLanguage(code = "de", label = "Deutsch"),
    VoiceLanguage(code = "it", label = "Italiano"),
    VoiceLanguage(code = "ar", label = "Arabe"),
    VoiceLanguage(code = "bg", label = "Bulgare")
)
