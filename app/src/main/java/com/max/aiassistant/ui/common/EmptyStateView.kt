package com.max.aiassistant.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.max.aiassistant.ui.theme.TextSecondary

/**
 * Composant d'état vide réutilisable avec animation de pulsation légère.
 *
 * @param icon Icône à afficher (Material Icon)
 * @param iconTint Couleur de l'icône et du fond circulaire
 * @param title Titre principal de l'état vide
 * @param subtitle Texte secondaire descriptif (optionnel)
 * @param modifier Modifier pour le layout parent
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    // Animation de pulsation douce sur le conteneur de l'icône
    val infiniteTransition = rememberInfiniteTransition(label = "emptyStatePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icône dans un cercle coloré semi-transparent avec pulsation
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = iconTint.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
