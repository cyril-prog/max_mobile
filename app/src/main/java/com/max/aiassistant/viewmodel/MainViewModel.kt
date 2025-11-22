package com.max.aiassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.max.aiassistant.data.api.MaxApiService
import com.max.aiassistant.data.api.toTask
import com.max.aiassistant.data.api.toEvent
import com.max.aiassistant.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
class MainViewModel : ViewModel() {

    // ========== SERVICE API ==========

    private val apiService = MaxApiService.create()
    private val TAG = "MainViewModel"

    // ========== ÉTAT DU CHAT ==========

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /**
     * Charge les messages récents depuis l'API au démarrage
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
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Ajoute le message de l'utilisateur immédiatement
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = content,
            isFromUser = true
        )
        _messages.value = _messages.value + userMessage

        // Envoie le message à l'API et récupère la réponse
        viewModelScope.launch {
            try {
                Log.d(TAG, "Envoi du message: $content")

                // Appel API avec GET (passage du texte en query parameter)
                val response = apiService.sendMessage(content)

                Log.d(TAG, "Réponse reçue: ${response.text}")

                // Ajoute la réponse de Max
                val aiResponse = Message(
                    id = UUID.randomUUID().toString(),
                    content = response.text,
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
            }
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
     * Supprime une tâche
     */
    fun deleteTask(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }

    // ========== ÉTAT DU PLANNING ==========

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()

    /**
     * Récupère les événements du calendrier depuis l'API
     */
    fun refreshCalendarEvents() {
        viewModelScope.launch {
            try {
                _isLoadingEvents.value = true
                Log.d(TAG, "Récupération des événements du calendrier...")

                val response = apiService.getCalendarEvents()
                val events = response.text.data.map { it.toEvent() }

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

    // ========== ÉTAT VOICE ==========

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceTranscript = MutableStateFlow("The capital of France is Paris")
    val voiceTranscript: StateFlow<String> = _voiceTranscript.asStateFlow()

    /**
     * Toggle le mode écoute voice
     *
     * STUB : Simule l'activation du micro
     *
     * TODO: Intégration reconnaissance vocale
     * - Utiliser SpeechRecognizer d'Android
     * - Ou intégrer Google Cloud Speech-to-Text
     * - Gérer les permissions RECORD_AUDIO
     */
    fun toggleListening() {
        _isListening.value = !_isListening.value

        if (_isListening.value) {
            // Simule la capture audio
            viewModelScope.launch {
                delay(2000)
                _voiceTranscript.value = "Nouvelle transcription simulée..."
                _isListening.value = false
            }
        }
    }

    // ========== INITIALISATION ==========

    /**
     * Bloc d'initialisation : charge les messages, tâches et événements au démarrage
     */
    init {
        loadRecentMessages()
        refreshTasks()
        refreshCalendarEvents()
    }

}
