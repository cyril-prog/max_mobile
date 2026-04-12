package com.max.aiassistant.data.local

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

sealed interface OnDeviceModelProvisioningState {
    data class Checking(val message: String) : OnDeviceModelProvisioningState
    data class Downloading(
        val message: String,
        val downloadedBytes: Long,
        val totalBytes: Long?
    ) : OnDeviceModelProvisioningState
    data class Verifying(val message: String) : OnDeviceModelProvisioningState
    data class Ready(val modelPath: String, val message: String) : OnDeviceModelProvisioningState
    data class Error(val message: String) : OnDeviceModelProvisioningState
}

class OnDeviceModelManager(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun prepareModel(
        modelVariant: OnDeviceModelVariant,
        forceDownload: Boolean = false,
        onStateChanged: (OnDeviceModelProvisioningState) -> Unit
    ): OnDeviceModelAvailability = withContext(Dispatchers.IO) {
        onStateChanged(
            OnDeviceModelProvisioningState.Checking(
                "Verification du modele local ${modelVariant.storageFileName}..."
            )
        )

        val appModelFile = getAppModelFile(modelVariant)
        val devModelFile = getDevModelFile(modelVariant)

        cleanupManagedAppModels(modelVariant)

        if (!forceDownload) {
            if (isValidModelFile(appModelFile, modelVariant, onStateChanged)) {
                return@withContext OnDeviceModelAvailability(
                    isAvailable = true,
                    modelPath = appModelFile.absolutePath,
                    statusMessage = buildReadyStatusMessage(appModelFile.name)
                ).also {
                    onStateChanged(
                        OnDeviceModelProvisioningState.Ready(
                            modelPath = appModelFile.absolutePath,
                            message = it.statusMessage
                        )
                    )
                }
            }

            if (isValidModelFile(devModelFile, modelVariant, onStateChanged)) {
                return@withContext OnDeviceModelAvailability(
                    isAvailable = true,
                    modelPath = devModelFile.absolutePath,
                    statusMessage = buildReadyStatusMessage(devModelFile.name)
                ).also {
                    onStateChanged(
                        OnDeviceModelProvisioningState.Ready(
                            modelPath = devModelFile.absolutePath,
                            message = it.statusMessage
                        )
                    )
                }
            }
        }

        downloadToAppStorage(modelVariant, appModelFile, onStateChanged)

        if (!isValidModelFile(appModelFile, modelVariant, onStateChanged)) {
            appModelFile.delete()
            throw IOException("Le modele telecharge est invalide ou corrompu.")
        }

        OnDeviceModelAvailability(
            isAvailable = true,
            modelPath = appModelFile.absolutePath,
            statusMessage = buildReadyStatusMessage(appModelFile.name)
        ).also {
            onStateChanged(
                OnDeviceModelProvisioningState.Ready(
                    modelPath = appModelFile.absolutePath,
                    message = it.statusMessage
                )
            )
        }
    }

    fun getCachedAvailability(modelVariant: OnDeviceModelVariant): OnDeviceModelAvailability {
        val appModelFile = getAppModelFile(modelVariant)
        val devModelFile = getDevModelFile(modelVariant)

        return when {
            appModelFile.exists() -> OnDeviceModelAvailability(
                isAvailable = false,
                modelPath = appModelFile.absolutePath,
                statusMessage = "Modele detecte: ${appModelFile.name}. Verification en cours..."
            )

            devModelFile.exists() -> OnDeviceModelAvailability(
                isAvailable = false,
                modelPath = devModelFile.absolutePath,
                statusMessage = "Modele detecte: ${devModelFile.name}. Verification en cours..."
            )

            else -> OnDeviceModelAvailability(
                isAvailable = false,
                statusMessage = "Preparation du modele local en cours."
            )
        }
    }

    fun getAppModelFile(modelVariant: OnDeviceModelVariant): File {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        return File(root, "models/${modelVariant.storageFileName}").apply {
            parentFile?.mkdirs()
        }
    }

    fun getDevModelFile(modelVariant: OnDeviceModelVariant): File =
        File("/data/local/tmp/llm/${modelVariant.storageFileName}")

    private fun downloadToAppStorage(
        modelVariant: OnDeviceModelVariant,
        destinationFile: File,
        onStateChanged: (OnDeviceModelProvisioningState) -> Unit
    ) {
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.part")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val request = Request.Builder()
            .url(modelVariant.downloadUrl)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Telechargement du modele impossible (${response.code}).")
            }

            val body = response.body ?: throw IOException("Reponse de telechargement vide.")
            val totalBytes = body.contentLength().takeIf { it > 0L }
            var downloadedBytes = 0L
            var lastEmission = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastEmission > 250L) {
                            lastEmission = now
                            onStateChanged(
                                OnDeviceModelProvisioningState.Downloading(
                                    message = buildDownloadMessage(modelVariant, downloadedBytes, totalBytes),
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            )
                        }
                    }
                    output.fd.sync()
                }
            }
        }

        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        if (!tempFile.renameTo(destinationFile)) {
            throw IOException("Impossible de finaliser le fichier du modele.")
        }
    }

    private fun isValidModelFile(
        file: File,
        modelVariant: OnDeviceModelVariant,
        onStateChanged: (OnDeviceModelProvisioningState) -> Unit
    ): Boolean {
        if (!file.exists() || !file.isFile) return false

        onStateChanged(
            OnDeviceModelProvisioningState.Verifying(
                "Verification de l'integrite du modele ${file.name}..."
            )
        )

        val digest = sha256(file)
        return if (digest.equals(modelVariant.expectedSha256, ignoreCase = true)) {
            true
        } else {
            if (file == getAppModelFile(modelVariant)) {
                file.delete()
            }
            false
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            "%02X".format(byte)
        }
    }

    private fun cleanupManagedAppModels(selectedVariant: OnDeviceModelVariant) {
        OnDeviceModelVariant.entries
            .filter { it != selectedVariant }
            .map(::getAppModelFile)
            .forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
    }

    private fun buildDownloadMessage(
        modelVariant: OnDeviceModelVariant,
        downloadedBytes: Long,
        totalBytes: Long?
    ): String {
        val downloadedMb = downloadedBytes / (1024f * 1024f)
        val totalMb = totalBytes?.div(1024f * 1024f)
        return if (totalMb != null) {
            "Telechargement de ${modelVariant.storageFileName}: ${downloadedMb.toInt()} / ${totalMb.toInt()} Mo"
        } else {
            "Telechargement de ${modelVariant.storageFileName}: ${downloadedMb.toInt()} Mo"
        }
    }

    private fun buildReadyStatusMessage(fileName: String): String {
        return "Modele telecharge et verifie: $fileName. Pret pour initialisation locale."
    }
}
