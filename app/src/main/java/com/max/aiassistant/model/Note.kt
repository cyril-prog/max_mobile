package com.max.aiassistant.model

/**
 * Type de contenu d'une note
 */
enum class NoteContentType {
    TEXT,      // Note textuelle simple
    CHECKLIST  // Liste de cases à cocher
}

/**
 * Item d'une checklist
 */
data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

/**
 * Modèle de données pour une note
 */
data class Note(
    val id: String,
    val title: String,
    val content: String = "",
    val contentType: NoteContentType = NoteContentType.TEXT,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
