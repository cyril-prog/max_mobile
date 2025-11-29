package com.max.aiassistant.model

/**
 * Modèle de données pour une note
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
