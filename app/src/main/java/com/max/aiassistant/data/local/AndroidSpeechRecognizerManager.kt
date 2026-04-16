package com.max.aiassistant.data.local

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class AndroidSpeechRecognizerManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: RecognitionListener? = null
    private var isListening = false
    private var currentSessionId = 0
    private var hasDeliveredFinalResult = false
    private var hasCompletedCurrentSession = false
    private var cancellationRequested = false
    private var lastPartialTranscript = ""

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(appContext)
    }

    fun isListening(): Boolean = isListening

    fun startListening(
        locale: Locale = Locale.FRANCE,
        onReady: () -> Unit,
        onPartialTranscript: (String) -> Unit,
        onFinalTranscript: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isRecognitionAvailable()) {
            onError("La reconnaissance vocale Android n'est pas disponible sur cet appareil.")
            return
        }

        destroyRecognizer()
        currentSessionId += 1
        val sessionId = currentSessionId
        hasDeliveredFinalResult = false
        hasCompletedCurrentSession = false
        cancellationRequested = false
        lastPartialTranscript = ""
        Log.d(TAG, "Demarrage reconnaissance vocale session=$sessionId locale=${locale.toLanguageTag()}")

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                Log.d(TAG, "Reconnaissance prete session=$sessionId")
                onReady()
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Debut de parole session=$sessionId")
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                isListening = false
                Log.d(TAG, "Fin de parole detectee session=$sessionId")
            }

            override fun onError(error: Int) {
                isListening = false
                val userMessage = error.toUserMessage()
                val ignoredLateError = hasCompletedCurrentSession || hasDeliveredFinalResult
                val ignoredCancellationError =
                    cancellationRequested && error == SpeechRecognizer.ERROR_CLIENT

                Log.w(
                    TAG,
                    "Erreur reconnaissance session=$sessionId code=$error " +
                        "ignoredLate=$ignoredLateError ignoredCancellation=$ignoredCancellationError"
                )

                if (ignoredLateError || ignoredCancellationError) {
                    destroyRecognizer()
                    return
                }

                hasCompletedCurrentSession = true
                destroyRecognizer()
                onError(userMessage)
            }

            override fun onResults(results: Bundle?) {
                if (hasCompletedCurrentSession) {
                    Log.d(TAG, "Resultat final ignore car session deja terminee session=$sessionId")
                    destroyRecognizer()
                    return
                }

                isListening = false
                val transcript = extractBestTranscript(results)
                    .ifBlank { lastPartialTranscript.trim() }

                Log.d(
                    TAG,
                    "Resultat final session=$sessionId longueur=${transcript.length} " +
                        "preview='${transcript.previewForLog()}'"
                )

                if (transcript.isBlank()) {
                    hasCompletedCurrentSession = true
                    destroyRecognizer()
                    onError("Je n'ai pas reussi a comprendre ce qui a ete dit.")
                } else {
                    hasDeliveredFinalResult = true
                    hasCompletedCurrentSession = true
                    destroyRecognizer()
                    onFinalTranscript(transcript)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                extractBestTranscript(partialResults)
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { transcript ->
                        lastPartialTranscript = transcript
                        Log.d(
                            TAG,
                            "Resultat partiel session=$sessionId longueur=${transcript.length} " +
                                "preview='${transcript.previewForLog()}'"
                        )
                        onPartialTranscript(transcript)
                    }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        listener = recognitionListener
        speechRecognizer = recognizer.apply {
            setRecognitionListener(recognitionListener)
            startListening(intent)
        }
    }

    fun stopListening() {
        Log.d(TAG, "Demande de finalisation de l'ecoute session=$currentSessionId")
        speechRecognizer?.stopListening()
    }

    fun cancelListening() {
        isListening = false
        cancellationRequested = true
        hasCompletedCurrentSession = true
        Log.d(TAG, "Annulation de l'ecoute session=$currentSessionId")
        destroyRecognizer()
    }

    fun destroy() {
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        isListening = false
        speechRecognizer?.setRecognitionListener(null)
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        listener = null
        cancellationRequested = false
        lastPartialTranscript = ""
    }

    private fun extractBestTranscript(results: Bundle?): String {
        return results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
    }

    private fun Int.toUserMessage(): String {
        return when (this) {
            SpeechRecognizer.ERROR_AUDIO -> "Le micro a rencontre une erreur audio."
            SpeechRecognizer.ERROR_CLIENT -> "La reconnaissance vocale a ete interrompue."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "La permission micro est manquante."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "La reconnaissance vocale Android a besoin du reseau pour fonctionner ici."
            SpeechRecognizer.ERROR_NO_MATCH -> "Je n'ai pas reconnu de phrase exploitable."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "La reconnaissance vocale est deja occupee."
            SpeechRecognizer.ERROR_SERVER -> "Le service de reconnaissance vocale a rencontre une erreur."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucune parole detectee."
            else -> "La reconnaissance vocale a echoue."
        }
    }

    private fun String.previewForLog(): String {
        return take(80).replace('\n', ' ')
    }

    private companion object {
        const val TAG = "VoiceRecognizer"
    }
}
