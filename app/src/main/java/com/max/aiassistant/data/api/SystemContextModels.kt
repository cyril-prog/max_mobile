package com.max.aiassistant.data.api

import com.google.gson.annotations.SerializedName

/**
 * Modèles de données pour le contexte système du voice-to-voice
 *
 * Ce contexte enrichit le prompt système avec :
 * - Les tâches en cours de l'utilisateur (via /webhook/get_tasks)
 * - La mémoire (via /webhook/get_memory)
 * - L'historique récent des messages (via /webhook/get_recent_messages)
 * - Les événements du calendrier (via /webhook/get_calendar)
 */

/**
 * Réponse de l'API (tableau contenant un seul élément)
 */
data class SystemContextApiResponse(
    val tasks: List<SystemContextTask>,
    val memory: SystemMemory,
    @SerializedName("last_messages")
    val lastMessages: List<LastMessage>
)

/**
 * Tâche dans le contexte système
 */
data class SystemContextTask(
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
 * Mémoire du système (informations sur l'utilisateur et le contexte)
 */
data class SystemMemory(
    @SerializedName("Interets")
    val interets: Map<String, Any>? = null,

    @SerializedName("Materiel")
    val materiel: Map<String, Any>? = null,

    @SerializedName("Personnalite")
    val personnalite: Map<String, Any>? = null,

    @SerializedName("Autonomie_Max")
    val autonomieMax: Map<String, Any>? = null,

    @SerializedName("Certifications")
    val certifications: Map<String, Any>? = null,

    @SerializedName("Projets_En_Cours")
    val projetsEnCours: Map<String, Any>? = null,

    @SerializedName("Interet_Modeles_IA")
    val interetModelesIA: Map<String, Any>? = null,

    @SerializedName("Objectif_Amelioration")
    val objectifAmelioration: Map<String, Any>? = null,

    @SerializedName("Methodes_Apprentissage")
    val methodesApprentissage: Map<String, Any>? = null,

    @SerializedName("Rappels_et_Notifications")
    val rappelsEtNotifications: Map<String, Any>? = null
)

/**
 * Message dans l'historique récent
 */
data class LastMessage(
    val id: Int,
    @SerializedName("session_id")
    val sessionId: String,
    val message: ConversationMessageContent
)

/**
 * Contenu d'un message de conversation
 */
data class ConversationMessageContent(
    val type: String,  // "human" ou "ai"
    val content: String,
    @SerializedName("tool_calls")
    val toolCalls: List<Any>? = null,
    @SerializedName("additional_kwargs")
    val additionalKwargs: Map<String, Any>? = null,
    @SerializedName("response_metadata")
    val responseMetadata: Map<String, Any>? = null,
    @SerializedName("invalid_tool_calls")
    val invalidToolCalls: List<Any>? = null
)

/**
 * Réponse de l'API /webhook/get_memory
 * Retourne la mémoire long terme de l'utilisateur
 * Format: [{id: 1, content: {...}}]
 */
data class MemoryItem(
    val id: Int,
    val content: SystemMemory
)

/**
 * Convertit le contexte système en prompt texte enrichi
 */
fun SystemContextApiResponse.toEnrichedPrompt(): String {
    val builder = StringBuilder()

    // Ajouter les informations de mémoire
    builder.append("\n\n=== CONTEXTE UTILISATEUR ===\n")

    if (memory.interets != null) {
        builder.append("\nINTERÊTS :\n")
        formatMemorySection(memory.interets, builder)
    }

    if (memory.materiel != null) {
        builder.append("\nMATÉRIEL :\n")
        formatMemorySection(memory.materiel, builder)
    }

    if (memory.personnalite != null) {
        builder.append("\nPERSONNALITÉ :\n")
        formatMemorySection(memory.personnalite, builder)
    }

    if (memory.autonomieMax != null) {
        builder.append("\nAUTONOMIE DE MAX :\n")
        formatMemorySection(memory.autonomieMax, builder)
    }

    if (memory.certifications != null) {
        builder.append("\nCERTIFICATIONS :\n")
        formatMemorySection(memory.certifications, builder)
    }

    if (memory.projetsEnCours != null) {
        builder.append("\nPROJETS EN COURS :\n")
        formatMemorySection(memory.projetsEnCours, builder)
    }

    if (memory.interetModelesIA != null) {
        builder.append("\nINTÉRÊT MODÈLES IA :\n")
        formatMemorySection(memory.interetModelesIA, builder)
    }

    if (memory.objectifAmelioration != null) {
        builder.append("\nOBJECTIFS D'AMÉLIORATION :\n")
        formatMemorySection(memory.objectifAmelioration, builder)
    }

    if (memory.methodesApprentissage != null) {
        builder.append("\nMÉTHODES D'APPRENTISSAGE :\n")
        formatMemorySection(memory.methodesApprentissage, builder)
    }

    if (memory.rappelsEtNotifications != null) {
        builder.append("\nRAPPELS ET NOTIFICATIONS :\n")
        formatMemorySection(memory.rappelsEtNotifications, builder)
    }

    // Ajouter les tâches
    if (tasks.isNotEmpty()) {
        builder.append("\nTÂCHES EN COURS (${tasks.size}) :\n")
        tasks.forEach { task ->
            builder.append("- [${task.statut}] ${task.titre}")
            if (task.priorite.isNotEmpty() && task.priorite.lowercase() != "normale") {
                builder.append(" (${task.priorite})")
            }
            if (task.dateLimite.isNotEmpty()) {
                builder.append(" - Échéance: ${task.dateLimite.split("T")[0]}")
            }
            builder.append("\n")
            if (task.description.isNotEmpty()) {
                builder.append("  Description: ${task.description}\n")
            }
        }
    }

    // Ajouter un résumé de l'historique récent (derniers messages)
    if (lastMessages.isNotEmpty()) {
        builder.append("\nCONTEXTE RÉCENT DE CONVERSATION :\n")
        // Prendre les 5 derniers messages pour ne pas surcharger
        lastMessages.takeLast(5).forEach { msg ->
            val type = if (msg.message.type == "human") "Utilisateur" else "Max"
            val content = msg.message.content.take(100) // Limiter à 100 caractères
            builder.append("- $type: $content\n")
        }
    }

    return builder.toString()
}

/**
 * Formatte une section de mémoire (récursif pour les maps imbriquées)
 */
private fun formatMemorySection(section: Map<String, Any>, builder: StringBuilder, indent: String = "") {
    section.forEach { (key, value) ->
        when (value) {
            is Map<*, *> -> {
                builder.append("$indent- $key:\n")
                @Suppress("UNCHECKED_CAST")
                formatMemorySection(value as Map<String, Any>, builder, "$indent  ")
            }
            else -> {
                builder.append("$indent- $key: $value\n")
            }
        }
    }
}
