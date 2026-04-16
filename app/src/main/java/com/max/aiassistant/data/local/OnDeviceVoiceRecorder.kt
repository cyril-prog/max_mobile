package com.max.aiassistant.data.local

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

data class VoiceRecordingResult(
    val wavBytes: ByteArray,
    val durationMillis: Long
)

class OnDeviceVoiceRecorder {
    companion object {
        private const val TAG = "OnDeviceVoiceRecorder"
        const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BITS_PER_SAMPLE = 16
        private const val CHANNEL_COUNT = 1
    }

    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var recordingStartedAtMillis: Long = 0L
    private val isRecording = AtomicBoolean(false)
    private val pcmBuffer = ByteArrayOutputStream()

    fun isRecording(): Boolean = isRecording.get()

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording.get()) {
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        require(minBufferSize > 0) {
            "Configuration audio locale invalide pour l'enregistrement."
        }

        pcmBuffer.reset()
        audioRecord?.release()
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufferSize
        )

        val recorder = audioRecord
            ?.takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            ?: throw IllegalStateException("Impossible d'initialiser le micro.")

        recordingStartedAtMillis = System.currentTimeMillis()
        isRecording.set(true)
        recorder.startRecording()

        recordingJob = recorderScope.launch {
            val buffer = ByteArray(minBufferSize)
            while (isRecording.get()) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                when {
                    bytesRead > 0 -> pcmBuffer.write(buffer, 0, bytesRead)
                    bytesRead == 0 -> Unit
                    else -> {
                        Log.w(TAG, "Lecture audio interrompue: $bytesRead")
                        break
                    }
                }
            }
        }
    }

    suspend fun stopRecording(): VoiceRecordingResult {
        val recorder = audioRecord ?: throw IllegalStateException("Aucun enregistrement en cours.")
        if (!isRecording.compareAndSet(true, false)) {
            throw IllegalStateException("Aucun enregistrement en cours.")
        }

        runCatching {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
        }

        recordingJob?.let { joinAll(it) }
        recordingJob = null

        recorder.release()
        audioRecord = null

        val pcmBytes = pcmBuffer.toByteArray().also { pcmBuffer.reset() }
        if (pcmBytes.isEmpty()) {
            throw IllegalStateException("Le clip audio enregistre est vide.")
        }

        return VoiceRecordingResult(
            wavBytes = pcmBytes.toWaveFile(),
            durationMillis = System.currentTimeMillis() - recordingStartedAtMillis
        )
    }

    fun cleanup() {
        isRecording.set(false)
        runCatching {
            audioRecord?.stop()
        }
        runCatching {
            audioRecord?.release()
        }
        audioRecord = null
        recordingJob?.cancel()
        recordingJob = null
        pcmBuffer.reset()
        recorderScope.cancel()
    }

    private fun ByteArray.toWaveFile(): ByteArray {
        val pcmDataSize = size
        val waveFileSize = pcmDataSize + 36
        val byteRate = SAMPLE_RATE * CHANNEL_COUNT * BITS_PER_SAMPLE / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (waveFileSize and 0xff).toByte()
        header[5] = (waveFileSize shr 8 and 0xff).toByte()
        header[6] = (waveFileSize shr 16 and 0xff).toByte()
        header[7] = (waveFileSize shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = CHANNEL_COUNT.toByte()
        header[23] = 0
        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = (SAMPLE_RATE shr 8 and 0xff).toByte()
        header[26] = (SAMPLE_RATE shr 16 and 0xff).toByte()
        header[27] = (SAMPLE_RATE shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (CHANNEL_COUNT * BITS_PER_SAMPLE / 8).toByte()
        header[33] = 0
        header[34] = BITS_PER_SAMPLE.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataSize and 0xff).toByte()
        header[41] = (pcmDataSize shr 8 and 0xff).toByte()
        header[42] = (pcmDataSize shr 16 and 0xff).toByte()
        header[43] = (pcmDataSize shr 24 and 0xff).toByte()

        return header + this
    }
}
