package com.max.aiassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.max.aiassistant.BuildConfig
import com.max.aiassistant.data.api.MaxApiService
import com.max.aiassistant.data.api.MessageContent
import com.max.aiassistant.data.api.MessageData
import com.max.aiassistant.data.api.parseWebhookResponse
import com.max.aiassistant.data.api.toTask
import com.max.aiassistant.data.api.toEvent
import com.max.aiassistant.data.api.toEnrichedPrompt
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

    // Clé API OpenAI pour l'API Realtime (chargée depuis local.properties via BuildConfig)
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY

    // Service Realtime (initialisé paresseusement)
    private var realtimeService: RealtimeApiService? = null
    private var audioManager: RealtimeAudioManager? = null

    // Contexte système pour enrichir le prompt du voice-to-voice
    private var systemContext: String = ""

    // ========== ÉTAT DU CHAT ==========

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /**
     * Charge le contexte système (tâches, mémoire, historique) pour enrichir le prompt voice-to-voice
     */
    private fun loadSystemContext() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Chargement du contexte système...")

                val response = apiService.getSystemContext()

                // La réponse est un tableau, on prend le premier élément
                if (response.isNotEmpty()) {
                    systemContext = response[0].toEnrichedPrompt()
                    Log.d(TAG, "Contexte système chargé (${systemContext.length} caractères)")
                    Log.d(TAG, "Contexte prévisualisation: ${systemContext.take(300)}...")
                } else {
                    Log.w(TAG, "La réponse du contexte système est vide")
                    systemContext = ""
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement du contexte système", e)
                Log.e(TAG, "Stack trace:", e)
                // En cas d'erreur, on garde le contexte vide
                systemContext = ""
            }
        }
    }

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

    // ========== STOCKAGE DES CONVERSATIONS VOICE ==========

    // Liste des messages de la conversation en cours (pour envoi à n8n)
    private val conversationMessages = mutableListOf<MessageData>()

    // ID de session unique pour cette conversation
    private var sessionId: String = UUID.randomUUID().toString()

    // Compteur pour générer les IDs de messages
    private var messageIdCounter = 0

    // Dernière transcription utilisateur (en attente de la réponse IA)
    private var pendingUserTranscript: String? = null

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

        // Lance la connexion WebSocket avec le contexte système
        realtimeService?.connect(systemContext)
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
                // Stocke la transcription utilisateur en attente de la réponse IA
                pendingUserTranscript = event.transcript
                Log.d(TAG, "*** pendingUserTranscript stocké: $pendingUserTranscript")
            }

            is RealtimeServerEvent.ResponseAudioTranscriptDone -> {
                Log.d(TAG, "*** Transcription IA reçue: ${event.transcript}")
                Log.d(TAG, "*** pendingUserTranscript avant ajout: $pendingUserTranscript")
                _voiceTranscript.value = event.transcript
                _liveTranscript.value = ""

                // Ajoute la paire utilisateur + IA à la conversation
                addConversationPair(pendingUserTranscript ?: "", event.transcript)
                pendingUserTranscript = null
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
     * Envoie la conversation complète à n8n
     */
    private fun sendConversationToN8n() {
        Log.d(TAG, "*** sendConversationToN8n appelé")

        viewModelScope.launch {
            try {
                if (conversationMessages.isEmpty()) {
                    Log.w(TAG, "*** Pas de messages à envoyer (liste vide)")
                    return@launch
                }

                Log.d(TAG, "*** Envoi de ${conversationMessages.size} messages à n8n...")
                Log.d(TAG, "*** URL: https://n8n.srv1086212.hstgr.cloud/webhook/save_conv")
                Log.d(TAG, "*** Payload: ${conversationMessages.take(2)}")  // Log les 2 premiers messages

                val response = apiService.saveConversation(conversationMessages)

                if (response.isSuccessful) {
                    Log.d(TAG, "*** ✅ Conversation envoyée avec succès à n8n")
                    Log.d(TAG, "*** Response code: ${response.code()}")
                } else {
                    Log.e(TAG, "*** ❌ Erreur lors de l'envoi de la conversation: ${response.code()}")
                    Log.e(TAG, "*** Response body: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "*** ❌ Exception lors de l'envoi de la conversation à n8n", e)
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
        pendingUserTranscript = null
        Log.d(TAG, "Conversation réinitialisée avec nouveau session ID: $sessionId")
    }

    // ========== INITIALISATION ==========

    /**
     * Bloc d'initialisation : charge les messages, tâches et événements au démarrage
     */
    init {
        loadSystemContext()
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
