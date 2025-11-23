package com.max.aiassistant.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Gestionnaire de permissions pour l'application
 *
 * Gère les demandes de permissions runtime, notamment pour RECORD_AUDIO
 */
class PermissionHelper(
    private val activity: ComponentActivity,
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit
) {
    private var permissionLauncher: ActivityResultLauncher<String>? = null

    /**
     * Initialise le launcher de permissions
     * Doit être appelé dans onCreate avant setContent
     */
    fun initPermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    /**
     * Vérifie si la permission RECORD_AUDIO est accordée
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Demande la permission RECORD_AUDIO
     */
    fun requestRecordAudioPermission() {
        permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
    }
}
