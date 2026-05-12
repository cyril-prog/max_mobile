package com.max.aiassistant.data.realtime

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import java.util.concurrent.LinkedBlockingQueue

/**
 * Gestionnaire audio pour l'API Realtime
 *
 * Gère :
 * - Enregistrement audio via AudioRecord (PCM16, 24kHz, mono)
 * - Conversion en Base64 pour envoi à l'API
 * - Lecture audio via AudioTrack (réception depuis l'API)
 * - Décodage Base64 vers PCM16
 * - Suppression d'écho : mode MODE_IN_COMMUNICATION + AEC hardware + pause micro pendant le playback IA
 *
 * Timing correct du micro :
 *   startSpeaking()  → appelé sur chaque ResponseAudioDelta (micro en pause)
 *   markAudioDone()  → appelé sur ResponseAudioDone (signale que l'envoi réseau est terminé)
 *   Le micro ne reprend QUE quand la queue de playback est entièrement vidée,
 *   afin d'éviter que le micro capte la fin de la réponse IA encore en cours de lecture.
 */
class RealtimeAudioManager(
    private val context: Context,
    private val onAudioChunk: (String) -> Unit, // Callback pour envoyer les chunks audio
    private val onBargeInDetected: (() -> Unit)? = null,
    private val pauseInputWhileSpeaking: Boolean = true,
    private val onSpeakingFinished: (() -> Unit)? = null // Callback quand le playback est vraiment terminé
) {
    private val TAG = "RealtimeAudioManager"

    // Configuration audio pour l'API Realtime (PCM16, 24kHz, mono)
    companion object {
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
        private const val PLAY_BUFFER_SIZE_MULTIPLIER = 4
        private const val PLAY_PREBUFFER_MS = 120L
        private const val BARGE_IN_RMS_THRESHOLD = 850.0
        private const val BARGE_IN_PEAK_THRESHOLD = 4200
        private const val BARGE_IN_CONSECUTIVE_CHUNKS = 1
        private const val BARGE_IN_START_GRACE_MS = 150L
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private val playbackLock = Any()
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private var audioChunkQueue = LinkedBlockingQueue<ShortArray>()
    private val prebufferedAudioChunks = ArrayDeque<ShortArray>()
    private var prebufferedSampleCount = 0
    private var playbackStarted = false
    private var playbackHeadStartSample = 0L
    private var writtenPlaybackSamples = 0L
    private var isRecording = false
    private var isPlaybackActive = false

    // Flag : true pendant que l'IA joue sa réponse audio
    // Quand true, les chunks micro sont capturés mais NOT envoyés au serveur
    @Volatile
    private var isSpeaking = false

    // Flag : true quand ResponseAudioDone a été reçu (réseau fini)
    // Le micro reprend seulement quand ce flag est true ET la queue est vide
    @Volatile
    private var pendingAudioDone = false
    @Volatile
    private var speakingStartedAtMs = 0L
    private var bargeInLoudChunkCount = 0

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
    ) * PLAY_BUFFER_SIZE_MULTIPLIER

    private val playPrebufferSampleCount = (SAMPLE_RATE * PLAY_PREBUFFER_MS / 1000L).toInt()

    /**
     * Démarre l'enregistrement audio.
     * Configure AudioManager en MODE_IN_COMMUNICATION pour activer l'AEC hardware.
     */
    fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "Enregistrement déjà en cours")
            return
        }

        try {
            // ── Mode téléphonie : active l'AEC hardware ──────────────────
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            Log.d(TAG, "AudioManager mode → MODE_IN_COMMUNICATION")

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

            // ── Active l'AEC software en fallback si le hardware le supporte ──
            val audioSessionId = audioRecord!!.audioSessionId
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioSessionId)
                echoCanceler?.enabled = true
                Log.d(TAG, "AcousticEchoCanceler activé (session $audioSessionId)")
            } else {
                Log.w(TAG, "AcousticEchoCanceler non disponible sur cet appareil")
            }

            // Initialise AudioTrack pour la lecture (mode non-bloquant)
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .setEncoding(AUDIO_FORMAT)
                        .build()
                )
                .setBufferSizeInBytes(playBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // Démarre l'enregistrement
            audioRecord?.startRecording()
            isRecording = true
            isPlaybackActive = true
            synchronized(playbackLock) {
                isSpeaking = false
                pendingAudioDone = false
                speakingStartedAtMs = 0L
                resetPlaybackBuffering()
                bargeInLoudChunkCount = 0
            }

            Log.d(TAG, "Enregistrement audio démarré")

            // Lance la capture audio dans une coroutine
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                captureAudio()
            }

            // Lance le playback des chunks dans une coroutine dédiée
            playbackJob = CoroutineScope(Dispatchers.IO).launch {
                playbackAudioChunks()
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission RECORD_AUDIO manquante", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du démarrage de l'enregistrement", e)
        }
    }

    /**
     * Arrête l'enregistrement audio et restaure le mode audio normal.
     */
    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Aucun enregistrement en cours")
            return
        }

        isRecording = false
        isPlaybackActive = false
        synchronized(playbackLock) {
            isSpeaking = false
            pendingAudioDone = false
            speakingStartedAtMs = 0L
            resetPlaybackBuffering()
            bargeInLoudChunkCount = 0
        }
        recordingJob?.cancel()
        playbackJob?.cancel()

        try {
            echoCanceler?.release()
            echoCanceler = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            // Vide la queue
            audioChunkQueue.clear()

            // ── Restaure le mode audio normal ────────────────────────────
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            Log.d(TAG, "AudioManager mode → MODE_NORMAL")

            Log.d(TAG, "Enregistrement audio arrêté")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'arrêt de l'enregistrement", e)
        }
    }

    /**
     * Capture l'audio et envoie les chunks en Base64.
     * Les chunks sont silencieusement ignorés (non envoyés) pendant que l'IA parle.
     */
    private suspend fun captureAudio() {
        val buffer = ShortArray(recordBufferSize / 2) // ShortArray car PCM16

        while (isRecording) {
            try {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (readResult > 0) {
                    if (pauseInputWhileSpeaking && isSpeaking && !shouldAllowBargeIn(buffer, readResult)) {
                        continue
                    }

                    // ── Ne pas envoyer pendant le playback IA ────────────
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
     * Signale le début du playback IA — suspend l'envoi micro.
     */
    fun startSpeaking() {
        synchronized(playbackLock) {
            if (isSpeaking) {
                return
            }

            isSpeaking = true
            speakingStartedAtMs = System.currentTimeMillis()
            resetPlaybackBuffering()
            bargeInLoudChunkCount = 0
        }
        Log.d(TAG, "IA commence à parler → micro en pause")
    }

    /**
     * Signale que le serveur a fini d'envoyer les chunks audio (ResponseAudioDone).
     * Le micro reprendra seulement quand la queue de playback sera entièrement vidée.
     */
    fun interruptPlayback() {
        synchronized(playbackLock) {
            audioChunkQueue.clear()
            pendingAudioDone = false
            isSpeaking = false
            speakingStartedAtMs = 0L
            resetPlaybackBuffering()
            bargeInLoudChunkCount = 0

            try {
                audioTrack?.pause()
                audioTrack?.flush()
                Log.d(TAG, "Playback IA interrompu localement")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'interruption du playback IA", e)
            }
        }
    }

    fun markAudioDone() {
        synchronized(playbackLock) {
            pendingAudioDone = true
        }
        Log.d(TAG, "ResponseAudioDone reçu → en attente vidage de la queue avant de réactiver le micro")
    }

    /**
     * Ajoute un chunk audio à la queue de playback (Base64 encoded PCM16)
     */
    private fun shouldAllowBargeIn(buffer: ShortArray, sampleCount: Int): Boolean {
        val elapsedSinceSpeakingStarted = System.currentTimeMillis() - speakingStartedAtMs
        if (elapsedSinceSpeakingStarted < BARGE_IN_START_GRACE_MS) {
            bargeInLoudChunkCount = 0
            return false
        }

        val rms = calculateRms(buffer, sampleCount)
        val peak = calculatePeak(buffer, sampleCount)
        if (rms >= BARGE_IN_RMS_THRESHOLD || peak >= BARGE_IN_PEAK_THRESHOLD) {
            bargeInLoudChunkCount += 1
        } else {
            bargeInLoudChunkCount = 0
        }

        if (bargeInLoudChunkCount >= BARGE_IN_CONSECUTIVE_CHUNKS) {
            Log.d(TAG, "Barge-in local detecte rms=$rms peak=$peak")
            onBargeInDetected?.invoke()
            return true
        }

        return false
    }

    private fun calculateRms(buffer: ShortArray, sampleCount: Int): Double {
        var sumSquares = 0.0
        for (index in 0 until sampleCount) {
            val sample = buffer[index].toDouble()
            sumSquares += sample * sample
        }
        return kotlin.math.sqrt(sumSquares / sampleCount.coerceAtLeast(1))
    }

    private fun calculatePeak(buffer: ShortArray, sampleCount: Int): Int {
        var peak = 0
        for (index in 0 until sampleCount) {
            val absoluteValue = kotlin.math.abs(buffer[index].toInt())
            if (absoluteValue > peak) {
                peak = absoluteValue
            }
        }
        return peak
    }

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

            // Ajoute le chunk à la queue (non-bloquant)
            audioChunkQueue.offer(shortBuffer)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du décodage audio", e)
        }
    }

    /**
     * Consomme la queue de chunks audio et les joue de manière séquentielle.
     * Ne remet le micro actif (isSpeaking = false) que quand la queue est entièrement vidée
     * ET que pendingAudioDone == true (signal que le serveur a fini d'envoyer).
     */
    private suspend fun playbackAudioChunks() {
        while (isPlaybackActive) {
            try {
                // Attend et récupère le prochain chunk (bloquant jusqu'à disponibilité)
                val chunk = audioChunkQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)

                if (chunk != null) {
                    enqueueOrPlayChunk(chunk)
                } else {
                    var speakingFinished = false

                    synchronized(playbackLock) {
                        if (!playbackStarted && pendingAudioDone && prebufferedAudioChunks.isNotEmpty()) {
                            startBufferedPlayback()
                        }

                        // Queue vide : si le serveur a signalé la fin, on reprend le micro
                        if (pendingAudioDone && isSpeaking && prebufferedAudioChunks.isEmpty() && isPlaybackDrained()) {
                            isSpeaking = false
                            pendingAudioDone = false
                            pausePlaybackUntilNextResponse()
                            speakingFinished = true
                        }
                    }

                    if (speakingFinished) {
                        Log.d(TAG, "Queue vidée après ResponseAudioDone → micro actif")
                        onSpeakingFinished?.invoke()
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Playback interrompu")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du playback audio", e)
            }
        }
    }

    private fun enqueueOrPlayChunk(chunk: ShortArray) {
        synchronized(playbackLock) {
            if (!playbackStarted) {
                prebufferedAudioChunks.add(chunk)
                prebufferedSampleCount += chunk.size

                if (prebufferedSampleCount >= playPrebufferSampleCount || pendingAudioDone) {
                    startBufferedPlayback()
                }
                return
            }

            writeAudioChunk(chunk)
        }
    }

    private fun startBufferedPlayback() {
        if (playbackStarted) {
            return
        }

        playbackStarted = true
        playbackHeadStartSample = currentPlaybackHeadSample()
        writtenPlaybackSamples = 0L
        audioTrack?.play()

        while (prebufferedAudioChunks.isNotEmpty()) {
            writeAudioChunk(prebufferedAudioChunks.removeFirst())
        }
        prebufferedSampleCount = 0
    }

    private fun writeAudioChunk(chunk: ShortArray) {
        val written = audioTrack?.write(chunk, 0, chunk.size) ?: 0

        if (written > 0) {
            writtenPlaybackSamples += written.toLong()
        } else if (written < 0) {
            Log.e(TAG, "Erreur d'écriture AudioTrack: $written")
        }
    }

    private fun isPlaybackDrained(): Boolean {
        if (!playbackStarted) {
            return true
        }

        val currentPlaybackSample = currentPlaybackHeadSample()
        val playedSamples = if (currentPlaybackSample >= playbackHeadStartSample) {
            currentPlaybackSample - playbackHeadStartSample
        } else {
            (1L shl 32) - playbackHeadStartSample + currentPlaybackSample
        }
        return playedSamples >= writtenPlaybackSamples
    }

    private fun currentPlaybackHeadSample(): Long {
        return Integer.toUnsignedLong(audioTrack?.playbackHeadPosition ?: 0)
    }

    private fun pausePlaybackUntilNextResponse() {
        resetPlaybackBuffering()

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise en pause du playback IA", e)
        }
    }

    private fun resetPlaybackBuffering() {
        prebufferedAudioChunks.clear()
        prebufferedSampleCount = 0
        playbackStarted = false
        playbackHeadStartSample = 0L
        writtenPlaybackSamples = 0L
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        stopRecording()
    }
}
