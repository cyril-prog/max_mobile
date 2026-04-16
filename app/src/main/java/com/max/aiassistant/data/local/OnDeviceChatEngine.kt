package com.max.aiassistant.data.local

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.tool
import com.max.aiassistant.data.local.db.MaxDatabase
import com.max.aiassistant.data.local.db.ChatMessageEntity
import com.max.aiassistant.data.local.db.ChatMessageRole
import com.max.aiassistant.data.local.db.MemoryGraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OnDeviceModelAvailability(
    val isAvailable: Boolean,
    val modelPath: String? = null,
    val statusMessage: String
)

data class OnDeviceRuntimeReadiness(
    val canRun: Boolean,
    val reason: String
)

enum class ToolDebugPhase {
    CALL,
    RESULT,
    ERROR
}

data class ToolDebugEvent(
    val toolName: String,
    val phase: ToolDebugPhase,
    val payload: String
)

class OnDeviceChatEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "OnDeviceChatEngine"
        private const val MIN_TOTAL_RAM_BYTES = 8L * 1024L * 1024L * 1024L
        private const val MIN_AVAILABLE_RAM_BYTES = 2_200L * 1024L * 1024L
        private const val CPU_THREADS = 4
    }

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedConversationKey: String? = null
    private var loadedModelPath: String? = null
    private var loadedSystemInstruction: String? = null
    private var loadedMaxContextTokens: Int? = null
    private var loadedVisionEnabled: Boolean? = null
    private var loadedAudioEnabled: Boolean? = null
    var onToolDebugEvent: ((ToolDebugEvent) -> Unit)? = null
    private val memoryGraphRepository = MemoryGraphRepository(MaxDatabase.getInstance(context).memoryGraphDao())
    private val toolProviders: List<ToolProvider> = listOf(
        buildStoreImportantMemoryToolProvider(),
        buildSearchMemoryToolProvider()
    )

    fun getAvailability(modelVariant: OnDeviceModelVariant): OnDeviceModelAvailability {
        val modelFile = resolveModelFile(modelVariant)
        return if (modelFile != null) {
            OnDeviceModelAvailability(
                isAvailable = true,
                modelPath = modelFile.absolutePath,
                statusMessage = "Modele local detecte: ${modelFile.name}"
            )
        } else {
            OnDeviceModelAvailability(
                isAvailable = false,
                statusMessage = "Modele local introuvable sur l'appareil."
            )
        }
    }

    fun getRuntimeReadiness(): OnDeviceRuntimeReadiness {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        if (activityManager.isLowRamDevice) {
            return OnDeviceRuntimeReadiness(
                canRun = false,
                reason = "Le telephone est detecte comme appareil low-RAM. Le modele local risque de faire planter l'application."
            )
        }

        if (memoryInfo.totalMem < MIN_TOTAL_RAM_BYTES) {
            return OnDeviceRuntimeReadiness(
                canRun = false,
                reason = "Memoire totale insuffisante pour ce modele local. Il faut idealement au moins 8 Go de RAM."
            )
        }

        if (memoryInfo.availMem < MIN_AVAILABLE_RAM_BYTES) {
            val availableMb = memoryInfo.availMem / (1024L * 1024L)
            return OnDeviceRuntimeReadiness(
                canRun = false,
                reason = "Memoire libre insuffisante pour lancer le modele maintenant (${availableMb} Mo libres). Ferme quelques applications puis reessaie."
            )
        }

        return OnDeviceRuntimeReadiness(
            canRun = true,
            reason = "Memoire suffisante detectee pour tenter l'inference locale."
        )
    }

    suspend fun generateResponse(
        userMessage: String,
        userImageUri: Uri? = null,
        userAudioClip: ByteArray? = null,
        modelVariant: OnDeviceModelVariant,
        maxContextTokens: Int,
        systemInstruction: String? = null,
        conversationKey: String? = null,
        initialHistory: List<ChatMessageEntity> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val availability = getAvailability(modelVariant)
        val modelPath = resolveModelFile(modelVariant)?.absolutePath
            ?: availability.modelPath
            ?: throw IllegalStateException(availability.statusMessage)
        val runtimeReadiness = getRuntimeReadiness()
        if (!runtimeReadiness.canRun) {
            throw IllegalStateException(runtimeReadiness.reason)
        }
        val enableVision = userImageUri != null || initialHistory.any { !it.imageUri.isNullOrBlank() }
        val enableAudio = userAudioClip != null

        val activeConversation = getOrCreateConversation(
            modelPath = modelPath,
            systemInstruction = systemInstruction,
            maxContextTokens = maxContextTokens,
            conversationKey = conversationKey,
            initialHistory = initialHistory,
            enableVision = enableVision,
            enableAudio = enableAudio
        )
        val response = activeConversation.sendMessage(
            buildUserInputContents(
                text = userMessage,
                imageUri = userImageUri,
                audioClip = userAudioClip
            ),
            emptyMap<String, Any>()
        )
        response.extractText()
    }

    suspend fun generateResponseStreaming(
        userMessage: String,
        userImageUri: Uri? = null,
        userAudioClip: ByteArray? = null,
        modelVariant: OnDeviceModelVariant,
        maxContextTokens: Int,
        systemInstruction: String? = null,
        conversationKey: String? = null,
        initialHistory: List<ChatMessageEntity> = emptyList(),
        onPartialResponse: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val availability = getAvailability(modelVariant)
        val modelPath = resolveModelFile(modelVariant)?.absolutePath
            ?: availability.modelPath
            ?: throw IllegalStateException(availability.statusMessage)
        val runtimeReadiness = getRuntimeReadiness()
        if (!runtimeReadiness.canRun) {
            throw IllegalStateException(runtimeReadiness.reason)
        }
        val enableVision = userImageUri != null || initialHistory.any { !it.imageUri.isNullOrBlank() }
        val enableAudio = userAudioClip != null

        val activeConversation = getOrCreateConversation(
            modelPath = modelPath,
            systemInstruction = systemInstruction,
            maxContextTokens = maxContextTokens,
            conversationKey = conversationKey,
            initialHistory = initialHistory,
            enableVision = enableVision,
            enableAudio = enableAudio
        )
        val userInput = buildUserInputContents(
            text = userMessage,
            imageUri = userImageUri,
            audioClip = userAudioClip
        )

        suspendCancellableCoroutine { continuation ->
            var latestText = ""

            continuation.invokeOnCancellation {
                activeConversation.cancelProcess()
            }

            activeConversation.sendMessageAsync(
                userInput,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        val partialText = message.extractText()
                        // Preserve newline-only chunks: they carry paragraph structure in streaming mode.
                        if (partialText.isEmpty()) {
                            return
                        }

                        val mergedText = mergeStreamingText(
                            currentText = latestText,
                            incomingText = partialText
                        )
                        if (mergedText == latestText) {
                            return
                        }

                        latestText = mergedText
                        onPartialResponse(mergedText)
                    }

                    override fun onDone() {
                        if (continuation.isActive) {
                            continuation.resume(latestText)
                        }
                    }

                    override fun onError(throwable: Throwable) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(throwable)
                        }
                    }
                },
                emptyMap<String, Any>()
            )
        }
    }

    fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        loadedConversationKey = null
        loadedModelPath = null
        loadedSystemInstruction = null
        loadedMaxContextTokens = null
        loadedVisionEnabled = null
        loadedAudioEnabled = null
    }

    private fun resolveModelFile(modelVariant: OnDeviceModelVariant): File? {
        val externalRoot = context.getExternalFilesDir(null)
        val candidatePaths = buildList {
            if (externalRoot != null) {
                add(File(externalRoot, "models/${modelVariant.storageFileName}").absolutePath)
            }

            add("/data/local/tmp/llm/${modelVariant.storageFileName}")
        }

        return candidatePaths
            .asSequence()
            .map(::File)
            .firstOrNull { it.exists() && it.isFile }
    }

    @Synchronized
    private fun getOrCreateConversation(
        modelPath: String,
        systemInstruction: String?,
        maxContextTokens: Int,
        conversationKey: String?,
        initialHistory: List<ChatMessageEntity>,
        enableVision: Boolean,
        enableAudio: Boolean
    ): Conversation {
        if (
            conversation != null &&
            engine != null &&
            loadedConversationKey == conversationKey &&
            loadedModelPath == modelPath &&
            loadedSystemInstruction == systemInstruction &&
            loadedMaxContextTokens == maxContextTokens &&
            loadedVisionEnabled == enableVision &&
            loadedAudioEnabled == enableAudio
        ) {
            return conversation!!
        }

        conversation?.close()
        engine?.close()

        val backend: Backend = Backend.CPU(CPU_THREADS)
        val cacheDir = if (modelPath.startsWith("/data/local/tmp/")) {
            context.getExternalFilesDir(null)?.absolutePath
        } else {
            null
        }

        val newEngine = createEngine(
            modelPath = modelPath,
            backend = backend,
            maxContextTokens = maxContextTokens,
            cacheDir = cacheDir,
            enableVision = enableVision,
            enableAudio = enableAudio
        )
        val newConversation = newEngine.createConversation(
            ConversationConfig(
                systemInstruction = systemInstruction
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Contents.of(it) },
                initialMessages = initialHistory.toOnDeviceMessages(),
                tools = toolProviders,
                samplerConfig = SamplerConfig(
                    40,
                    0.9,
                    0.8,
                    1
                ),
                automaticToolCalling = true
            )
        )

        engine = newEngine
        conversation = newConversation
        loadedConversationKey = conversationKey
        loadedModelPath = modelPath
        loadedSystemInstruction = systemInstruction
        loadedMaxContextTokens = maxContextTokens
        loadedVisionEnabled = enableVision
        loadedAudioEnabled = enableAudio
        return newConversation
    }

    private fun Message.extractText(): String {
        return contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
    }

    private fun mergeStreamingText(
        currentText: String,
        incomingText: String
    ): String {
        return when {
            currentText.isEmpty() -> incomingText
            incomingText.startsWith(currentText) -> incomingText
            currentText.endsWith(incomingText) -> currentText
            else -> currentText + incomingText
        }
    }

    private fun createEngine(
        modelPath: String,
        backend: Backend,
        maxContextTokens: Int,
        cacheDir: String?,
        enableVision: Boolean,
        enableAudio: Boolean
    ): Engine {
        val audioBackend = if (enableAudio) Backend.CPU() else null
        val engineConfigs = buildList {
            add(
                EngineConfig(
                    modelPath,
                    backend,
                    if (enableVision) Backend.GPU() else null,
                    audioBackend,
                    maxContextTokens,
                    cacheDir
                )
            )
            if (enableVision) {
                add(
                    EngineConfig(
                        modelPath,
                        backend,
                        Backend.CPU(CPU_THREADS),
                        audioBackend,
                        maxContextTokens,
                        cacheDir
                    )
                )
            }
        }

        var lastError: Throwable? = null
        engineConfigs.forEachIndexed { index, engineConfig ->
            try {
                return Engine(engineConfig).also { it.initialize() }
            } catch (throwable: Throwable) {
                lastError = throwable
                if (enableVision && index == 0) {
                    Log.w(
                        TAG,
                        "Backend vision GPU indisponible, nouvelle tentative en CPU.",
                        throwable
                    )
                }
            }
        }

        throw IllegalStateException(
            buildString {
                append("Impossible d'initialiser le moteur local")
                if (enableVision) {
                    append(" avec support image")
                }
                lastError?.message
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        append(". ")
                        append(it)
                    }
            },
            lastError
        )
    }

    private fun buildUserInputContents(
        text: String,
        imageUri: Uri?,
        audioClip: ByteArray?
    ): Contents {
        val contents = mutableListOf<Content>()

        if (imageUri != null) {
            contents += resolveImageContent(
                imageUri = imageUri,
                requireReadable = true
            ) ?: error("resolveImageContent returned null while requireReadable=true")
        }
        if (audioClip != null) {
            if (audioClip.isEmpty()) {
                throw IllegalArgumentException("Le clip audio utilisateur est vide.")
            }
            contents += Content.AudioBytes(audioClip)
        }
        if (text.isNotBlank()) {
            contents += Content.Text(text)
        }

        if (contents.isEmpty()) {
            throw IllegalArgumentException("Le message utilisateur est vide.")
        }

        return Contents.of(contents)
    }

    private fun List<ChatMessageEntity>.toOnDeviceMessages(): List<Message> {
        return mapNotNull { message ->
            when (message.role) {
                ChatMessageRole.USER -> message.toOnDeviceUserMessage()
                ChatMessageRole.ASSISTANT -> Message.model(message.content)
                else -> null
            }
        }
    }

    private fun ChatMessageEntity.toOnDeviceUserMessage(): Message? {
        val contents = mutableListOf<Content>()

        imageUri
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?.let { uri ->
                resolveImageContent(
                    imageUri = uri,
                    requireReadable = false
                )?.let(contents::add)
            }

        if (content.isNotBlank()) {
            contents += Content.Text(content)
        }

        return contents
            .takeIf { it.isNotEmpty() }
            ?.let { Message.user(Contents.of(it)) }
    }

    private fun resolveImageContent(
        imageUri: Uri,
        requireReadable: Boolean
    ): Content? {
        if (imageUri.scheme == ContentResolver.SCHEME_FILE || imageUri.scheme.isNullOrBlank()) {
            imageUri.path
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.exists() && it.isFile }
                ?.absolutePath
                ?.let { return Content.ImageFile(it) }
        }

        return runCatching {
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.isEmpty()) {
                    throw IllegalStateException("Le fichier image selectionne est vide.")
                }
                Content.ImageBytes(bytes)
            } ?: throw IllegalStateException("Impossible d'ouvrir l'image selectionnee.")
        }.getOrElse { throwable ->
            if (requireReadable) {
                throw IllegalStateException(
                    "Impossible de preparer l'image pour le modele local.",
                    throwable
                )
            }

            Log.w(TAG, "Image historique ignoree pour le contexte: $imageUri", throwable)
            null
        }
    }

    private fun buildStoreImportantMemoryToolProvider(): ToolProvider {
        return tool(object : OpenApiTool {
            override fun getToolDescriptionJsonString(): String {
                return JSONObject()
                    .put("name", "store_important_memory")
                    .put(
                        "description",
                        "A utiliser quand l'utilisateur donne un fait important, une preference, une relation entre entites, une decision ou toute information utile a long terme, afin de pouvoir la reutiliser dans une future conversation. Memorise en priorite les informations de profil durables comme le prenom, le nom, le surnom, les preferences stables, les relations et le materiel. Exemple: \"Mon prenom est Cyril\" doit etre memorise. Pour un fait direct sur l'utilisateur, entity_name peut etre omis dans facts et vaudra \"utilisateur\"."
                    )
                    .put(
                        "parameters",
                        JSONObject()
                            .put("type", "object")
                            .put(
                                "properties",
                                JSONObject()
                                    .put(
                                        "entities",
                                        JSONObject()
                                            .put("type", "array")
                                            .put(
                                                "items",
                                                JSONObject()
                                                    .put("type", "object")
                                                    .put(
                                                        "properties",
                                                        JSONObject()
                                                            .put("name", JSONObject().put("type", "string"))
                                                            .put("type", JSONObject().put("type", "string"))
                                                            .put("canonical_name", JSONObject().put("type", "string"))
                                                            .put("summary", JSONObject().put("type", "string"))
                                                    )
                                                    .put("required", JSONArray().put("name").put("type"))
                                            )
                                    )
                                    .put(
                                        "relations",
                                        JSONObject()
                                            .put("type", "array")
                                            .put(
                                                "items",
                                                JSONObject()
                                                    .put("type", "object")
                                                    .put(
                                                        "properties",
                                                        JSONObject()
                                                            .put("from_entity_name", JSONObject().put("type", "string"))
                                                            .put("relation_type", JSONObject().put("type", "string"))
                                                            .put("to_entity_name", JSONObject().put("type", "string"))
                                                            .put("confidence", JSONObject().put("type", "number"))
                                                    )
                                                    .put(
                                                        "required",
                                                        JSONArray()
                                                            .put("from_entity_name")
                                                            .put("relation_type")
                                                            .put("to_entity_name")
                                                    )
                                            )
                                    )
                                    .put(
                                        "facts",
                                        JSONObject()
                                            .put("type", "array")
                                            .put(
                                                "items",
                                                JSONObject()
                                                    .put("type", "object")
                                                    .put(
                                                        "properties",
                                                        JSONObject()
                                                            .put("entity_name", JSONObject().put("type", "string"))
                                                            .put("fact_type", JSONObject().put("type", "string"))
                                                            .put("value", JSONObject().put("type", "string"))
                                                            .put("confidence", JSONObject().put("type", "number"))
                                                    )
                                                    .put(
                                                        "required",
                                                        JSONArray().put("fact_type").put("value")
                                                    )
                                            )
                                    )
                            )
                    )
                    .toString()
            }

            override fun execute(paramsJsonString: String): String {
                emitToolDebugEvent(
                    toolName = "store_important_memory",
                    phase = ToolDebugPhase.CALL,
                    payload = paramsJsonString
                )

                return runBlocking {
                    runCatching {
                        val payload = paramsJsonString
                            .takeIf { it.isNotBlank() }
                            ?.let(::JSONObject)
                            ?: JSONObject()

                        val entities = payload.optJSONArray("entities").toMemoryEntityInputs()
                        val relations = payload.optJSONArray("relations").toMemoryRelationInputs()
                        val facts = payload.optJSONArray("facts").toMemoryFactInputs()

                        val result = memoryGraphRepository.storeImportantMemory(
                            entities = entities,
                            relations = relations,
                            facts = facts
                        )

                        JSONObject()
                            .put("ok", true)
                            .put("saved_entities", result.entityCount)
                            .put("saved_relations", result.relationCount)
                            .put("saved_facts", result.factCount)
                            .toString()
                    }.fold(
                        onSuccess = { resultJson ->
                            emitToolDebugEvent(
                                toolName = "store_important_memory",
                                phase = ToolDebugPhase.RESULT,
                                payload = resultJson
                            )
                            resultJson
                        },
                        onFailure = { throwable ->
                            val errorJson = JSONObject()
                                .put("ok", false)
                                .put("error", throwable.message ?: "Erreur inconnue")
                                .toString()
                            emitToolDebugEvent(
                                toolName = "store_important_memory",
                                phase = ToolDebugPhase.ERROR,
                                payload = errorJson
                            )
                            errorJson
                        }
                    )
                }
            }
        })
    }

    private fun buildSearchMemoryToolProvider(): ToolProvider {
        return tool(object : OpenApiTool {
            override fun getToolDescriptionJsonString(): String {
                return JSONObject()
                    .put("name", "search_memory")
                    .put(
                        "description",
                        "Recherche dans la memoire relationnelle les informations utilisateur deja connues avant de repondre a une question qui depend du contexte memorise. Utilisation obligatoire avant de dire que l'information utilisateur n'est pas connue, indisponible ou absente. Utilise une requete courte par mots-cles comme prenom, preference linux, NAS ou nom d'une personne."
                    )
                    .put(
                        "parameters",
                        JSONObject()
                            .put("type", "object")
                            .put(
                                "properties",
                                JSONObject()
                                    .put("query", JSONObject().put("type", "string"))
                                    .put("limit", JSONObject().put("type", "integer"))
                            )
                            .put("required", JSONArray().put("query"))
                    )
                    .toString()
            }

            override fun execute(paramsJsonString: String): String {
                emitToolDebugEvent(
                    toolName = "search_memory",
                    phase = ToolDebugPhase.CALL,
                    payload = paramsJsonString
                )

                return runBlocking {
                    runCatching {
                        val payload = paramsJsonString
                            .takeIf { it.isNotBlank() }
                            ?.let(::JSONObject)
                            ?: JSONObject()

                        val query = payload.optString("query").trim()
                        val limit = payload.optInt("limit", 5).coerceIn(1, 10)
                        val matches = if (query.isBlank()) {
                            emptyList()
                        } else {
                            memoryGraphRepository.searchMemory(query, limit)
                        }

                        JSONObject()
                            .put("ok", true)
                            .put("query", query)
                            .put("count", matches.size)
                            .put("answer_hint", buildSearchAnswerHint(query, matches))
                            .put("resolved_facts", buildResolvedFactsJson(query, matches))
                            .put(
                                "matches",
                                JSONArray().apply {
                                    matches.forEach { match ->
                                        put(
                                            JSONObject()
                                                .put("name", match.name)
                                                .put("type", match.type)
                                                .put("canonical_name", match.canonicalName)
                                                .put("summary", match.summary)
                                                .put(
                                                    "facts",
                                                    JSONArray().apply {
                                                        match.facts.forEach { fact ->
                                                            put(
                                                                JSONObject()
                                                                    .put("fact_type", fact.factType)
                                                                    .put("value", fact.value)
                                                                    .put("confidence", fact.confidence)
                                                            )
                                                        }
                                                    }
                                                )
                                                .put(
                                                    "relations",
                                                    JSONArray().apply {
                                                        match.relations.forEach { relation ->
                                                            put(
                                                                JSONObject()
                                                                    .put("relation_type", relation.relationType)
                                                                    .put("other_entity_name", relation.otherEntityName)
                                                                    .put("direction", relation.direction)
                                                                    .put("confidence", relation.confidence)
                                                            )
                                                        }
                                                    }
                                                )
                                        )
                                    }
                                }
                            )
                            .toString()
                    }.fold(
                        onSuccess = { resultJson ->
                            emitToolDebugEvent(
                                toolName = "search_memory",
                                phase = ToolDebugPhase.RESULT,
                                payload = resultJson
                            )
                            resultJson
                        },
                        onFailure = { throwable ->
                            val errorJson = JSONObject()
                                .put("ok", false)
                                .put("error", throwable.message ?: "Erreur inconnue")
                                .toString()
                            emitToolDebugEvent(
                                toolName = "search_memory",
                                phase = ToolDebugPhase.ERROR,
                                payload = errorJson
                            )
                            errorJson
                        }
                    )
                }
            }
        })
    }

    private fun emitToolDebugEvent(
        toolName: String,
        phase: ToolDebugPhase,
        payload: String
    ) {
        Log.d(
            TAG,
            "Tool $toolName ${phase.name.lowercase()} ${payload.take(300)}"
        )
        onToolDebugEvent?.invoke(
            ToolDebugEvent(
                toolName = toolName,
                phase = phase,
                payload = payload.take(4_000)
            )
        )
    }

    private fun buildSearchAnswerHint(
        query: String,
        matches: List<MemoryGraphRepository.SearchMatch>
    ): String {
        val normalizedQuery = query.normalizeMemoryLookup()
        val firstRelevantFact = matches
            .flatMap { match -> match.facts.map { fact -> match to fact } }
            .firstOrNull { (_, fact) ->
                val normalizedFactType = fact.factType.normalizeMemoryLookup()
                normalizedFactType.contains(normalizedQuery) ||
                    fact.value.normalizeMemoryLookup().contains(normalizedQuery) ||
                    normalizedQuery in setOf("prenom", "first_name") &&
                    normalizedFactType in setOf("prenom", "first_name")
            }

        return if (firstRelevantFact != null) {
            val (_, fact) = firstRelevantFact
            "L'information est connue: ${fact.factType} = ${fact.value}."
        } else {
            ""
        }
    }

    private fun buildResolvedFactsJson(
        query: String,
        matches: List<MemoryGraphRepository.SearchMatch>
    ): JSONArray {
        val normalizedQuery = query.normalizeMemoryLookup()
        return JSONArray().apply {
            matches.forEach { match ->
                match.facts.forEach { fact ->
                    val normalizedFactType = fact.factType.normalizeMemoryLookup()
                    val isRelevant = normalizedFactType.contains(normalizedQuery) ||
                        fact.value.normalizeMemoryLookup().contains(normalizedQuery) ||
                        normalizedQuery in setOf("prenom", "first_name") &&
                        normalizedFactType in setOf("prenom", "first_name")

                    if (isRelevant) {
                        put(
                            JSONObject()
                                .put("entity_name", match.name)
                                .put("entity_type", match.type)
                                .put("fact_type", fact.factType)
                                .put("value", fact.value)
                                .put("confidence", fact.confidence)
                        )
                    }
                }
            }
        }
    }

    private fun JSONArray?.toMemoryEntityInputs(): List<MemoryGraphRepository.EntityInput> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val type = item.optString("type").trim()
                if (name.isBlank() || type.isBlank()) continue

                add(
                    MemoryGraphRepository.EntityInput(
                        name = name,
                        type = type,
                        canonicalName = item.optTrimmedString("canonical_name"),
                        summary = item.optTrimmedString("summary")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toMemoryRelationInputs(): List<MemoryGraphRepository.RelationInput> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val fromEntityName = item.optString("from_entity_name").trim()
                val relationType = item.optString("relation_type").trim()
                val toEntityName = item.optString("to_entity_name").trim()
                if (fromEntityName.isBlank() || relationType.isBlank() || toEntityName.isBlank()) continue

                add(
                    MemoryGraphRepository.RelationInput(
                        fromEntityName = fromEntityName,
                        relationType = relationType,
                        toEntityName = toEntityName,
                        confidence = item.optNullableDouble("confidence")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toMemoryFactInputs(): List<MemoryGraphRepository.FactInput> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val entityName = item.optString("entity_name").trim().ifBlank { "utilisateur" }
                val factType = item.optString("fact_type").trim()
                val value = item.optString("value").trim()
                if (factType.isBlank() || value.isBlank()) continue

                add(
                    MemoryGraphRepository.FactInput(
                        entityName = entityName,
                        factType = factType,
                        value = value,
                        confidence = item.optNullableDouble("confidence")
                    )
                )
            }
        }
    }

    private fun JSONObject.optTrimmedString(key: String): String? {
        return optString(key)
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) {
            return null
        }
        return optDouble(key)
            .takeUnless { it.isNaN() }
    }

    private fun String.normalizeMemoryLookup(): String {
        return lowercase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("ë", "e")
            .replace("à", "a")
            .replace("â", "a")
            .replace("ä", "a")
            .replace("î", "i")
            .replace("ï", "i")
            .replace("ô", "o")
            .replace("ö", "o")
            .replace("ù", "u")
            .replace("û", "u")
            .replace("ü", "u")
            .trim()
    }
}
