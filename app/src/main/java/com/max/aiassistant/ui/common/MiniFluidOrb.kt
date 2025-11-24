package com.max.aiassistant.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Orbe fluide miniature pour les barres de titre
 *
 * Version compacte de l'orbe animé de VoiceScreen pour créer
 * une cohérence visuelle entre les écrans
 * Cliquable pour retourner à l'écran principal
 */
@Composable
fun MiniFluidOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation continue pour créer l'effet fluide
    val infiniteTransition = rememberInfiniteTransition(label = "mini_fluid")

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

    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
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
        val offsetRadius1 = baseRadius * 0.25f * pulse
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
        val offsetRadius2 = baseRadius * 0.3f * pulse
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
        val offsetRadius3 = baseRadius * 0.2f * pulse
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
