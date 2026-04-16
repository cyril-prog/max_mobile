package com.max.aiassistant.data.local

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidTextToSpeechManager(
    private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private var pendingReadyContinuations =
        mutableListOf<kotlin.coroutines.Continuation<TextToSpeech>>()
    private var isInitializing = false

    suspend fun speak(
        text: String,
        locale: Locale = Locale.FRANCE,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            return
        }

        val engine = ensureReady()
        val languageResult = engine.setLanguage(locale)
        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            engine.setLanguage(Locale.getDefault())
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            val utteranceId = UUID.randomUUID().toString()
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStart?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    if (continuation.isActive) {
                        onDone?.invoke()
                        continuation.resume(Unit)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("La synthese vocale a echoue.")
                        )
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("La synthese vocale a echoue (code $errorCode).")
                        )
                    }
                }
            })

            val result = engine.speak(
                normalizedText,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                utteranceId
            )

            if (result == TextToSpeech.ERROR && continuation.isActive) {
                continuation.resumeWithException(
                    IllegalStateException("Impossible de lancer la synthese vocale.")
                )
            }

            continuation.invokeOnCancellation {
                stop()
            }
        }
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitializing = false
        pendingReadyContinuations = mutableListOf()
    }

    private suspend fun ensureReady(): TextToSpeech {
        textToSpeech?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            pendingReadyContinuations.add(continuation)
            if (isInitializing) {
                return@suspendCancellableCoroutine
            }

            isInitializing = true
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                val readyEngine = textToSpeech
                val continuations = pendingReadyContinuations.toList()
                pendingReadyContinuations.clear()
                isInitializing = false

                if (status == TextToSpeech.SUCCESS && readyEngine != null) {
                    continuations.forEach { pending ->
                        pending.resume(readyEngine)
                    }
                } else {
                    val error = IllegalStateException("Le moteur de synthese vocale Android est indisponible.")
                    readyEngine?.shutdown()
                    textToSpeech = null
                    continuations.forEach { pending ->
                        pending.resumeWithException(error)
                    }
                }
            }
        }
    }
}
