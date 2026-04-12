package com.max.aiassistant.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.max.aiassistant.BuildConfig
import com.max.aiassistant.data.api.MaxApiService
import com.max.aiassistant.data.api.MessageContent
import com.max.aiassistant.data.api.MessageData
import com.max.aiassistant.data.api.toEvent
import com.max.aiassistant.data.api.toActuArticle
import com.max.aiassistant.data.api.toCurrentPollen
import com.max.aiassistant.data.api.toRechercheArticle
import com.max.aiassistant.data.api.toWeatherData
import com.max.aiassistant.data.chat.ChatTitleFormatter
import com.max.aiassistant.data.local.DEFAULT_SYSTEM_PROMPT
import com.max.aiassistant.data.local.OnDeviceAiSettings
import com.max.aiassistant.data.local.OnDeviceChatEngine
import com.max.aiassistant.data.local.OnDeviceModelManager
import com.max.aiassistant.data.local.OnDeviceModelProvisioningState
import com.max.aiassistant.data.local.OnDeviceModelVariant
import com.max.aiassistant.data.local.db.ChatConversationEntity
import com.max.aiassistant.data.local.db.ChatMessageEntity
import com.max.aiassistant.data.local.db.ChatMessageRole
import com.max.aiassistant.data.local.db.ChatMessageStatus
import com.max.aiassistant.data.local.db.ChatRepository
import com.max.aiassistant.data.local.db.ConversationTitleStatus
import com.max.aiassistant.data.local.db.MaxDatabase
import com.max.aiassistant.data.local.db.TaskRepository
import com.max.aiassistant.data.local.db.WeatherCacheRepository
import com.max.aiassistant.data.local.db.WeatherCacheSnapshot
import com.max.aiassistant.data.preferences.OnDeviceAiPreferences
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
import kotlinx.coroutines.flow.collectLatest
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
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val CHAT_UI_MESSAGE_LIMIT = 50
        private const val CHAT_CONTEXT_MESSAGE_LIMIT = 10
        private const val MAX_MESSAGES_PER_CONVERSATION = 300
        private const val WEATHER_CACHE_TTL_MILLIS = 60 * 60 * 1000L
    }

    // ========== SERVICES API ==========

    private val apiService = MaxApiService.create()
    private val weatherApiService = com.max.aiassistant.data.api.WeatherApiService.create()
    private val pollenApiService = com.max.aiassistant.data.api.PollenApiService.create()
    private val geocodingApiService = com.max.aiassistant.data.api.GeocodingApiService.create()
    private val weatherPreferences = com.max.aiassistant.data.preferences.WeatherPreferences(application.applicationContext)
    private val notesPreferences = com.max.aiassistant.data.preferences.NotesPreferences(application.applicationContext)
    private val onDeviceAiPreferences = OnDeviceAiPreferences(application.applicationContext)
    private val onDeviceModelManager = OnDeviceModelManager(application.applicationContext)
    private val onDeviceChatEngine = OnDeviceChatEngine(application.applicationContext)
    private val chatDatabase = MaxDatabase.getInstance(application.applicationContext)
    private val chatRepository = ChatRepository(chatDatabase)
    private val taskRepository = TaskRepository(chatDatabase)
    private val weatherCacheRepository = WeatherCacheRepository(chatDatabase.weatherCacheDao())
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
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    private val _conversations = MutableStateFlow<List<ChatConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ChatConversationEntity>> = _conversations.asStateFlow()
    private val _currentConversationTitle = MutableStateFlow(ChatRepository.DEFAULT_CONVERSATION_TITLE)
    val currentConversationTitle: StateFlow<String> = _currentConversationTitle.asStateFlow()
    private val _onDeviceAiSettings = MutableStateFlow(OnDeviceAiPreferences.DEFAULT_SETTINGS)
    val onDeviceAiSettings: StateFlow<OnDeviceAiSettings> = _onDeviceAiSettings.asStateFlow()
    private val _isConversationLimitReached = MutableStateFlow(false)
    val isConversationLimitReached: StateFlow<Boolean> = _isConversationLimitReached.asStateFlow()

    // Indicateur de chargement pour la réponse de l'IA
    private val _isWaitingForAiResponse = MutableStateFlow(false)
    val isWaitingForAiResponse: StateFlow<Boolean> = _isWaitingForAiResponse.asStateFlow()
    private val _isOnDeviceModelReady = MutableStateFlow(false)
    val isOnDeviceModelReady: StateFlow<Boolean> = _isOnDeviceModelReady.asStateFlow()
    private val _onDeviceModelStatus = MutableStateFlow("")
    val onDeviceModelStatus: StateFlow<String> = _onDeviceModelStatus.asStateFlow()
    private val _onDeviceModelProvisioningState =
        MutableStateFlow<OnDeviceModelProvisioningState>(
            OnDeviceModelProvisioningState.Checking("Preparation du modele local...")
        )
    val onDeviceModelProvisioningState: StateFlow<OnDeviceModelProvisioningState> =
        _onDeviceModelProvisioningState.asStateFlow()
    private val _realtimeError = MutableStateFlow<String?>(null)
    val realtimeError: StateFlow<String?> = _realtimeError.asStateFlow()
    private var onDeviceProvisioningJob: Job? = null
    private var activeConversationJob: Job? = null
    private var activeMessagesJob: Job? = null
    private var hasLoadedOnDeviceAiSettings = false

    /**
     * Charge le contexte système (tâches, mémoire, historique, calendrier) pour enrichir le prompt voice-to-voice
     * Récupère les données depuis 4 endpoints différents de manière robuste
     */
    private fun loadSystemContext() {
        viewModelScope.launch {
            Log.d(TAG, "Chargement du contexte système...")
            val builder = StringBuilder()

            // 1. Récupération des tâches locales
            try {
                Log.d(TAG, "Récupération des tâches locales...")
                val tasks = taskRepository.getTasks()

                if (tasks.isNotEmpty()) {
                    builder.append("\n\n=== TÂCHES EN COURS (${tasks.size}) ===\n")
                    tasks.forEach { task ->
                        builder.append("- [${task.status.toSystemContextLabel()}] ${task.title}")
                        if (task.priority != TaskPriority.P3) {
                            builder.append(" (${task.priority.name})")
                        }
                        if (task.deadlineDate.isNotEmpty()) {
                            builder.append(" - Échéance: ${task.deadlineDate}")
                        }
                        builder.append("\n")
                        if (task.description.isNotEmpty()) {
                            builder.append("  Description: ${task.description}\n")
                        }
                    }
                    Log.d(TAG, "✅ ${tasks.size} tâches locales récupérées")
                } else {
                    Log.d(TAG, "Aucune tâche locale trouvée")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la récupération des tâches locales (ignorée)", e)
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
        ensureOnDeviceModelAvailable()
        viewModelScope.launch {
            val conversation = loadOrCreateCurrentConversation()
            attachConversationObservers(conversation.id)
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val conversation = chatRepository.createConversation()
            attachConversationObservers(conversation.id)
        }
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            val conversation = chatRepository.getConversation(conversationId) ?: return@launch
            chatRepository.touchConversationOpened(conversation.id)
            attachConversationObservers(conversation.id)
        }
    }

    fun renameConversation(conversationId: String, title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return

        viewModelScope.launch {
            chatRepository.updateConversationTitle(conversationId, normalizedTitle)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val isCurrentConversation = _currentConversationId.value == conversationId
            chatRepository.deleteConversation(conversationId)

            if (isCurrentConversation) {
                val replacement = chatRepository.getLastOpenedConversationOrCreate()
                attachConversationObservers(replacement.id)
            }
        }
    }

    fun updateOnDeviceAiSettings(
        modelVariant: OnDeviceModelVariant,
        maxContextTokens: Int,
        systemPrompt: String
    ) {
        val normalizedPrompt = systemPrompt.trim().ifBlank { DEFAULT_SYSTEM_PROMPT }
        val newSettings = OnDeviceAiSettings(
            modelVariant = modelVariant,
            maxContextTokens = maxContextTokens,
            systemPrompt = normalizedPrompt
        )

        viewModelScope.launch {
            onDeviceAiPreferences.save(newSettings)
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

        // Active l'indicateur de chargement

        // Envoie le message à l'API et récupère la réponse
        viewModelScope.launch {
            val conversation = loadOrCreateCurrentConversation()
            attachConversationObservers(conversation.id)

            if (conversation.messageCount >= MAX_MESSAGES_PER_CONVERSATION) {
                _isWaitingForAiResponse.value = false
                persistConversationLimitWarning(conversation.id)
                return@launch
            }

            val previousContext = chatRepository.getRecentMessagesForContext(
                conversationId = conversation.id,
                limit = CHAT_CONTEXT_MESSAGE_LIMIT
            )
            val shouldGenerateTitle = conversation.messageCount == 0
            val aiSettings = _onDeviceAiSettings.value
            val userMessage = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversation.id,
                role = ChatMessageRole.USER,
                content = content,
                createdAt = System.currentTimeMillis(),
                imageUri = imageUri?.toString(),
                status = ChatMessageStatus.COMPLETED
            )

            chatRepository.insertMessage(userMessage)
            _isWaitingForAiResponse.value = true

            val aiMessageId = UUID.randomUUID().toString()
            var latestPartialResponse = ""

            try {
                Log.d(TAG, "Envoi du message au moteur local: $content, avec image: ${imageUri != null}")
                val modelReady = awaitOnDeviceModelAvailable()

                val responseText = when {
                    !modelReady -> {
                        "Le modele local n'est pas pret. ${buildProvisioningStatusMessage()}"
                    }

                    imageUri != null -> {
                        "Le mode chat local est branche sur le modele on-device, mais l'analyse d'image n'est pas encore connectee. Envoie un message texte ou ajoute ensuite l'entree vision dans la session LiteRT."
                    }

                    else -> {
                        val readiness = onDeviceChatEngine.getRuntimeReadiness()
                        if (!readiness.canRun) {
                            readiness.reason
                        } else {
                            onDeviceChatEngine.generateResponseStreaming(
                                userMessage = content,
                                modelVariant = aiSettings.modelVariant,
                                maxContextTokens = aiSettings.maxContextTokens,
                                systemInstruction = buildOnDeviceSystemInstruction(previousContext)
                            ) { partialText ->
                                latestPartialResponse = partialText
                                viewModelScope.launch {
                                    chatRepository.upsertAssistantMessage(
                                        conversationId = conversation.id,
                                        messageId = aiMessageId,
                                        content = partialText,
                                        status = ChatMessageStatus.STREAMING
                                    )
                                }
                            }
                        }
                    }
                }

                // Ajoute la réponse de Max
                val finalResponseText = responseText.ifBlank {
                    "Le modele local n'a pas retourne de texte exploitable."
                }

                chatRepository.upsertAssistantMessage(
                    conversationId = conversation.id,
                    messageId = aiMessageId,
                    content = finalResponseText,
                    status = ChatMessageStatus.COMPLETED
                )

                if (shouldGenerateTitle) {
                    generateConversationTitle(conversation.id, content, modelReady)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'envoi du message", e)
                ensureOnDeviceModelAvailable()
                val rawReason = e.message?.takeIf { it.isNotBlank() }
                    ?: buildProvisioningStatusMessage()
                val userFacingReason = if (
                    rawReason.contains("Input token ids are too long", ignoreCase = true) ||
                    rawReason.contains("maximum number of tokens allowed", ignoreCase = true)
                ) {
                    "Le contexte envoye au modele etait trop long pour sa fenetre actuelle. La build a ete ajustee pour compresser davantage le contexte."
                } else {
                    rawReason
                }

                val fallbackMessage = if (latestPartialResponse.isNotBlank()) {
                    latestPartialResponse
                } else {
                    "Impossible d'interroger le modele local pour l'instant. $userFacingReason"
                }

                chatRepository.upsertAssistantMessage(
                    conversationId = conversation.id,
                    messageId = aiMessageId,
                    content = fallbackMessage,
                    status = ChatMessageStatus.ERROR
                )

                if (shouldGenerateTitle) {
                    generateConversationTitle(conversation.id, content, modelReady = false)
                }
            } finally {
                // Désactive l'indicateur de chargement
                _isWaitingForAiResponse.value = false
            }
        }
    }
    
    private suspend fun loadOrCreateCurrentConversation(): ChatConversationEntity {
        val existingConversationId = _currentConversationId.value
        if (existingConversationId != null) {
            val existingConversation = chatRepository.getConversation(existingConversationId)
            if (existingConversation != null) {
                chatRepository.touchConversationOpened(existingConversation.id)
                return existingConversation
            }
        }

        val conversation = chatRepository.getLastOpenedConversationOrCreate()
        chatRepository.touchConversationOpened(conversation.id)
        return conversation
    }

    private fun attachConversationObservers(conversationId: String) {
        if (_currentConversationId.value == conversationId &&
            activeConversationJob?.isActive == true &&
            activeMessagesJob?.isActive == true
        ) {
            return
        }

        _currentConversationId.value = conversationId

        activeConversationJob?.cancel()
        activeConversationJob = viewModelScope.launch {
            chatRepository.observeConversation(conversationId).collectLatest { conversation ->
                if (conversation == null) {
                    return@collectLatest
                }
                _currentConversationTitle.value = conversation.title
                _isConversationLimitReached.value =
                    conversation.messageCount >= MAX_MESSAGES_PER_CONVERSATION
            }
        }

        activeMessagesJob?.cancel()
        activeMessagesJob = viewModelScope.launch {
            chatRepository.observeRecentMessages(conversationId, CHAT_UI_MESSAGE_LIMIT)
                .collectLatest { recentMessages ->
                    _messages.value = recentMessages
                        .asReversed()
                        .map { it.toUiMessage() }
                }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.observeConversations().collectLatest { conversations ->
                _conversations.value = conversations
            }
        }
    }

    private fun observeOnDeviceAiSettings() {
        viewModelScope.launch {
            onDeviceAiPreferences.settings.collectLatest { settings ->
                val previousSettings = _onDeviceAiSettings.value
                val isFirstLoad = !hasLoadedOnDeviceAiSettings

                _onDeviceAiSettings.value = settings
                hasLoadedOnDeviceAiSettings = true

                if (isFirstLoad) {
                    val cachedAvailability = onDeviceModelManager.getCachedAvailability(settings.modelVariant)
                    _isOnDeviceModelReady.value = cachedAvailability.isAvailable
                    _onDeviceModelStatus.value = cachedAvailability.statusMessage
                    ensureOnDeviceModelAvailable()
                    return@collectLatest
                }

                if (previousSettings == settings) {
                    return@collectLatest
                }

                onDeviceChatEngine.close()

                if (previousSettings.modelVariant != settings.modelVariant) {
                    _isOnDeviceModelReady.value = false
                    _onDeviceModelStatus.value = "Changement de modele demande. Preparation de ${settings.modelVariant.displayName}..."
                    ensureOnDeviceModelAvailable()
                } else {
                    val cachedAvailability = onDeviceModelManager.getCachedAvailability(settings.modelVariant)
                    if (cachedAvailability.isAvailable) {
                        _isOnDeviceModelReady.value = true
                        _onDeviceModelStatus.value = "Parametres IA enregistres. Le moteur sera recharge au prochain message."
                    } else {
                        _isOnDeviceModelReady.value = false
                        _onDeviceModelStatus.value = cachedAvailability.statusMessage
                        ensureOnDeviceModelAvailable()
                    }
                }
            }
        }
    }

    private suspend fun persistConversationLimitWarning(conversationId: String) {
        chatRepository.upsertAssistantMessage(
            conversationId = conversationId,
            messageId = "conversation-limit-$conversationId",
            content = "Cette conversation est devenue trop longue pour rester robuste sur mobile. Ouvre une nouvelle conversation pour continuer proprement.",
            status = ChatMessageStatus.ERROR
        )
    }

    private fun generateConversationTitle(
        conversationId: String,
        firstPrompt: String,
        modelReady: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingConversation = chatRepository.getConversation(conversationId) ?: return@launch
            if (existingConversation.titleStatus != ConversationTitleStatus.PENDING) {
                return@launch
            }

            val fallbackTitle = ChatTitleFormatter.fallbackTitleFromPrompt(firstPrompt)
            val finalTitle = if (modelReady && firstPrompt.isNotBlank()) {
                runCatching {
                    val titleEngine = OnDeviceChatEngine(getApplication<Application>().applicationContext)
                    val aiSettings = _onDeviceAiSettings.value
                    try {
                        titleEngine.generateResponse(
                            userMessage = firstPrompt,
                            modelVariant = aiSettings.modelVariant,
                            maxContextTokens = aiSettings.maxContextTokens,
                            systemInstruction = """
                                Tu generes des titres de conversations.
                                Retourne uniquement un titre court en francais.
                                Maximum 6 mots.
                                Pas de guillemets.
                            """.trimIndent()
                        )
                    } finally {
                        titleEngine.close()
                    }
                }.getOrNull()
                    ?.let(ChatTitleFormatter::normalizeGeneratedTitle)
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackTitle
            } else {
                fallbackTitle
            }

            chatRepository.updateConversationTitle(conversationId, finalTitle)
        }
    }

    fun retryOnDeviceModelDownload() {
        ensureOnDeviceModelAvailable(forceDownload = true)
    }

    private suspend fun awaitOnDeviceModelAvailable(forceDownload: Boolean = false): Boolean {
        ensureOnDeviceModelAvailable(forceDownload = forceDownload)
        onDeviceProvisioningJob?.join()
        return _isOnDeviceModelReady.value
    }

    private fun ensureOnDeviceModelAvailable(forceDownload: Boolean = false) {
        if (!hasLoadedOnDeviceAiSettings) {
            return
        }

        if (onDeviceProvisioningJob?.isActive == true && !forceDownload) {
            return
        }

        if (_isOnDeviceModelReady.value && !forceDownload) {
            return
        }

        onDeviceProvisioningJob?.cancel()
        onDeviceProvisioningJob = viewModelScope.launch {
            try {
                val availability = onDeviceModelManager.prepareModel(
                    modelVariant = _onDeviceAiSettings.value.modelVariant,
                    forceDownload = forceDownload
                ) { state ->
                    _onDeviceModelProvisioningState.value = state
                    syncOnDeviceUiState(state)
                }
                _isOnDeviceModelReady.value = availability.isAvailable
                _onDeviceModelStatus.value = availability.statusMessage
            } catch (e: Exception) {
                Log.e(TAG, "Provisioning du modele local impossible", e)
                val state = OnDeviceModelProvisioningState.Error(
                    "Le telechargement du modele a echoue. Verifie la connexion reseau puis reessaie."
                )
                _onDeviceModelProvisioningState.value = state
                syncOnDeviceUiState(state)
            }
        }
    }

    private fun syncOnDeviceUiState(state: OnDeviceModelProvisioningState) {
        when (state) {
            is OnDeviceModelProvisioningState.Checking -> {
                _isOnDeviceModelReady.value = false
                _onDeviceModelStatus.value = state.message
            }

            is OnDeviceModelProvisioningState.Downloading -> {
                _isOnDeviceModelReady.value = false
                _onDeviceModelStatus.value = state.message
            }

            is OnDeviceModelProvisioningState.Verifying -> {
                _isOnDeviceModelReady.value = false
                _onDeviceModelStatus.value = state.message
            }

            is OnDeviceModelProvisioningState.Ready -> {
                _isOnDeviceModelReady.value = true
                _onDeviceModelStatus.value = state.message
            }

            is OnDeviceModelProvisioningState.Error -> {
                _isOnDeviceModelReady.value = false
                _onDeviceModelStatus.value = state.message
            }
        }
    }

    private fun buildProvisioningStatusMessage(): String {
        return when (val state = _onDeviceModelProvisioningState.value) {
            is OnDeviceModelProvisioningState.Checking -> state.message
            is OnDeviceModelProvisioningState.Downloading -> state.message
            is OnDeviceModelProvisioningState.Verifying -> state.message
            is OnDeviceModelProvisioningState.Ready -> state.message
            is OnDeviceModelProvisioningState.Error -> state.message
        }
    }

    private fun buildOnDeviceSystemInstruction(recentMessages: List<ChatMessageEntity>): String {
        val basePrompt = _onDeviceAiSettings.value.systemPrompt.trim().ifBlank { DEFAULT_SYSTEM_PROMPT }
        return buildString {
            appendLine(basePrompt)
            if (recentMessages.isNotEmpty()) {
                appendLine()
                appendLine("Historique recent de la conversation:")
                recentMessages.forEach { message ->
                    val speaker = if (message.role == ChatMessageRole.USER) "Utilisateur" else "Max"
                    appendLine("- $speaker: ${message.content.take(250)}")
                }
            }
            if (systemContext.isNotBlank()) {
                appendLine()
                appendLine("Contexte utile:")
                appendLine(systemContext.take(1_200))
            }
        }
    }

    // ========== ÉTAT DES TÂCHES ==========

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoadingTasks = MutableStateFlow(false)
    val isLoadingTasks: StateFlow<Boolean> = _isLoadingTasks.asStateFlow()
    private val _taskError = MutableStateFlow<String?>(null)
    val taskError: StateFlow<String?> = _taskError.asStateFlow()

    /**
     * Recharge les tâches depuis la base locale
     */
    fun refreshTasks() {
        viewModelScope.launch {
            if (_isLoadingTasks.value) return@launch
            val startedAt = System.currentTimeMillis()
            try {
                _isLoadingTasks.value = true
                _taskError.value = null
                Log.d(TAG, "Récupération des tâches locales...")
                val tasks = taskRepository.getTasks()

                _tasks.value = tasks
                Log.d(TAG, "Tâches récupérées: ${tasks.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des tâches", e)
                _taskError.value = "Impossible de récupérer les tâches locales pour le moment."
            } finally {
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < 250L) {
                    delay(250L - elapsed)
                }
                _isLoadingTasks.value = false
            }
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeTasks().collectLatest { tasks ->
                _tasks.value = tasks
            }
        }
    }

    /**
     * Met à jour le statut d'une tâche locale
     */
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        updateTaskLocally(taskId) { it.copy(status = newStatus) }
    }

    /**
     * Met à jour la priorité d'une tâche
     */
    fun updateTaskPriority(taskId: String, newPriority: TaskPriority) {
        updateTaskLocally(taskId) { it.copy(priority = newPriority) }
    }

    /**
     * Met à jour la durée estimée d'une tâche
     */
    fun updateTaskDuration(taskId: String, newDuration: String) {
        updateTaskLocally(taskId) { it.copy(estimatedDuration = newDuration) }
    }

    /**
     * Met à jour la date d'échéance d'une tâche
     * @param taskId ID de la tâche
     * @param newDeadlineDate Date au format ISO (ex: "2025-12-15")
     */
    fun updateTaskDeadline(taskId: String, newDeadlineDate: String) {
        updateTaskLocally(taskId) { task ->
            task.copy(
                deadlineDate = newDeadlineDate,
                deadline = com.max.aiassistant.data.api.formatDeadline(newDeadlineDate)
            )
        }
    }

    /**
     * Met à jour la catégorie d'une tâche
     */
    fun updateTaskCategory(taskId: String, newCategory: String) {
        updateTaskLocally(taskId) { it.copy(category = newCategory) }
    }

    /**
     * Met à jour le titre d'une tâche
     */
    fun updateTaskTitle(taskId: String, newTitle: String) {
        updateTaskLocally(taskId) { it.copy(title = newTitle) }
    }

    /**
     * Met à jour la description d'une tâche
     */
    fun updateTaskDescription(taskId: String, newDescription: String) {
        updateTaskLocally(taskId) { it.copy(description = newDescription) }
    }

    /**
     * Met à jour les notes d'une tâche
     */
    fun updateTaskNote(taskId: String, newNote: String) {
        updateTaskLocally(taskId) { it.copy(note = newNote) }
    }

    /**
     * Persiste la version courante d'une tâche locale en base
     */
    private fun persistTask(taskId: String) {
        val task = _tasks.value.find { it.id == taskId }
        if (task == null) {
            Log.e(TAG, "persistTask: Tâche $taskId non trouvée")
            return
        }

        viewModelScope.launch {
            try {
                taskRepository.updateTask(task)
                Log.d(TAG, "Tâche locale persistée: $taskId")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la persistance de la tâche locale", e)
                _taskError.value = "Impossible d'enregistrer cette tâche."
            }
        }
    }

    private fun updateTaskLocally(taskId: String, transform: (Task) -> Task) {
        var updated = false
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                updated = true
                transform(task)
            } else {
                task
            }
        }
        if (updated) {
            persistTask(taskId)
        }
    }

    /**
     * Supprime une tâche locale et met à jour la liste affichée
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Suppression locale de la tâche $taskId...")
                taskRepository.deleteTask(taskId)
                _tasks.value = _tasks.value.filter { it.id != taskId }
                Log.d(TAG, "Tâche supprimée avec succès")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression locale de la tâche", e)
                _taskError.value = "Impossible de supprimer cette tâche."
            }
        }
    }

    /**
     * Crée une nouvelle tâche en local et rafraîchit la liste
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
                Log.d(TAG, "Création locale de la tâche: $titre...")
                taskRepository.createTask(
                    titre = titre,
                    categorie = categorie,
                    description = description,
                    note = note,
                    priorite = priorite,
                    dateLimite = dateLimite,
                    dureeEstimee = dureeEstimee
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création locale de la tâche", e)
                _taskError.value = "Impossible de créer cette tâche."
            }
        }
    }

    // ========== GESTION DES SOUS-TÂCHES ==========

    /**
     * Crée une nouvelle sous-tâche en local
     */
    fun createSubTask(taskId: String, text: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Création locale de la sous-tâche pour la tâche $taskId...")
                val createdSubTask = taskRepository.createSubTask(taskId = taskId, text = text)
                _tasks.value = _tasks.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(subTasks = task.subTasks + createdSubTask)
                    } else {
                        task
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création locale de la sous-tâche", e)
                _taskError.value = "Impossible d'ajouter cette sous-tâche."
            }
        }
    }

    /**
     * Met à jour une sous-tâche locale (texte ou statut)
     */
    fun updateSubTask(subTaskId: String, text: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Mise à jour locale de la sous-tâche $subTaskId...")
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
                taskRepository.updateSubTask(
                    subTaskId = subTaskId,
                    text = text,
                    isCompleted = isCompleted
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la mise à jour locale de la sous-tâche", e)
                _taskError.value = "Impossible d'enregistrer cette sous-tâche."
                refreshTasks()
            }
        }
    }

    /**
     * Supprime une sous-tâche locale
     */
    fun deleteSubTask(subTaskId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Suppression locale de la sous-tâche $subTaskId...")
                _tasks.value = _tasks.value.map { task ->
                    task.copy(
                        subTasks = task.subTasks.filter { it.id != subTaskId }
                    )
                }
                taskRepository.deleteSubTask(subTaskId)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression locale de la sous-tâche", e)
                _taskError.value = "Impossible de supprimer cette sous-tâche."
                refreshTasks()
            }
        }
    }

    // ========== ÉTAT DU PLANNING ==========

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()
    private val _eventsError = MutableStateFlow<String?>(null)
    val eventsError: StateFlow<String?> = _eventsError.asStateFlow()

    // ========== ÉTAT DE LA MÉTÉO ==========

    private val _weatherData = MutableStateFlow<com.max.aiassistant.data.api.WeatherData?>(null)
    val weatherData: StateFlow<com.max.aiassistant.data.api.WeatherData?> = _weatherData.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()
    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

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
    private val _actuError = MutableStateFlow<String?>(null)
    val actuError: StateFlow<String?> = _actuError.asStateFlow()

    /**
     * Récupère les actualités et les recherches IA depuis l'API N8N
     */
    fun refreshActu() {
        viewModelScope.launch {
            try {
                _isLoadingActu.value = true
                _actuError.value = null
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
                _actuError.value = "Impossible de récupérer les actualités."
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
    fun refreshWeather(force: Boolean = false) {
        viewModelScope.launch {
            if (_isLoadingWeather.value) {
                Log.d(TAG, "Rafraichissement meteo ignore car un chargement est deja en cours")
                return@launch
            }

            _isLoadingWeather.value = true
            _weatherError.value = null

            val cityName = _cityName.value
            val latitude = _cityLatitude.value
            val longitude = _cityLongitude.value
            val now = System.currentTimeMillis()
            var hasRefreshError = false

            try {
                val cachedSnapshot = weatherCacheRepository.get(latitude, longitude)
                cachedSnapshot?.let { snapshot ->
                    _weatherData.value = snapshot.weatherData
                    Log.d(
                        TAG,
                        "Donnees meteo chargees depuis la BDD (weatherFetchedAt=${snapshot.weatherFetchedAt}, pollenFetchedAt=${snapshot.pollenFetchedAt})"
                    )
                }

                val shouldRefreshWeather = force ||
                    cachedSnapshot == null ||
                    cachedSnapshot.isWeatherExpired(now, WEATHER_CACHE_TTL_MILLIS)
                val shouldRefreshPollen = force ||
                    cachedSnapshot == null ||
                    cachedSnapshot.isPollenExpired(now, WEATHER_CACHE_TTL_MILLIS)

                if (!shouldRefreshWeather && !shouldRefreshPollen) {
                    Log.d(TAG, "Cache meteo et allergies encore frais, aucun appel API necessaire")
                    return@launch
                }

                var currentWeatherData = cachedSnapshot?.weatherData
                var weatherFetchedAt = cachedSnapshot?.weatherFetchedAt ?: 0L
                var pollenFetchedAt = cachedSnapshot?.pollenFetchedAt

                if (shouldRefreshWeather) {
                    try {
                        val weatherFromApi = weatherApiService.getWeatherForecast(
                            latitude = latitude,
                            longitude = longitude
                        ).toWeatherData()

                        currentWeatherData = if (!shouldRefreshPollen) {
                            weatherCacheRepository.mergePollen(
                                weatherFromApi,
                                cachedSnapshot!!.weatherData.toCurrentPollenData()
                            )
                        } else {
                            weatherFromApi
                        }
                        weatherFetchedAt = now
                        Log.d(TAG, "Meteo actualisee depuis l'API")
                    } catch (e: Exception) {
                        hasRefreshError = true
                        Log.e(TAG, "Erreur lors de l'actualisation de la meteo", e)
                    }
                }

                if (shouldRefreshPollen) {
                    try {
                        val pollenData = pollenApiService.getPollenForecast(
                            latitude = latitude,
                            longitude = longitude
                        ).toCurrentPollen()

                        currentWeatherData = currentWeatherData?.let {
                            weatherCacheRepository.mergePollen(it, pollenData)
                        }
                        pollenFetchedAt = now
                        Log.d(TAG, "Pollens actualises depuis l'API air-quality")
                    } catch (e: Exception) {
                        hasRefreshError = true
                        Log.w(TAG, "Impossible de recuperer les pollens depuis l'API air-quality", e)
                    }
                }

                currentWeatherData?.let { weatherData ->
                    val snapshot = WeatherCacheSnapshot(
                        cityName = cityName,
                        latitude = latitude,
                        longitude = longitude,
                        weatherData = weatherData,
                        weatherFetchedAt = weatherFetchedAt.takeIf { it > 0 } ?: now,
                        pollenFetchedAt = pollenFetchedAt
                    )
                    weatherCacheRepository.save(snapshot)
                    _weatherData.value = snapshot.weatherData
                    Log.d(
                        TAG,
                        "Donnees meteo disponibles: ${weatherData.currentTemperature}°C, ${weatherData.hourlyForecasts.size} previsions horaires"
                    )
                }

                if (hasRefreshError && _weatherData.value == null) {
                    _weatherError.value = "Impossible de recuperer la meteo pour l'instant."
                }

                return@launch
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
                _weatherError.value = "Impossible de récupérer la météo pour l'instant."
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
                _eventsError.value = null
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
                _eventsError.value = "Impossible de récupérer le planning."
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
        _realtimeError.value = null

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
                _realtimeError.value = error
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
        _realtimeError.value = null
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
                _realtimeError.value = event.error.message
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
        observeOnDeviceAiSettings()
        observeConversations()
        observeTasks()
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
        onDeviceChatEngine.close()
        audioManager?.cleanup()
        realtimeService?.cleanup()
    }

}

private fun ChatMessageEntity.toUiMessage(): Message {
    return Message(
        id = id,
        content = content,
        isFromUser = role == ChatMessageRole.USER,
        timestamp = createdAt,
        imageUri = imageUri?.let(Uri::parse)
    )
}

private fun com.max.aiassistant.data.api.WeatherData.toCurrentPollenData():
    com.max.aiassistant.data.api.CurrentPollenData {
    return com.max.aiassistant.data.api.CurrentPollenData(
        grassPollen = grassPollen,
        birchPollen = birchPollen,
        alderPollen = alderPollen,
        olivePollen = olivePollen,
        mugwortPollen = mugwortPollen,
        ragweedPollen = ragweedPollen
    )
}

private fun TaskStatus.toSystemContextLabel(): String {
    return when (this) {
        TaskStatus.TODO -> "À faire"
        TaskStatus.IN_PROGRESS -> "En cours"
        TaskStatus.COMPLETED -> "Terminé"
    }
}
