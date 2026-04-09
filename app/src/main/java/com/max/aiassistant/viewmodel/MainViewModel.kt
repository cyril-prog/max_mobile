package com.max.aiassistant.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.max.aiassistant.BuildConfig
import com.max.aiassistant.data.api.MaxApiService
import com.max.aiassistant.data.api.MessageContent
import com.max.aiassistant.data.api.MessageData
import com.max.aiassistant.data.api.parseWebhookResponse
import com.max.aiassistant.data.api.toEvent
import com.max.aiassistant.data.api.toActuArticle
import com.max.aiassistant.data.api.toCurrentPollen
import com.max.aiassistant.data.api.toRechercheArticle
import com.max.aiassistant.data.api.toTask
import com.max.aiassistant.data.api.toUpdateRequest
import com.max.aiassistant.data.api.toWeatherData
import com.max.aiassistant.data.realtime.RealtimeApiService
import com.max.aiassistant.data.realtime.RealtimeAudioManager
import com.max.aiassistant.data.realtime.RealtimeServerEvent
import com.max.aiassistant.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * ViewModel principal de l'application Max
 *
 * Gère l'état de toute l'application :
 * - Messages du chat
 * - Liste des tâches
 * - Événements du planning
 * - État du mode voice
 *
 * Utilise StateFlow pour exposer l'état de manière réactive aux composables
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ========== SERVICES API ==========

    private val apiService = MaxApiService.create()
    private val weatherApiService = com.max.aiassistant.data.api.WeatherApiService.create()
    private val pollenApiService = com.max.aiassistant.data.api.PollenApiService.create()
    private val geocodingApiService = com.max.aiassistant.data.api.GeocodingApiService.create()
    private val weatherPreferences = com.max.aiassistant.data.preferences.WeatherPreferences(application.applicationContext)
    private val notesPreferences = com.max.aiassistant.data.preferences.NotesPreferences(application.applicationContext)
    private val TAG = "MainViewModel"

    // Clé API OpenAI pour l'API Realtime (chargée depuis local.properties via BuildConfig)
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY

    // Service Realtime (initialisé paresseusement)
    private var realtimeService: RealtimeApiService? = null
    private var audioManager: RealtimeAudioManager? = null

    // Jobs des collecteurs Realtime — annulés à chaque déconnexion pour éviter les doublons
    private var realtimeEventsJob: Job? = null
    private var realtimeErrorsJob: Job? = null
    private var realtimeConnectionJob: Job? = null

    // Contexte système pour enrichir le prompt du voice-to-voice
    private var systemContext: String = ""

    // ========== ÉTAT DU CHAT ==========

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Indicateur de chargement pour la réponse de l'IA
    private val _isWaitingForAiResponse = MutableStateFlow(false)
    val isWaitingForAiResponse: StateFlow<Boolean> = _isWaitingForAiResponse.asStateFlow()

    /**
     * Charge le contexte système (tâches, mémoire, historique, calendrier) pour enrichir le prompt voice-to-voice
     * Récupère les données depuis 4 endpoints différents de manière robuste
     */
    private fun loadSystemContext() {
        viewModelScope.launch {
            Log.d(TAG, "Chargement du contexte système...")
            val builder = StringBuilder()

            // 1. Récupération des tâches
            try {
                Log.d(TAG, "Récupération des tâches...")
                val tasksResponse = apiService.getTasks()
                val tasks = tasksResponse.text.data

                if (tasks.isNotEmpty()) {
                    builder.append("\n\n=== TÂCHES EN COURS (${tasks.size}) ===\n")
                    tasks.forEach { task ->
                        builder.append("- [${task.statut ?: "À faire"}] ${task.titre}")
                        if (!task.priorite.isNullOrEmpty() && task.priorite.lowercase() != "normale") {
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
                    Log.d(TAG, "✅ ${tasks.size} tâches récupérées")
                } else {
                    Log.d(TAG, "Aucune tâche trouvée")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la récupération des tâches (ignorée)", e)
            }

            // 2. Récupération des événements du calendrier
            try {
                Log.d(TAG, "Récupération des événements du calendrier...")
                val eventsResponse = apiService.getCalendarEvents()
                val events = eventsResponse.text.data

                if (events.isNotEmpty()) {
                    builder.append("\n=== ÉVÉNEMENTS DU CALENDRIER (${events.size}) ===\n")
                    events.forEach { event ->
                        builder.append("- ${event.summary}")
                        val startTime = event.start.dateTime ?: event.start.date ?: "?"
                        builder.append(" - ${startTime.split("T")[0]}")
                        if (event.location?.isNotEmpty() == true) {
                            builder.append(" (${event.location})")
                        }
                        builder.append("\n")
                    }
                    Log.d(TAG, "✅ ${events.size} événements récupérés")
                } else {
                    Log.d(TAG, "Aucun événement trouvé")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la récupération des événements (ignorée)", e)
            }

            // 3. Récupération des messages récents
            try {
                Log.d(TAG, "Récupération des messages récents...")
                val messagesResponse = apiService.getRecentMessages()
                val messages = messagesResponse.text.data

                if (messages.isNotEmpty()) {
                    builder.append("\n=== CONTEXTE RÉCENT DE CONVERSATION ===\n")
                    // Prendre les 5 derniers messages pour ne pas surcharger
                    messages.takeLast(5).forEach { msg ->
                        val type = if (msg.message.type == "human") "Utilisateur" else "Max"
                        val content = msg.message.content.take(100) // Limiter à 100 caractères
                        builder.append("- $type: $content\n")
                    }
                    Log.d(TAG, "✅ ${messages.size} messages récupérés (${messages.takeLast(5).size} affichés)")
                } else {
                    Log.d(TAG, "Aucun message récent trouvé")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la récupération des messages récents (ignorée)", e)
            }

            // 4. Récupération de la mémoire long terme
            try {
                Log.d(TAG, "Récupération de la mémoire long terme...")
                val memoryList = apiService.getMemory()

                if (memoryList.isNotEmpty()) {
                    val memory = memoryList[0].content
                    builder.append("\n=== CONTEXTE UTILISATEUR (MÉMOIRE) ===\n")

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

                    Log.d(TAG, "✅ Mémoire long terme récupérée")
                } else {
                    Log.d(TAG, "Aucune mémoire trouvée")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la récupération de la mémoire (ignorée)", e)
            }

            // Stocke le contexte final
            systemContext = builder.toString()
            Log.d(TAG, "✅ Contexte système complet chargé (${systemContext.length} caractères)")
            Log.d(TAG, "Aperçu du contexte: ${systemContext.take(300)}...")
        }
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

    /**
     * Charge les messages récents depuis l'API
     * Peut être appelée au démarrage ou quand on arrive sur l'écran du chat
     */
    fun loadRecentMessages() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Chargement des messages récents...")

                val response = apiService.getRecentMessages()

                // Extrait la liste de messages depuis response.text.data
                val apiMessages = response.text.data
                Log.d(TAG, "Nombre de messages dans text.data = ${apiMessages.size}")

                if (apiMessages.isEmpty()) {
                    Log.w(TAG, "Aucun message dans text.data")
                    return@launch
                }

                // Convertit les messages API en objets Message de l'app
                // Trie par ID croissant : les plus anciens en haut, les plus récents en bas
                val messages = apiMessages
                    .sortedBy { it.id }
                    .map { apiMessage ->
                        Log.d(TAG, "Mapping message ${apiMessage.id}: type=${apiMessage.message.type}, content=${apiMessage.message.content.take(50)}...")
                        Message(
                            id = apiMessage.id.toString(),
                            content = apiMessage.message.content,
                            isFromUser = apiMessage.message.type == "human"
                        )
                    }

                _messages.value = messages
                Log.d(TAG, "Messages récents chargés et affichés: ${messages.size}")
                Log.d(TAG, "Premier message: ${messages.firstOrNull()?.content?.take(50)}")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement des messages récents", e)
                Log.e(TAG, "Stack trace:", e)
                // En cas d'erreur, on garde la liste vide (pas de messages)
            }
        }
    }

    /**
     * Envoie un message à Max via l'API webhook
     * @param content Le texte du message
     * @param imageUri L'URI de l'image attachée (optionnel)
     */
    fun sendMessage(content: String, imageUri: Uri? = null) {
        if (content.isBlank() && imageUri == null) return

        // Ajoute le message de l'utilisateur immédiatement
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = content,
            isFromUser = true,
            imageUri = imageUri
        )
        _messages.value = _messages.value + userMessage

        // Active l'indicateur de chargement
        _isWaitingForAiResponse.value = true

        // Envoie le message à l'API et récupère la réponse
        viewModelScope.launch {
            try {
                Log.d(TAG, "Envoi du message: $content, avec image: ${imageUri != null}")

                // Prépare la requête avec ou sans image
                val base64Image = if (imageUri != null) {
                    withContext(Dispatchers.IO) {
                        convertImageToBase64(imageUri)
                    }.also { 
                        Log.d(TAG, "Image convertie en Base64, taille: ${it?.length ?: 0} caractères")
                    }
                } else null
                
                val request = com.max.aiassistant.data.api.ChatMessageRequest(
                    text = content.ifBlank { if (base64Image != null) "Analyse cette image" else "" },
                    image = base64Image
                )
                
                val httpResponse = apiService.sendMessage(request)

                // Vérifie que la requête a réussi
                if (!httpResponse.isSuccessful) {
                    Log.e(TAG, "Erreur HTTP: ${httpResponse.code()}")
                    throw Exception("Erreur HTTP: ${httpResponse.code()}")
                }

                // Récupère le corps de la réponse
                val rawBody = httpResponse.body()?.string()
                Log.d(TAG, "Corps de la réponse brut: ${rawBody ?: "(vide)"}")

                // Parse la réponse (gère corps vide, JSON et texte brut)
                val webhookResponse = parseWebhookResponse(rawBody)
                Log.d(TAG, "Réponse parsée: text=${webhookResponse.text}")

                // Vérifie si la réponse contient du texte
                val responseText = webhookResponse.text
                if (responseText.isNullOrBlank()) {
                    Log.w(TAG, "Réponse vide du webhook - le workflow n8n ne retourne pas de données")

                    val errorMessage = Message(
                        id = UUID.randomUUID().toString(),
                        content = "Le serveur a reçu votre message mais n'a pas renvoyé de réponse. Veuillez vérifier la configuration du webhook n8n.",
                        isFromUser = false
                    )
                    _messages.value = _messages.value + errorMessage
                    return@launch
                }

                // Ajoute la réponse de Max
                val aiResponse = Message(
                    id = UUID.randomUUID().toString(),
                    content = responseText,
                    isFromUser = false
                )
                _messages.value = _messages.value + aiResponse

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'envoi du message", e)

                // Message d'erreur pour l'utilisateur
                val errorMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = "Désolé, une erreur s'est produite. Veuillez réessayer.",
                    isFromUser = false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                // Désactive l'indicateur de chargement
                _isWaitingForAiResponse.value = false
            }
        }
    }
    
    /**
     * Convertit une image URI en chaîne Base64
     */
    private fun convertImageToBase64(uri: Uri): String? {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            bytes?.let {
                Base64.encodeToString(it, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la conversion de l'image en Base64", e)
            null
        }
    }

    // ========== ÉTAT DES TÂCHES ==========

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoadingTasks = MutableStateFlow(false)
    val isLoadingTasks: StateFlow<Boolean> = _isLoadingTasks.asStateFlow()

    /**
     * Récupère les tâches depuis l'API
     */
    fun refreshTasks() {
        viewModelScope.launch {
            try {
                _isLoadingTasks.value = true
                Log.d(TAG, "Récupération des tâches...")

                val response = apiService.getTasks()
                val tasks = response.text.data.map { it.toTask() }

                _tasks.value = tasks
                Log.d(TAG, "Tâches récupérées: ${tasks.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des tâches", e)
                // En cas d'erreur, on garde les tâches actuelles
            } finally {
                _isLoadingTasks.value = false
            }
        }
    }

    /**
     * Met à jour le statut d'une tâche
     * Note: Le statut n'est pas synchronisé via upd_task car non inclus dans les champs
     */
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(status = newStatus)
            } else {
                task
            }
        }
    }

    /**
     * Met à jour la priorité d'une tâche
     */
    fun updateTaskPriority(taskId: String, newPriority: TaskPriority) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(priority = newPriority)
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Met à jour la durée estimée d'une tâche
     */
    fun updateTaskDuration(taskId: String, newDuration: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(estimatedDuration = newDuration)
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Met à jour la date d'échéance d'une tâche
     * @param taskId ID de la tâche
     * @param newDeadlineDate Date au format ISO (ex: "2025-12-15")
     */
    fun updateTaskDeadline(taskId: String, newDeadlineDate: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    deadlineDate = newDeadlineDate,
                    deadline = com.max.aiassistant.data.api.formatDeadline(newDeadlineDate)
                )
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Met à jour la catégorie d'une tâche
     */
    fun updateTaskCategory(taskId: String, newCategory: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(category = newCategory)
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Met à jour le titre d'une tâche
     */
    fun updateTaskTitle(taskId: String, newTitle: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(title = newTitle)
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Met à jour la description d'une tâche
     */
    fun updateTaskDescription(taskId: String, newDescription: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(description = newDescription)
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Met à jour les notes d'une tâche
     */
    fun updateTaskNote(taskId: String, newNote: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(note = newNote)
            } else {
                task
            }
        }
        syncTaskToApi(taskId)
    }

    /**
     * Synchronise une tâche avec l'API après modification
     */
    private fun syncTaskToApi(taskId: String) {
        val task = _tasks.value.find { it.id == taskId }
        if (task == null) {
            Log.e(TAG, "syncTaskToApi: Tâche $taskId non trouvée")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Synchronisation de la tâche $taskId avec l'API...")
                val request = task.toUpdateRequest()
                Log.d(TAG, "Requête: id=${request.id}, titre=${request.titre}, categorie=${request.categorie}, priorite=${request.priorite}")
                val response = apiService.updateTask(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Tâche synchronisée avec succès")
                } else {
                    Log.e(TAG, "Erreur lors de la synchronisation: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation de la tâche", e)
            }
        }
    }

    /**
     * Supprime une tâche via l'API et met à jour la liste locale
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Suppression de la tâche $taskId...")

                // Appel de l'API pour supprimer la tâche
                val response = apiService.deleteTask(taskId)

                if (response.isSuccessful) {
                    // Suppression réussie, on retire la tâche de la liste locale
                    _tasks.value = _tasks.value.filter { it.id != taskId }
                    Log.d(TAG, "Tâche supprimée avec succès")
                } else {
                    Log.e(TAG, "Erreur lors de la suppression de la tâche: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression de la tâche", e)
            }
        }
    }

    /**
     * Crée une nouvelle tâche via l'API et rafraîchit la liste
     */
    fun createTask(
        titre: String,
        categorie: String,
        description: String = "",
        note: String = "",
        priorite: TaskPriority = TaskPriority.P3,
        dateLimite: String = "",
        dureeEstimee: String = ""
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Création de la tâche: $titre...")

                val request = com.max.aiassistant.data.api.TaskCreateRequest(
                    titre = titre,
                    statut = "À faire",
                    categorie = categorie,
                    description = description,
                    note = note,
                    priorite = when (priorite) {
                        TaskPriority.P1 -> "P1"
                        TaskPriority.P2 -> "P2"
                        TaskPriority.P3 -> "P3"
                        TaskPriority.P4 -> "P4"
                        TaskPriority.P5 -> "P5"
                    },
                    dateLimite = dateLimite,
                    dureeEstimee = dureeEstimee
                )

                val response = apiService.createTask(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Tâche créée avec succès, rafraîchissement de la liste...")
                    // Rafraîchir la liste des tâches pour récupérer la nouvelle tâche avec son ID
                    refreshTasks()
                } else {
                    Log.e(TAG, "Erreur lors de la création de la tâche: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la tâche", e)
            }
        }
    }

    // ========== GESTION DES SOUS-TÂCHES ==========

    /**
     * Crée une nouvelle sous-tâche via l'API
     */
    fun createSubTask(taskId: String, text: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Création de la sous-tâche pour la tâche $taskId...")

                // Mise à jour optimiste : ajouter temporairement la sous-tâche (sans ID)
                _tasks.value = _tasks.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            subTasks = task.subTasks + com.max.aiassistant.model.SubTask(
                                id = "temp_${System.currentTimeMillis()}",
                                taskId = taskId,
                                text = text,
                                isCompleted = false
                            )
                        )
                    } else {
                        task
                    }
                }

                val request = com.max.aiassistant.data.api.SubTaskCreateRequest(
                    taskId = taskId,
                    text = text,
                    isCompleted = false
                )

                val response = apiService.createSubTask(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Sous-tâche créée avec succès, rafraîchissement pour obtenir l'ID...")
                    refreshTasks() // Pour obtenir le vrai ID de la sous-tâche
                } else {
                    Log.e(TAG, "Erreur lors de la création de la sous-tâche: ${response.code()}")
                    refreshTasks() // Reverter la mise à jour optimiste
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la sous-tâche", e)
                refreshTasks()
            }
        }
    }

    /**
     * Met à jour une sous-tâche via l'API (texte ou statut)
     */
    fun updateSubTask(subTaskId: String, text: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Mise à jour de la sous-tâche $subTaskId...")

                // Mise à jour optimiste de la liste locale immédiatement
                _tasks.value = _tasks.value.map { task ->
                    task.copy(
                        subTasks = task.subTasks.map { subTask ->
                            if (subTask.id == subTaskId) {
                                subTask.copy(text = text, isCompleted = isCompleted)
                            } else {
                                subTask
                            }
                        }
                    )
                }

                val request = com.max.aiassistant.data.api.SubTaskUpdateRequest(
                    id = subTaskId,
                    text = text,
                    isCompleted = isCompleted
                )

                val response = apiService.updateSubTask(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Sous-tâche mise à jour avec succès")
                    // Pas besoin de refreshTasks(), la mise à jour optimiste suffit
                } else {
                    Log.e(TAG, "Erreur lors de la mise à jour de la sous-tâche: ${response.code()}")
                    // En cas d'erreur, on pourrait reverter la mise à jour optimiste
                    refreshTasks()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la mise à jour de la sous-tâche", e)
                // En cas d'erreur, rafraîchir pour avoir l'état réel
                refreshTasks()
            }
        }
    }

    /**
     * Supprime une sous-tâche via l'API
     */
    fun deleteSubTask(subTaskId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Suppression de la sous-tâche $subTaskId...")

                // Mise à jour optimiste de la liste locale immédiatement
                _tasks.value = _tasks.value.map { task ->
                    task.copy(
                        subTasks = task.subTasks.filter { it.id != subTaskId }
                    )
                }

                val request = com.max.aiassistant.data.api.SubTaskDeleteRequest(id = subTaskId)

                val response = apiService.deleteSubTask(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Sous-tâche supprimée avec succès")
                } else {
                    Log.e(TAG, "Erreur lors de la suppression de la sous-tâche: ${response.code()}")
                    refreshTasks()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression de la sous-tâche", e)
                refreshTasks()
            }
        }
    }

    // ========== ÉTAT DU PLANNING ==========

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()

    // ========== ÉTAT DE LA MÉTÉO ==========

    private val _weatherData = MutableStateFlow<com.max.aiassistant.data.api.WeatherData?>(null)
    val weatherData: StateFlow<com.max.aiassistant.data.api.WeatherData?> = _weatherData.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()

    // État de la ville actuelle
    private val _cityName = MutableStateFlow(com.max.aiassistant.data.preferences.WeatherPreferences.DEFAULT_CITY_NAME)
    val cityName: StateFlow<String> = _cityName.asStateFlow()

    private val _cityLatitude = MutableStateFlow(com.max.aiassistant.data.preferences.WeatherPreferences.DEFAULT_LATITUDE)
    val cityLatitude: StateFlow<Double> = _cityLatitude.asStateFlow()

    private val _cityLongitude = MutableStateFlow(com.max.aiassistant.data.preferences.WeatherPreferences.DEFAULT_LONGITUDE)
    val cityLongitude: StateFlow<Double> = _cityLongitude.asStateFlow()

    private val _showAllergies = MutableStateFlow(com.max.aiassistant.data.preferences.WeatherPreferences.DEFAULT_SHOW_ALLERGIES)
    val showAllergies: StateFlow<Boolean> = _showAllergies.asStateFlow()

    // Résultats de recherche de ville
    private val _citySearchResults = MutableStateFlow<List<com.max.aiassistant.data.api.CityResult>>(emptyList())
    val citySearchResults: StateFlow<List<com.max.aiassistant.data.api.CityResult>> = _citySearchResults.asStateFlow()

    // ========== ÉTAT DES NOTES ==========

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    /**
     * Ajoute une nouvelle note
     */
    fun addNote(title: String, content: String) {
        val newNote = Note(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        _notes.value = _notes.value + newNote
        notesPreferences.saveNotes(_notes.value)
        Log.d(TAG, "Note ajoutée: $title")
    }

    /**
     * Met à jour une note existante
     */
    fun updateNote(noteId: String, title: String, content: String) {
        _notes.value = _notes.value.map { note ->
            if (note.id == noteId) {
                note.copy(title = title, content = content)
            } else {
                note
            }
        }
        notesPreferences.saveNotes(_notes.value)
        Log.d(TAG, "Note mise à jour: $noteId")
    }

    /**
     * Supprime une note
     */
    fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filter { it.id != noteId }
        notesPreferences.saveNotes(_notes.value)
        Log.d(TAG, "Note supprimée: $noteId")
    }

    // ========== ÉTAT DES ACTUALITÉS ==========

    private val _actuArticles = MutableStateFlow<List<com.max.aiassistant.data.api.ActuArticle>>(emptyList())
    val actuArticles: StateFlow<List<com.max.aiassistant.data.api.ActuArticle>> = _actuArticles.asStateFlow()

    private val _rechercheArticles = MutableStateFlow<List<com.max.aiassistant.data.api.RechercheArticle>>(emptyList())
    val rechercheArticles: StateFlow<List<com.max.aiassistant.data.api.RechercheArticle>> = _rechercheArticles.asStateFlow()

    private val _isLoadingActu = MutableStateFlow(false)
    val isLoadingActu: StateFlow<Boolean> = _isLoadingActu.asStateFlow()

    /**
     * Récupère les actualités et les recherches IA depuis l'API N8N
     */
    fun refreshActu() {
        viewModelScope.launch {
            try {
                _isLoadingActu.value = true
                Log.d(TAG, "Récupération des actualités...")
                val response = apiService.getActu()
                _actuArticles.value = response.response?.actu
                    ?.mapNotNull { it.toActuArticle() }
                    ?.sortedByDescending { it.score } ?: emptyList()
                _rechercheArticles.value = response.response?.recherche
                    ?.mapNotNull { it.toRechercheArticle() } ?: emptyList()
                Log.d(TAG, "Actualités récupérées: ${_actuArticles.value.size} actu, ${_rechercheArticles.value.size} recherches")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des actualités", e)
            } finally {
                _isLoadingActu.value = false
            }
        }
    }

    /**
     * Recherche des villes par nom
     */
    fun searchCity(cityName: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Recherche de ville: $cityName")
                val response = geocodingApiService.searchCity(cityName)
                _citySearchResults.value = response.results ?: emptyList()
                Log.d(TAG, "Résultats de recherche: ${_citySearchResults.value.size} villes trouvées")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la recherche de ville", e)
                _citySearchResults.value = emptyList()
            }
        }
    }

    /**
     * Change la ville actuelle et recharge les données météo
     */
    fun selectCity(city: com.max.aiassistant.data.api.CityResult) {
        viewModelScope.launch {
            _cityName.value = city.name
            _cityLatitude.value = city.latitude
            _cityLongitude.value = city.longitude
            Log.d(TAG, "Ville sélectionnée: ${city.name} (${city.latitude}, ${city.longitude})")

            weatherPreferences.saveCity(city.name, city.latitude, city.longitude)
            refreshWeather()
        }
    }

    /**
     * Modifie la préférence d'affichage du bloc allergies
     */
    fun setShowAllergies(show: Boolean) {
        viewModelScope.launch {
            _showAllergies.value = show
            weatherPreferences.setShowAllergies(show)
            Log.d(TAG, "Affichage des allergies: $show")
        }
    }

    /**
     * Récupère les données météo depuis Open-Meteo
     */
    fun refreshWeather() {
        viewModelScope.launch {
            try {
                _isLoadingWeather.value = true
                Log.d(TAG, "Récupération des données météo pour ${_cityName.value}...")

                val latitude = _cityLatitude.value
                val longitude = _cityLongitude.value
                val response = weatherApiService.getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude
                )
                val pollenData = try {
                    pollenApiService.getPollenForecast(
                        latitude = latitude,
                        longitude = longitude
                    ).toCurrentPollen()
                } catch (e: Exception) {
                    Log.w(TAG, "Impossible de recuperer les pollens depuis l'API air-quality", e)
                    null
                }
                val weatherData = response.toWeatherData(pollenData)

                _weatherData.value = weatherData
                Log.d(TAG, "Données météo récupérées: ${weatherData.currentTemperature}°C, ${weatherData.hourlyForecasts.size} prévisions horaires")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération de la météo", e)
                // En cas d'erreur, on garde les données actuelles
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    /**
     * Récupère les événements du calendrier depuis l'API
     */
    fun refreshCalendarEvents() {
        viewModelScope.launch {
            try {
                _isLoadingEvents.value = true
                Log.d(TAG, "Récupération des événements du calendrier...")

                val response = apiService.getCalendarEvents()
                // Trie les événements par date/heure (les plus proches en premier)
                val events = response.text.data
                    .sortedBy { eventApiData ->
                        // Utilise dateTime si disponible, sinon date (pour événements "toute la journée")
                        eventApiData.start.dateTime ?: eventApiData.start.date ?: ""
                    }
                    .map { it.toEvent() }

                _events.value = events
                Log.d(TAG, "Événements récupérés: ${events.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des événements", e)
                // En cas d'erreur, on garde les événements actuels
            } finally {
                _isLoadingEvents.value = false
            }
        }
    }

    // ========== ÉTAT VOICE & REALTIME ==========

    // État de connexion à l'API Realtime
    private val _isRealtimeConnected = MutableStateFlow(false)
    val isRealtimeConnected: StateFlow<Boolean> = _isRealtimeConnected.asStateFlow()

    // Transcription vocale reçue
    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript: StateFlow<String> = _voiceTranscript.asStateFlow()

    // Transcription en temps réel (pendant que l'utilisateur parle)
    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    // ========== STOCKAGE DES CONVERSATIONS VOICE ==========

    // Liste des messages de la conversation en cours (pour envoi à n8n)
    private val conversationMessages = mutableListOf<MessageData>()

    // ID de session unique pour cette conversation
    private var sessionId: String = UUID.randomUUID().toString()

    // Compteur pour générer les IDs de messages
    private var messageIdCounter = 0

    // Dernière transcription utilisateur (en attente de la réponse IA)
    // File d'attente pour éviter qu'une transcription rapide écrase la précédente
    private val pendingUserTranscripts = kotlin.collections.ArrayDeque<String>()

    /**
     * Toggle la connexion à l'API Realtime
     *
     * - Si déconnecté : se connecte au WebSocket et démarre l'enregistrement audio
     * - Si connecté : arrête l'enregistrement et déconnecte
     */
    fun toggleRealtimeConnection() {
        if (_isRealtimeConnected.value) {
            // Déconnexion
            disconnectRealtime()
        } else {
            // Connexion
            connectRealtime()
        }
    }

    /**
     * Connecte à l'API Realtime et démarre l'enregistrement audio
     */
    private fun connectRealtime() {
        Log.d(TAG, "Connexion à l'API Realtime...")

        // Annule les collecteurs de la session précédente (évite les doublons)
        realtimeEventsJob?.cancel()
        realtimeErrorsJob?.cancel()
        realtimeConnectionJob?.cancel()
        realtimeEventsJob = null
        realtimeErrorsJob = null
        realtimeConnectionJob = null

        // Initialise le service Realtime
        realtimeService = RealtimeApiService(OPENAI_API_KEY)

        // Initialise le gestionnaire audio
        audioManager = RealtimeAudioManager(
            context = getApplication(),
            onAudioChunk = { base64Audio ->
                // Callback appelé pour chaque chunk audio capturé
                realtimeService?.sendAudioChunk(base64Audio)
            },
            onSpeakingFinished = {
                // Le playback IA est vraiment terminé (queue vidée) :
                // purge le buffer d'entrée côté serveur pour éliminer ce que le micro
                // a pu capter pendant le playback (écho résiduel avant que isSpeaking soit actif)
                realtimeService?.clearAudioBuffer()
                Log.d(TAG, "Playback IA terminé → input_audio_buffer.clear envoyé")
            }
        )

        // Observe les événements du serveur — Job stocké pour pouvoir l'annuler
        realtimeEventsJob = viewModelScope.launch {
            realtimeService?.serverEvents?.collect { event ->
                handleRealtimeEvent(event)
            }
        }

        // Observe les erreurs — Job stocké
        realtimeErrorsJob = viewModelScope.launch {
            realtimeService?.errors?.collect { error ->
                Log.e(TAG, "Erreur Realtime: $error")
            }
        }

        // Observe l'état de connexion — Job stocké
        // On utilise un flag pour ne démarrer l'enregistrement qu'une seule fois
        var recordingStarted = false
        realtimeConnectionJob = viewModelScope.launch {
            realtimeService?.isConnected?.collect { connected ->
                _isRealtimeConnected.value = connected
                if (connected && !recordingStarted) {
                    recordingStarted = true
                    audioManager?.startRecording()
                    Log.d(TAG, "Connexion Realtime établie, enregistrement démarré")
                }
            }
        }

        // Lance la connexion WebSocket avec le contexte système
        Log.d(TAG, "📝 Contexte système envoyé à Realtime (${systemContext.length} caractères)")
        if (systemContext.isEmpty()) {
            Log.w(TAG, "⚠️ ATTENTION : Le contexte système est VIDE ! Les appels API n'ont peut-être pas terminé.")
        } else {
            Log.d(TAG, "Aperçu du contexte envoyé: ${systemContext.take(200)}...")
        }
        realtimeService?.connect(systemContext)
    }

    /**
     * Déconnecte de l'API Realtime et arrête l'enregistrement audio
     */
    private fun disconnectRealtime() {
        Log.d(TAG, "Déconnexion de l'API Realtime...")

        // Annule tous les collecteurs pour éviter qu'ils tournent en arrière-plan
        realtimeEventsJob?.cancel()
        realtimeErrorsJob?.cancel()
        realtimeConnectionJob?.cancel()
        realtimeEventsJob = null
        realtimeErrorsJob = null
        realtimeConnectionJob = null

        // Arrête l'enregistrement audio
        audioManager?.stopRecording()

        // Déconnecte le WebSocket
        realtimeService?.disconnect()

        _isRealtimeConnected.value = false
        _voiceTranscript.value = ""
        _liveTranscript.value = ""

        // Réinitialise la conversation pour la prochaine session
        resetConversation()

        Log.d(TAG, "Déconnexion Realtime terminée")
    }

    /**
     * Gère les événements reçus du serveur Realtime
     */
    private fun handleRealtimeEvent(event: RealtimeServerEvent) {
        when (event) {
            is RealtimeServerEvent.Error -> {
                Log.e(TAG, "Erreur serveur Realtime: ${event.error.message}")
            }

            is RealtimeServerEvent.SessionCreated -> {
                Log.d(TAG, "Session Realtime créée")
            }

            is RealtimeServerEvent.InputAudioBufferSpeechStarted -> {
                Log.d(TAG, "Détection de parole démarrée")
                _liveTranscript.value = "Écoute en cours..."
            }

            is RealtimeServerEvent.InputAudioBufferSpeechStopped -> {
                Log.d(TAG, "Détection de parole arrêtée")
                _liveTranscript.value = "Traitement..."
            }

            is RealtimeServerEvent.InputAudioTranscriptionCompleted -> {
                Log.d(TAG, "*** Transcription utilisateur reçue: ${event.transcript}")
                // Empile la transcription en attente de la réponse IA correspondante
                pendingUserTranscripts.addLast(event.transcript)
                Log.d(TAG, "*** pendingUserTranscripts taille: ${pendingUserTranscripts.size}")
            }

            is RealtimeServerEvent.ResponseAudioTranscriptDone -> {
                Log.d(TAG, "*** Transcription IA reçue: ${event.transcript}")
                // Dépile la transcription utilisateur la plus ancienne (FIFO)
                val userTranscript = pendingUserTranscripts.removeFirstOrNull() ?: ""
                Log.d(TAG, "*** userTranscript associé: $userTranscript")
                _voiceTranscript.value = event.transcript
                _liveTranscript.value = ""

                // Ajoute la paire utilisateur + IA à la conversation
                addConversationPair(userTranscript, event.transcript)
            }

            is RealtimeServerEvent.ResponseAudioDelta -> {
                // Chunk audio reçu de l'IA : pause micro + lecture
                audioManager?.startSpeaking()
                audioManager?.playAudioChunk(event.delta)
            }

            is RealtimeServerEvent.ResponseAudioDone -> {
                // Fin de l'envoi réseau : le micro reprendra quand la queue sera vidée
                audioManager?.markAudioDone()
                Log.d(TAG, "Réponse audio terminée (réseau)")
            }

            is RealtimeServerEvent.ResponseTextDelta -> {
                // Delta de texte (si modalité texte activée)
                Log.d(TAG, "Texte reçu: ${event.delta}")
            }

            else -> {
                Log.d(TAG, "Événement Realtime: ${event::class.simpleName}")
            }
        }
    }

    /**
     * Ajoute une paire de messages utilisateur + IA à la conversation
     */
    private fun addConversationPair(userTranscript: String, aiTranscript: String) {
        Log.d(TAG, "*** addConversationPair appelé - User: '$userTranscript', AI: '$aiTranscript'")

        // Message utilisateur
        if (userTranscript.isNotEmpty()) {
            val userMessage = MessageData(
                id = messageIdCounter++,
                sessionId = sessionId,
                message = MessageContent(
                    type = "human",
                    content = userTranscript
                )
            )
            conversationMessages.add(userMessage)
            Log.d(TAG, "*** Message utilisateur ajouté: $userTranscript")
        } else {
            Log.w(TAG, "*** Transcription utilisateur vide, non ajoutée")
        }

        // Message IA
        if (aiTranscript.isNotEmpty()) {
            val aiMessage = MessageData(
                id = messageIdCounter++,
                sessionId = sessionId,
                message = MessageContent(
                    type = "ai",
                    content = aiTranscript
                )
            )
            conversationMessages.add(aiMessage)
            Log.d(TAG, "*** Message IA ajouté: $aiTranscript")
        } else {
            Log.w(TAG, "*** Transcription IA vide, non ajoutée")
        }

        Log.d(TAG, "*** Taille de conversationMessages: ${conversationMessages.size}")

        // Envoie automatiquement après chaque échange
        sendConversationToN8n()
    }

    /**
     * Envoie seulement les nouveaux messages (les 2 derniers) à n8n
     */
    private fun sendConversationToN8n() {
        Log.d(TAG, "*** sendConversationToN8n appelé")

        viewModelScope.launch {
            try {
                if (conversationMessages.isEmpty()) {
                    Log.w(TAG, "*** Pas de messages à envoyer (liste vide)")
                    return@launch
                }

                // Prend seulement les 2 derniers messages (la paire user + AI qui vient d'être ajoutée)
                val messagesToSend = conversationMessages.takeLast(2)

                Log.d(TAG, "*** Envoi de ${messagesToSend.size} nouveaux messages à n8n (total conversation: ${conversationMessages.size})...")
                Log.d(TAG, "*** URL: https://n8n.srv1086212.hstgr.cloud/webhook/save_conv")
                Log.d(TAG, "*** Payload: $messagesToSend")

                val response = apiService.saveConversation(messagesToSend)

                if (response.isSuccessful) {
                    Log.d(TAG, "*** ✅ Nouveaux messages envoyés avec succès à n8n")
                    Log.d(TAG, "*** Response code: ${response.code()}")
                } else {
                    Log.e(TAG, "*** ❌ Erreur lors de l'envoi des messages: ${response.code()}")
                    Log.e(TAG, "*** Response body: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "*** ❌ Exception lors de l'envoi des messages à n8n", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Réinitialise la conversation (nouveau session ID)
     */
    fun resetConversation() {
        conversationMessages.clear()
        sessionId = UUID.randomUUID().toString()
        messageIdCounter = 0
        pendingUserTranscripts.clear()
        Log.d(TAG, "Conversation réinitialisée avec nouveau session ID: $sessionId")
    }

    // ========== INITIALISATION ==========

    /**
     * Bloc d'initialisation : charge les messages, tâches, événements et météo au démarrage
     */
    init {
        loadSystemContext()
        loadRecentMessages()
        refreshTasks()
        refreshCalendarEvents()
        refreshWeather()
        // Charger les notes sauvegardées
        _notes.value = notesPreferences.loadNotes()

        viewModelScope.launch {
            weatherPreferences.cityPreferences.collect { prefs ->
                val cityChanged = prefs.cityName != _cityName.value ||
                    prefs.latitude != _cityLatitude.value ||
                    prefs.longitude != _cityLongitude.value
                _cityName.value = prefs.cityName
                _cityLatitude.value = prefs.latitude
                _cityLongitude.value = prefs.longitude
                _showAllergies.value = prefs.showAllergies
                if (cityChanged) {
                    refreshWeather()
                }
            }
        }
    }

    // ========== NETTOYAGE ==========

    /**
     * Appelé quand le ViewModel est détruit
     * Nettoie les ressources (WebSocket, AudioRecord, etc.)
     */
    override fun onCleared() {
        super.onCleared()
        audioManager?.cleanup()
        realtimeService?.cleanup()
    }

}
