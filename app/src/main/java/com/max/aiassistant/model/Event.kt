package com.max.aiassistant.model

/**
 * Représente un événement dans le planning (type Google Calendar)
 *
 * @property id Identifiant unique
 * @property title Titre de l'événement
 * @property startTime Heure de début (format "HH:MM AM/PM")
 * @property endTime Heure de fin (format "HH:MM AM/PM")
 * @property description Description optionnelle
 * @property location Lieu de l'événement
 * @property source Source de l'événement (ex: "Google Calendar", "Outlook")
 */
data class Event(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val description: String = "",
    val location: String = "",
    val source: String = "Google Calendar"
)
