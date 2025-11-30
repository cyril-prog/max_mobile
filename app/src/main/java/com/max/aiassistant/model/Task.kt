package com.max.aiassistant.model

/**
 * Représente une tâche dans la liste TO DO
 *
 * @property id Identifiant unique
 * @property title Titre de la tâche
 * @property description Description détaillée
 * @property note Notes additionnelles
 * @property status Statut actuel (TODO, IN_PROGRESS, COMPLETED)
 * @property priority Niveau de priorité (URGENT, NORMAL, LOW)
 * @property deadline Deadline (format texte simplifié, ex: "Today", "Tomorrow")
 * @property deadlineDate Date ISO de l'échéance (ex: "2025-11-30")
 * @property category Catégorie de la tâche (ex: "Travail", "Personnel")
 * @property estimatedDuration Durée estimée pour compléter la tâche
 */
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val note: String = "",
    val status: TaskStatus,
    val priority: TaskPriority,
    val deadline: String,
    val deadlineDate: String = "",
    val category: String = "",
    val estimatedDuration: String = ""
)

/**
 * États possibles d'une tâche
 */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED
}

/**
 * Niveaux de priorité
 * Détermine la couleur affichée dans l'interface
 */
enum class TaskPriority {
    P1,    // Priorité la plus haute - Rouge
    P2,    // Haute - Orange foncé
    P3,    // Moyenne - Orange
    P4,    // Basse - Jaune
    P5     // Très basse - Vert
}

/**
 * Représente une sous-tâche
 */
data class SubTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false
)
