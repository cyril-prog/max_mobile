package com.max.aiassistant.data.realtime

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Gestionnaire audio pour l'API Realtime
 *
 * Gère :
 * - Enregistrement audio via AudioRecord (PCM16, 24kHz, mono)
 * - Conversion en Base64 pour envoi à l'API
 * - Lecture audio via AudioTrack (réception depuis l'API)
 * - Décodage Base64 vers PCM16
 */
class RealtimeAudioManager(
    private val onAudioChunk: (String) -> Unit // Callback pour envoyer les chunks audio
) {
    private val TAG = "RealtimeAudioManager"

    // Configuration audio pour l'API Realtime (PCM16, 24kHz, mono)
    companion object {
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    // Buffer pour l'enregistrement
    private val recordBufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG_IN,
        AUDIO_FORMAT
    ) * BUFFER_SIZE_MULTIPLIER

    // Buffer pour la lecture
    private val playBufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG_OUT,
        AUDIO_FORMAT
    ) * BUFFER_SIZE_MULTIPLIER

    /**
     * Démarre l'enregistrement audio
     * Capture l'audio du micro et envoie les chunks via le callback
     */
    fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "Enregistrement déjà en cours")
            return
        }

        try {
            // Initialise AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                recordBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Échec d'initialisation d'AudioRecord")
                return
            }

            // Initialise AudioTrack pour la lecture
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .setEncoding(AUDIO_FORMAT)
                        .build()
                )
                .setBufferSizeInBytes(playBufferSize)
                .build()

            audioTrack?.play()

            // Démarre l'enregistrement
            audioRecord?.startRecording()
            isRecording = true

            Log.d(TAG, "Enregistrement audio démarré")

            // Lance la capture audio dans une coroutine
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                captureAudio()
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission RECORD_AUDIO manquante", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du démarrage de l'enregistrement", e)
        }
    }

    /**
     * Arrête l'enregistrement audio
     */
    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Aucun enregistrement en cours")
            return
        }

        isRecording = false
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            Log.d(TAG, "Enregistrement audio arrêté")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'arrêt de l'enregistrement", e)
        }
    }

    /**
     * Capture l'audio et envoie les chunks en Base64
     */
    private suspend fun captureAudio() {
        val buffer = ShortArray(recordBufferSize / 2) // ShortArray car PCM16

        while (isRecording) {
            try {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (readResult > 0) {
                    // Convertit ShortArray en ByteArray (PCM16)
                    val byteBuffer = ByteBuffer.allocate(readResult * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readResult) {
                        byteBuffer.putShort(buffer[i])
                    }

                    // Encode en Base64
                    val base64Audio = Base64.encodeToString(
                        byteBuffer.array(),
                        Base64.NO_WRAP
                    )

                    // Envoie le chunk via le callback
                    onAudioChunk(base64Audio)
                } else {
                    Log.w(TAG, "Échec de lecture audio: $readResult")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la capture audio", e)
                break
            }
        }
    }

    /**
     * Joue un chunk audio reçu de l'API (Base64 encoded PCM16)
     */
    fun playAudioChunk(base64Audio: String) {
        try {
            // Décode le Base64
            val audioData = Base64.decode(base64Audio, Base64.NO_WRAP)

            // Convertit ByteArray en ShortArray pour AudioTrack
            val shortBuffer = ShortArray(audioData.size / 2)
            val byteBuffer = ByteBuffer.wrap(audioData)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            for (i in shortBuffer.indices) {
                shortBuffer[i] = byteBuffer.short
            }

            // Joue l'audio
            audioTrack?.write(shortBuffer, 0, shortBuffer.size)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture audio", e)
        }
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        stopRecording()
    }
}
