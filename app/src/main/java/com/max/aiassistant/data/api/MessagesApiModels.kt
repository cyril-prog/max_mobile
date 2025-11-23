package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName

/**
 * Modèles de données pour l'API de récupération des messages récents
 *
 * Structure de l'API:
 * {
 *   "text": {
 *     "data": [
 *       { "id": 1146, "session_id": "...", "message": {...} },
 *       { "id": 1145, "session_id": "...", "message": {...} }
 *     ]
 *   }
 * }
 */

/**
 * Réponse racine avec le champ "text"
 */
data class MessagesApiResponse(
    val text: MessagesTextWrapper
)

/**
 * Wrapper "text" contenant le champ "data"
 */
data class MessagesTextWrapper(
    val data: List<MessageData>
)

/**
 * Un message dans la réponse de l'API
 */
data class MessageData(
    val id: Int,
    @SerializedName("session_id")
    val sessionId: String,
    val message: MessageContent
)

/**
 * Contenu d'un message
 */
data class MessageContent(
    val type: String,  // "human", "ai", ou "user"
    val content: String,

    @SerializedName("tool_calls")
    val toolCalls: List<Any> = emptyList(),

    @SerializedName("additional_kwargs")
    val additionalKwargs: Map<String, Any> = emptyMap(),

    @SerializedName("response_metadata")
    val responseMetadata: Map<String, Any> = emptyMap(),

    @SerializedName("invalid_tool_calls")
    val invalidToolCalls: List<Any> = emptyList()
)
