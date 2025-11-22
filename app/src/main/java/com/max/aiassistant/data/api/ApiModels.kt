package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName
import com.max.aiassistant.model.Task
import com.max.aiassistant.model.TaskPriority
import com.max.aiassistant.model.TaskStatus

/**
 * Modèles de données pour l'API Webhook Max Mobile
 */

// ========== TÂCHES (GET /webhook/get_tasks) ==========

/**
 * Réponse de l'API pour la liste des tâches
 */
data class TasksApiResponse(
    val text: TasksData
)

data class TasksData(
    val data: List<TaskApiData>
)

/**
 * Représentation d'une tâche telle que retournée par l'API
 */
data class TaskApiData(
    val id: Int,
    val titre: String,
    val description: String,
    val note: String,
    val statut: String,
    val priorite: String,
    @SerializedName("date_limite")
    val dateLimite: String,
    @SerializedName("date_fin")
    val dateFin: String,
    val categorie: String,
    @SerializedName("id_parent")
    val idParent: String,
    val recurrence: String,
    @SerializedName("duree_estimee")
    val dureeEstimee: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Convertit une TaskApiData en Task pour l'affichage dans l'app
 */
fun TaskApiData.toTask(): Task {
    return Task(
        id = id.toString(),
        title = titre,
        description = description,
        note = note,
        status = when (statut.lowercase()) {
            "à faire" -> TaskStatus.TODO
            "en cours" -> TaskStatus.IN_PROGRESS
            "terminé", "terminée" -> TaskStatus.COMPLETED
            else -> TaskStatus.TODO
        },
        priority = when (priorite.lowercase()) {
            "haute", "urgente" -> TaskPriority.URGENT
            "basse", "faible" -> TaskPriority.LOW
            else -> TaskPriority.NORMAL
        },
        deadline = formatDeadline(dateLimite),
        category = categorie,
        estimatedDuration = dureeEstimee
    )
}

/**
 * Formate la date limite pour un affichage simplifié
 */
private fun formatDeadline(isoDate: String): String {
    if (isoDate.isEmpty()) return "Aucune échéance"

    // Parse ISO date (ex: "2025-11-23T20:00:00")
    try {
        val parts = isoDate.split("T")[0].split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        val today = java.util.Calendar.getInstance()
        val deadline = java.util.Calendar.getInstance().apply {
            set(year, month - 1, day)
        }

        val diffDays = ((deadline.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> "En retard"
            diffDays == 0 -> "Aujourd'hui"
            diffDays == 1 -> "Demain"
            diffDays <= 7 -> "Dans $diffDays jours"
            else -> "$day/${String.format("%02d", month)}/$year"
        }
    } catch (e: Exception) {
        return isoDate.split("T")[0]
    }
}

// ========== ÉVÉNEMENTS CALENDRIER (GET /webhook/get_calendar) ==========

/**
 * Réponse de l'API pour les événements du calendrier
 */
data class CalendarApiResponse(
    val text: CalendarData
)

data class CalendarData(
    val data: List<EventApiData>
)

/**
 * Représentation d'un événement tel que retourné par l'API Google Calendar
 */
data class EventApiData(
    val id: String,
    val summary: String,
    val start: EventDateTime,
    val end: EventDateTime,
    val description: String? = null,
    val location: String? = null,
    val status: String? = null
)

data class EventDateTime(
    val dateTime: String? = null,  // Pour les événements avec heure
    val date: String? = null,       // Pour les événements "toute la journée"
    val timeZone: String? = null
)

/**
 * Convertit un EventApiData en Event pour l'affichage dans l'app
 */
fun EventApiData.toEvent(): com.max.aiassistant.model.Event {
    // Gère les événements avec heure (dateTime) et sans heure (date uniquement)
    val startTimeDisplay = when {
        start.dateTime != null -> formatEventTime(start.dateTime)
        start.date != null -> "Toute la journée"
        else -> "Heure inconnue"
    }

    val endTimeDisplay = when {
        end.dateTime != null -> formatEventTime(end.dateTime)
        end.date != null -> "Toute la journée"
        else -> "Heure inconnue"
    }

    return com.max.aiassistant.model.Event(
        id = id,
        title = summary,
        startTime = startTimeDisplay,
        endTime = endTimeDisplay,
        description = description ?: "",
        location = location ?: "",
        source = "Google Calendar"
    )
}

/**
 * Formate une date/heure ISO en format d'affichage simplifié
 * Ex: "2025-11-26T12:00:00+01:00" -> "12:00"
 */
private fun formatEventTime(isoDateTime: String): String {
    try {
        // Parse ISO datetime (ex: "2025-11-26T12:00:00+01:00")
        val timePart = isoDateTime.split("T")[1].split("+")[0].split("-")[0]
        val hours = timePart.split(":")[0].toInt()
        val minutes = timePart.split(":")[1]

        // Convertir en format 12h avec AM/PM
        val period = if (hours >= 12) "PM" else "AM"
        val displayHours = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }

        return "$displayHours:$minutes $period"
    } catch (e: Exception) {
        return isoDateTime.take(5) // Fallback basique
    }
}

// ========== REQUÊTE (Envoi de message) ==========

data class WebhookRequest(
    val time: Long,
    val id: String,
    val messaging: List<MessagingRequest>
)

data class MessagingRequest(
    val sender: Sender,
    val recipient: Recipient,
    val timestamp: Long,
    val message: MessageRequest
)

data class Sender(
    val id: String
)

data class Recipient(
    val id: String
)

data class MessageRequest(
    val mid: String,
    val text: String
)

// ========== RÉPONSE (Réception de message) ==========

/**
 * Réponse simplifiée de l'API (juste le texte)
 * Le champ text est nullable pour gérer les réponses vides du webhook
 */
data class WebhookResponse(
    val text: String? = null
)

// ========== HELPER FUNCTIONS ==========

/**
 * Parse une réponse brute du webhook en WebhookResponse
 * Gère les cas de corps vide, texte brut, et JSON
 */
fun parseWebhookResponse(responseBody: String?): WebhookResponse {
    if (responseBody.isNullOrBlank()) {
        return WebhookResponse(text = null)
    }

    return try {
        // Essaie de parser comme JSON
        com.google.gson.Gson().fromJson(responseBody, WebhookResponse::class.java)
    } catch (e: Exception) {
        // Si le parsing JSON échoue, traite comme du texte brut
        WebhookResponse(text = responseBody)
    }
}

/**
 * Génère un ID unique pour les messages
 */
fun generateMessageId(): String {
    return System.currentTimeMillis().toString() + (1000..9999).random()
}

/**
 * Crée une requête webhook à partir d'un texte
 */
fun createWebhookRequest(text: String): List<WebhookRequest> {
    val timestamp = System.currentTimeMillis()
    return listOf(
        WebhookRequest(
            time = timestamp,
            id = "840982269101938",
            messaging = listOf(
                MessagingRequest(
                    sender = Sender(id = "24886428771021238"),
                    recipient = Recipient(id = "840982269101938"),
                    timestamp = timestamp,
                    message = MessageRequest(
                        mid = "m_${generateMessageId()}",
                        text = text
                    )
                )
            )
        )
    )
}