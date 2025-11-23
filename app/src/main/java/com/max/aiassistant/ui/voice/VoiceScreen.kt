package com.max.aiassistant.ui.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.max.aiassistant.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

/**
 * ÉCRAN PRINCIPAL : Voice to Voice
 *
 * Interface minimaliste pour l'interaction vocale avec l'API Realtime d'OpenAI
 * - Visualiseur d'onde audio animé
 * - Bouton micro pour activer/désactiver la connexion Realtime
 * - Bouton message pour naviguer vers le chat
 * - Bouton tâches pour naviguer vers les tâches et agenda
 */
@Composable
fun VoiceScreen(
    isRealtimeConnected: Boolean,
    transcript: String,
    onToggleRealtime: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Espace en haut pour pousser l'orbe vers le haut
        Spacer(modifier = Modifier.weight(0.5f))

        // Visualiseur fluide avec orbe coloré
        FluidOrbVisualizer(
            isActive = isRealtimeConnected,
            modifier = Modifier
                .size(280.dp)
        )

        // Grand espace entre l'orbe et les boutons
        Spacer(modifier = Modifier.weight(2f))

        // Boutons d'action (Micro, Message et Tâches)
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton Micro (bascule entre micro barré et micro actif)
            RealtimeMicButton(
                isConnected = isRealtimeConnected,
                onClick = onToggleRealtime
            )

            // Bouton Message
            MessageButton(
                onClick = onNavigateToChat
            )

            // Bouton Tâches
            TaskButton(
                onClick = onNavigateToTasks
            )
        }

        // Espace en bas pour pousser les boutons vers le bas (plus petit)
        Spacer(modifier = Modifier.weight(0.5f))
    }
}

/**
 * Visualiseur fluide avec orbe coloré animé
 *
 * Dessine un cercle avec des couleurs fluides qui évoluent à l'intérieur
 * comme de la fumée ou un liquide en mouvement
 */
@Composable
fun FluidOrbVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation continue pour créer l'effet fluide
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")

    // Rotation principale
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    val rotation3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation3"
    )

    // Pulsation pour l'effet vivant
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Morphing des blobs
    val morph1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph1"
    )

    val morph2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph2"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = size.minDimension / 2

        // Couleurs pour l'effet fluide - Teintes de bleu
        val colors = listOf(
            Color(0xFF1E3A5F), // Bleu profond
            Color(0xFF2563EB), // Bleu vif
            Color(0xFF3B82F6), // Bleu moyen
            Color(0xFF60A5FA), // Bleu clair
            Color(0xFF0EA5E9)  // Bleu cyan
        )

        // Dessiner plusieurs blobs colorés qui se déplacent
        // Blob 1 - Indigo/Violet
        val angle1 = Math.toRadians(rotation1.toDouble())
        val offsetRadius1 = baseRadius * 0.25f * (if (isActive) pulse else 0.8f)
        val blob1X = centerX + (cos(angle1) * offsetRadius1 * morph1).toFloat()
        val blob1Y = centerY + (sin(angle1) * offsetRadius1 * morph1).toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[0].copy(alpha = 0.6f),
                    colors[1].copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(blob1X, blob1Y),
                radius = baseRadius * 0.7f * pulse
            ),
            radius = baseRadius * 0.7f * pulse,
            center = Offset(blob1X, blob1Y),
            blendMode = BlendMode.Screen
        )

        // Blob 2 - Rose/Orange
        val angle2 = Math.toRadians(rotation2.toDouble() + 120)
        val offsetRadius2 = baseRadius * 0.3f * (if (isActive) pulse else 0.8f)
        val blob2X = centerX + (cos(angle2) * offsetRadius2 * morph2).toFloat()
        val blob2Y = centerY + (sin(angle2) * offsetRadius2 * morph2).toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[2].copy(alpha = 0.6f),
                    colors[3].copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(blob2X, blob2Y),
                radius = baseRadius * 0.65f * pulse
            ),
            radius = baseRadius * 0.65f * pulse,
            center = Offset(blob2X, blob2Y),
            blendMode = BlendMode.Screen
        )

        // Blob 3 - Vert/Indigo
        val angle3 = Math.toRadians(rotation3.toDouble() + 240)
        val offsetRadius3 = baseRadius * 0.2f * (if (isActive) pulse else 0.8f)
        val blob3X = centerX + (cos(angle3) * offsetRadius3 * (1f - morph1)).toFloat()
        val blob3Y = centerY + (sin(angle3) * offsetRadius3 * (1f - morph1)).toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[4].copy(alpha = 0.5f),
                    colors[0].copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(blob3X, blob3Y),
                radius = baseRadius * 0.6f * pulse
            ),
            radius = baseRadius * 0.6f * pulse,
            center = Offset(blob3X, blob3Y),
            blendMode = BlendMode.Screen
        )

        // Blob central - toujours au centre avec plusieurs couleurs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[1].copy(alpha = 0.7f),
                    colors[2].copy(alpha = 0.5f),
                    colors[0].copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * 0.5f * pulse
            ),
            radius = baseRadius * 0.5f * pulse,
            center = Offset(centerX, centerY),
            blendMode = BlendMode.Screen
        )
    }
}

/**
 * Bouton circulaire avec icône micro pour l'API Realtime
 * - Micro barré (MicOff) par défaut (déconnecté)
 * - Micro actif (Mic) quand connecté à l'API Realtime
 */
@Composable
fun RealtimeMicButton(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    // Couleur du bouton change selon l'état
    val buttonColor = if (isConnected) {
        Color(0xFF2563EB) // Bleu vif quand connecté
    } else {
        Color(0xFF1E3A5F) // Bleu sombre quand déconnecté
    }

    // Bouton circulaire avec animation de couleur
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        containerColor = buttonColor,
        contentColor = Color.White
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isConnected) "Arrêter la conversation" else "Démarrer la conversation",
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * Bouton circulaire avec icône message
 * Pour naviguer vers le chat
 */
@Composable
fun MessageButton(
    onClick: () -> Unit
) {
    // Bouton circulaire bleu pour le message
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        containerColor = Color(0xFF1E3A5F),
        contentColor = Color.White
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Message,
            contentDescription = "Open chat",
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * Bouton circulaire avec icône tâches
 * Pour naviguer vers les tâches et l'agenda
 */
@Composable
fun TaskButton(
    onClick: () -> Unit
) {
    // Bouton circulaire bleu pour les tâches
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        containerColor = Color(0xFF1E3A5F),
        contentColor = Color.White
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Open tasks",
            modifier = Modifier.size(36.dp)
        )
    }
}

