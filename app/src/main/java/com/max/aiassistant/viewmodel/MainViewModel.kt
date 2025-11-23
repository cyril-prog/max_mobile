package com.max.aiassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.max.aiassistant.data.api.MaxApiService
import com.max.aiassistant.data.api.parseWebhookResponse
import com.max.aiassistant.data.api.toTask
import com.max.aiassistant.data.api.toEvent
import com.max.aiassistant.data.realtime.RealtimeApiService
import com.max.aiassistant.data.realtime.RealtimeAudioManager
import com.max.aiassistant.data.realtime.RealtimeServerEvent
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

    // ========== SERVICES API ==========

    private val apiService = MaxApiService.create()
    private val TAG = "MainViewModel"

    // Clé API OpenAI pour l'API Realtime
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY

    // Service Realtime (initialisé paresseusement)
    private var realtimeService: RealtimeApiService? = null
    private var audioManager: RealtimeAudioManager? = null

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
                val httpResponse = apiService.sendMessage(content)

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

        // Initialise le service Realtime
        realtimeService = RealtimeApiService(OPENAI_API_KEY)

        // Initialise le gestionnaire audio
        audioManager = RealtimeAudioManager { base64Audio ->
            // Callback appelé pour chaque chunk audio capturé
            realtimeService?.sendAudioChunk(base64Audio)
        }

        // Observe les événements du serveur
        viewModelScope.launch {
            realtimeService?.serverEvents?.collect { event ->
                handleRealtimeEvent(event)
            }
        }

        // Observe les erreurs
        viewModelScope.launch {
            realtimeService?.errors?.collect { error ->
                Log.e(TAG, "Erreur Realtime: $error")
            }
        }

        // Observe l'état de connexion
        viewModelScope.launch {
            realtimeService?.isConnected?.collect { connected ->
                _isRealtimeConnected.value = connected
                if (connected) {
                    // Démarre l'enregistrement audio une fois connecté
                    audioManager?.startRecording()
                    Log.d(TAG, "Connexion Realtime établie, enregistrement démarré")
                }
            }
        }

        // Lance la connexion WebSocket
        realtimeService?.connect()
    }

    /**
     * Déconnecte de l'API Realtime et arrête l'enregistrement audio
     */
    private fun disconnectRealtime() {
        Log.d(TAG, "Déconnexion de l'API Realtime...")

        // Arrête l'enregistrement audio
        audioManager?.stopRecording()

        // Déconnecte le WebSocket
        realtimeService?.disconnect()

        _isRealtimeConnected.value = false
        _voiceTranscript.value = ""
        _liveTranscript.value = ""

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

            is RealtimeServerEvent.ResponseAudioTranscriptDone -> {
                Log.d(TAG, "Transcription reçue: ${event.transcript}")
                _voiceTranscript.value = event.transcript
                _liveTranscript.value = ""
            }

            is RealtimeServerEvent.ResponseAudioDelta -> {
                // Chunk audio reçu de l'IA, on le joue
                audioManager?.playAudioChunk(event.delta)
            }

            is RealtimeServerEvent.ResponseAudioDone -> {
                Log.d(TAG, "Réponse audio terminée")
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

    // ========== INITIALISATION ==========

    /**
     * Bloc d'initialisation : charge les messages, tâches et événements au démarrage
     */
    init {
        loadRecentMessages()
        refreshTasks()
        refreshCalendarEvents()
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
