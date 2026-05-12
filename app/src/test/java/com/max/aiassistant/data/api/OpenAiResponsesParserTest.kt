package com.max.aiassistant.data.api

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenAiResponsesParserTest {

    @Test
    fun `extractErrorMessage ignore error null and keeps output text readable`() {
        val response = JsonParser.parseString(
            """{"error":null,"output_text":"ok"}"""
        ).asJsonObject

        assertNull(OpenAiResponsesParser.extractErrorMessage(response))
        assertEquals("ok", OpenAiResponsesParser.extractOutputText(response))
    }

    @Test
    fun `extractErrorMessage returns nested message when error is object`() {
        val response = JsonParser.parseString(
            """{"error":{"message":"quota depassee"}}"""
        ).asJsonObject

        assertEquals("quota depassee", OpenAiResponsesParser.extractErrorMessage(response))
    }

    @Test
    fun `extractErrorMessage returns primitive error without cast failure`() {
        val response = JsonParser.parseString(
            """{"error":"erreur brute"}"""
        ).asJsonObject

        assertEquals("erreur brute", OpenAiResponsesParser.extractErrorMessage(response))
    }

    @Test
    fun `extractErrorMessage ignores unsupported message type`() {
        val response = JsonParser.parseString(
            """{"error":{"message":{"detail":"ko"}}}"""
        ).asJsonObject

        assertNull(OpenAiResponsesParser.extractErrorMessage(response))
    }
}
