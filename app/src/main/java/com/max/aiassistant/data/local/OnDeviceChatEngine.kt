package com.max.aiassistant.data.local

import android.app.ActivityManager
import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

class OnDeviceChatEngine(
    private val context: Context
) {
    companion object {
        private const val MIN_TOTAL_RAM_BYTES = 8L * 1024L * 1024L * 1024L
        private const val MIN_AVAILABLE_RAM_BYTES = 2_200L * 1024L * 1024L
        private const val MAX_CONTEXT_TOKENS = 4096
        private const val CPU_THREADS = 4
    }

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedModelPath: String? = null
    private var loadedSystemInstruction: String? = null

    fun getAvailability(): OnDeviceModelAvailability {
        val modelFile = resolveModelFile()
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
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val availability = getAvailability()
        val modelPath = availability.modelPath
            ?: throw IllegalStateException(availability.statusMessage)
        val runtimeReadiness = getRuntimeReadiness()
        if (!runtimeReadiness.canRun) {
            throw IllegalStateException(runtimeReadiness.reason)
        }

        val activeConversation = getOrCreateConversation(modelPath, systemInstruction)
        val response = activeConversation.sendMessage(userMessage, emptyMap<String, Any>())
        response.extractText()
    }

    suspend fun generateResponseStreaming(
        userMessage: String,
        systemInstruction: String? = null,
        onPartialResponse: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val availability = getAvailability()
        val modelPath = availability.modelPath
            ?: throw IllegalStateException(availability.statusMessage)
        val runtimeReadiness = getRuntimeReadiness()
        if (!runtimeReadiness.canRun) {
            throw IllegalStateException(runtimeReadiness.reason)
        }

        val activeConversation = getOrCreateConversation(modelPath, systemInstruction)

        suspendCancellableCoroutine { continuation ->
            var latestText = ""

            continuation.invokeOnCancellation {
                activeConversation.cancelProcess()
            }

            activeConversation.sendMessageAsync(
                userMessage,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        val partialText = message.extractText()
                        if (partialText.isBlank()) {
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
        loadedModelPath = null
        loadedSystemInstruction = null
    }

    private fun resolveModelFile(): File? {
        val externalRoot = context.getExternalFilesDir(null)
        val candidatePaths = buildList {
            if (externalRoot != null) {
                add(File(externalRoot, "models/${OnDeviceModelManager.MODEL_FILE_NAME}").absolutePath)
            }

            add("/data/local/tmp/llm/${OnDeviceModelManager.MODEL_FILE_NAME}")
            if (externalRoot != null) {
                add(File(externalRoot, "models/gemma-4-E4B-it.litertlm").absolutePath)
                add(File(externalRoot, "models/gemma-4-E2B-it.litertlm").absolutePath)
            }
            add("/data/local/tmp/llm/gemma-4-E4B-it.litertlm")
            add("/data/local/tmp/llm/gemma-4-E2B-it.litertlm")
        }

        return candidatePaths
            .asSequence()
            .map(::File)
            .firstOrNull { it.exists() && it.isFile }
    }

    @Synchronized
    private fun getOrCreateConversation(
        modelPath: String,
        systemInstruction: String?
    ): Conversation {
        if (
            conversation != null &&
            engine != null &&
            loadedModelPath == modelPath &&
            loadedSystemInstruction == systemInstruction
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

        val engineConfig = EngineConfig(
            modelPath,
            backend,
            null,
            null,
            MAX_CONTEXT_TOKENS,
            cacheDir
        )
        val newEngine = Engine(engineConfig).also { it.initialize() }
        val newConversation = newEngine.createConversation(
            ConversationConfig(
                systemInstruction = systemInstruction
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Contents.of(it) },
                initialMessages = emptyList(),
                tools = emptyList<ToolProvider>(),
                samplerConfig = SamplerConfig(
                    40,
                    0.9,
                    0.8,
                    1
                ),
                automaticToolCalling = false
            )
        )

        engine = newEngine
        conversation = newConversation
        loadedModelPath = modelPath
        loadedSystemInstruction = systemInstruction
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
            currentText.isBlank() -> incomingText
            incomingText.startsWith(currentText) -> incomingText
            currentText.endsWith(incomingText) -> currentText
            else -> currentText + incomingText
        }
    }
}
