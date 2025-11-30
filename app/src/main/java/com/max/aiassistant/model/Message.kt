package com.max.aiassistant.model

import android.net.Uri

/**
 * Représente un message dans la conversation avec Max
 *
 * @property id Identifiant unique du message
 * @property content Le texte du message
 * @property isFromUser true si le message vient de l'utilisateur, false si c'est Max
 * @property timestamp Horodatage du message
 * @property imageUri URI de l'image attachée au message (optionnel)
 */
data class Message(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: Uri? = null
)
