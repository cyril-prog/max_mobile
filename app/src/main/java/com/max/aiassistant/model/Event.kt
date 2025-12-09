package com.max.aiassistant.model

/**
 * Représente un événement dans le planning (type Google Calendar)
 *
 * @property id Identifiant unique
 * @property title Titre de l'événement
 * @property date Date de l'événement (format "Lun 30 novembre")
 * @property startTime Heure de début (format "HH:MM" ou "Toute la journée")
 * @property endTime Heure de fin (format "HH:MM")
 * @property description Description optionnelle
 * @property location Lieu de l'événement
 * @property source Source de l'événement (ex: "Google Calendar", "Outlook")
 * @property startDateTime Date/heure de début ISO (ex: "2025-11-26T12:00:00+01:00" ou "2025-11-26" pour toute la journée)
 * @property endDateTime Date/heure de fin ISO
 */
data class Event(
    val id: String,
    val title: String,
    val date: String = "",
    val startTime: String,
    val endTime: String,
    val description: String = "",
    val location: String = "",
    val source: String = "Google Calendar",
    val startDateTime: String = "",
    val endDateTime: String = ""
)
