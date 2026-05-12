package com.max.aiassistant.data.realtime

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeSessionConfigSerializationTest {

    private val gson = Gson()

    @Test
    fun `translation session update only sends output language`() {
        val sessionConfig = SessionConfig(
            audio = RealtimeAudioConfig(
                output = RealtimeAudioOutputConfig(
                    format = null,
                    voice = null,
                    language = "fr"
                )
            )
        )

        val payload = gson.toJsonTree(
            mapOf(
                "type" to "session.update",
                "session" to sessionConfig
            )
        ).asJsonObject
        val rawJson = gson.toJson(payload)

        assertFalse(rawJson.contains("source_language"))
        assertFalse(rawJson.contains("target_language"))
        assertFalse(rawJson.contains("sourceLanguage"))
        assertFalse(rawJson.contains("targetLanguage"))
        assertFalse(rawJson.contains("output_modalities"))
        assertFalse(rawJson.contains("turn_detection"))
        assertFalse(rawJson.contains("tool_choice"))
        assertFalse(rawJson.contains("max_output_tokens"))

        val output = payload
            .getAsJsonObject("session")
            .getAsJsonObject("audio")
            .getAsJsonObject("output")
        assertNotNull(output)
        assertTrue(output.get("language").asString == "fr")
    }

    @Test
    fun `conversation session update puts turn detection under audio input`() {
        val sessionConfig = SessionConfig(
            type = "realtime",
            model = "gpt-realtime-2",
            outputModalities = listOf("audio"),
            audio = RealtimeAudioConfig(
                input = RealtimeAudioInputConfig(
                    format = RealtimeAudioFormat(),
                    transcription = InputAudioTranscription(language = "fr"),
                    turnDetection = TurnDetection(
                        type = "server_vad",
                        createResponse = true,
                        interruptResponse = true
                    )
                ),
                output = RealtimeAudioOutputConfig(
                    format = RealtimeAudioFormat(),
                    voice = "echo"
                )
            )
        )

        val payload = gson.toJsonTree(
            mapOf(
                "type" to "session.update",
                "session" to sessionConfig
            )
        ).asJsonObject
        val rawJson = gson.toJson(payload)

        assertFalse(rawJson.contains("\"session\":{\"turn_detection\""))
        val session = payload.getAsJsonObject("session")
        assertFalse(session.has("turn_detection"))

        val turnDetection = session
            .getAsJsonObject("audio")
            .getAsJsonObject("input")
            .getAsJsonObject("turn_detection")
        assertNotNull(turnDetection)
        assertTrue(turnDetection.get("type").asString == "server_vad")
    }
}
