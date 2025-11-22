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
 * Les champs non définis (additional_kwargs, response_metadata, etc.) sont ignorés
 */
data class MessageContent(
    val type: String,  // "human" ou "ai"
    val content: String
    // Autres champs ignorés: additional_kwargs, response_metadata, tool_calls, invalid_tool_calls
)
