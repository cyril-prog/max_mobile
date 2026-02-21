package com.max.aiassistant.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import com.max.aiassistant.ui.theme.*
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import kotlin.math.abs

/**
 * ÉCRAN PRINCIPAL : Voice to Voice
 *
 * Interface pour l'interaction vocale avec l'API Realtime d'OpenAI
 * - Visualiseur d'onde audio animé (orbe fluide)
 * - Zone de transcription temps réel défilante
 * - Indicateur d'état de connexion animé
 * - Bouton micro central pour activer/désactiver le voice to voice
 */
@Composable
fun VoiceScreen(
    isRealtimeConnected: Boolean,
    transcript: String,
    onToggleRealtime: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToWeather: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToActu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()

    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.VOICE,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.HOME -> onNavigateToHome()
                NavigationScreen.VOICE -> { /* Déjà sur cet écran */ }
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
                NavigationScreen.ACTU -> onNavigateToActu()
            }
        },
        sidebarState = sidebarState
    ) {
        VoiceScreenContent(
            isRealtimeConnected = isRealtimeConnected,
            transcript = transcript,
            onToggleRealtime = onToggleRealtime,
            modifier = modifier
        )
    }
}

/**
 * Contenu de l'écran Voice
 * Layout : état connexion en haut → orbe → transcript défilant → bouton micro
 */
@Composable
private fun VoiceScreenContent(
    isRealtimeConnected: Boolean,
    transcript: String,
    onToggleRealtime: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Accumule les lignes du transcript pour un affichage historique
    val transcriptLines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    // Ajoute les nouvelles lignes quand le transcript change
    LaunchedEffect(transcript) {
        if (transcript.isNotBlank()) {
            // Évite les doublons consécutifs
            if (transcriptLines.isEmpty() || transcriptLines.last() != transcript) {
                transcriptLines.add(transcript)
                // Auto-scroll vers le bas
                if (transcriptLines.isNotEmpty()) {
                    listState.animateScrollToItem(transcriptLines.size - 1)
                }
            }
        }
    }

    // Reset du transcript quand on déconnecte
    LaunchedEffect(isRealtimeConnected) {
        if (!isRealtimeConnected) {
            // Garde l'historique visible mais stop le scroll actif
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TopBar harmonisé
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .statusBarsPadding()
                .padding(horizontal = Spacing.md.dp, vertical = Spacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(GradientVoice)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Vocal",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Contenu centré avec padding horizontal
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Indicateur d'état de connexion
        ConnectionStatusBadge(isConnected = isRealtimeConnected)

        Spacer(modifier = Modifier.height(24.dp))

        // Visualiseur orbe fluide
        FluidOrbVisualizer(
            isActive = isRealtimeConnected,
            modifier = Modifier.size(240.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Waveform audio animée (visible uniquement quand connecté)
        AnimatedVisibility(
            visible = isRealtimeConnected,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            AudioWaveform(
                isActive = isRealtimeConnected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zone de transcript défilante (prend l'espace disponible)
        TranscriptArea(
            lines = transcriptLines,
            listState = listState,
            isConnected = isRealtimeConnected,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bouton micro central
        LargeMicButton(
            isConnected = isRealtimeConnected,
            onClick = onToggleRealtime
        )

        Spacer(modifier = Modifier.height(32.dp))
        } // fin Column interne (padding horizontal)
    }
}

/**
 * Badge d'état de connexion avec animation de pulsation
 */
@Composable
private fun ConnectionStatusBadge(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val dotColor = if (isConnected) ConnectionActiveColor else ConnectionIdleColor
    val label = if (isConnected) "En écoute" else "Appuyez pour parler"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        // Dot animé
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = dotColor.copy(alpha = if (isConnected) pulseAlpha else 0.5f),
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isConnected) dotColor else TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Waveform audio animée — barres verticales qui oscillent
 */
@Composable
fun AudioWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    // Crée une animation décalée pour chaque barre
    val animations = (0 until barCount).map { index ->
        val durationMs = 600 + (index % 5) * 80
        val delayMs = (index * 40) % 500
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = durationMs,
                    delayMillis = delayMs,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2f)
        val maxHeight = size.height
        val centerY = size.height / 2f

        animations.forEachIndexed { index, anim ->
            val height = if (isActive) {
                maxHeight * anim.value * (0.4f + 0.6f * sin(index * 0.8f).toFloat().let { abs(it) })
            } else {
                maxHeight * 0.1f
            }
            val x = index * (barWidth * 2f) + barWidth / 2f
            val alpha = if (isActive) 0.7f + 0.3f * anim.value else 0.3f

            drawRoundRect(
                color = AccentBlue.copy(alpha = alpha),
                topLeft = Offset(x, centerY - height / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.7f, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

/**
 * Zone de transcript défilante avec effet de fondu en bas
 */
@Composable
private fun TranscriptArea(
    lines: List<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (lines.isEmpty()) {
            // État vide : instruction
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isConnected) "En attente de votre voix..." else "La transcription apparaîtra ici",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(lines) { line ->
                    TranscriptLine(text = line)
                }
            }

            // Fondu en bas pour indiquer que ça continue
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkBackground
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Ligne de transcript avec style bulle semi-transparente
 */
@Composable
private fun TranscriptLine(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface.copy(alpha = 0.8f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            lineHeight = 22.sp
        )
    }
}

/**
 * Visualiseur fluide avec orbe coloré animé
 */
@Composable
fun FluidOrbVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")

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

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

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

        val colors = listOf(
            AccentBlueMuted,
            AccentBlue,
            Color(0xFF3B82F6),
            AccentBlueLight,
            AccentBlueDark
        )

        val activePulse = if (isActive) pulse else 0.85f

        // Blob 1
        val angle1 = Math.toRadians(rotation1.toDouble())
        val offsetRadius1 = baseRadius * 0.25f * activePulse
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
                radius = baseRadius * 0.7f * activePulse
            ),
            radius = baseRadius * 0.7f * activePulse,
            center = Offset(blob1X, blob1Y),
            blendMode = BlendMode.Screen
        )

        // Blob 2
        val angle2 = Math.toRadians(rotation2.toDouble() + 120)
        val offsetRadius2 = baseRadius * 0.3f * activePulse
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
                radius = baseRadius * 0.65f * activePulse
            ),
            radius = baseRadius * 0.65f * activePulse,
            center = Offset(blob2X, blob2Y),
            blendMode = BlendMode.Screen
        )

        // Blob 3
        val angle3 = Math.toRadians(rotation3.toDouble() + 240)
        val offsetRadius3 = baseRadius * 0.2f * activePulse
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
                radius = baseRadius * 0.6f * activePulse
            ),
            radius = baseRadius * 0.6f * activePulse,
            center = Offset(blob3X, blob3Y),
            blendMode = BlendMode.Screen
        )

        // Blob central
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[1].copy(alpha = 0.7f),
                    colors[2].copy(alpha = 0.5f),
                    colors[0].copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * 0.5f * activePulse
            ),
            radius = baseRadius * 0.5f * activePulse,
            center = Offset(centerX, centerY),
            blendMode = BlendMode.Screen
        )
    }
}

/**
 * Grand bouton micro central
 * - Effet 3D avec dégradé, ombre et highlight
 * - Animation de pulsation quand actif
 */
@Composable
fun LargeMicButton(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    val buttonColorTop = if (isConnected) Color(0xFF0EA5E9) else Color(0xFF1E3A5F)
    val buttonColorBottom = if (isConnected) Color(0xFF0284C7) else Color(0xFF0F1C33)
    val shadowColor = Color(0xFF000000)
    val highlightColor = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Anneau de pulsation (visible seulement quand connecté)
        if (isConnected) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(
                        scaleX = ringScale,
                        scaleY = ringScale,
                        alpha = ringAlpha
                    )
                    .background(
                        color = AccentBlue.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(100.dp)
                .drawBehind {
                    val baseRadius = size.minDimension / 2f
                    drawCircle(
                        color = shadowColor.copy(alpha = 0.4f),
                        radius = baseRadius - 2.dp.toPx(),
                        center = center.copy(y = center.y + 8.dp.toPx())
                    )
                    drawCircle(
                        color = shadowColor.copy(alpha = 0.2f),
                        radius = baseRadius,
                        center = center.copy(y = center.y + 6.dp.toPx())
                    )
                }
                .drawWithContent {
                    drawContent()
                    val baseRadius = size.minDimension / 2f
                    val highlightRadius = baseRadius * 0.8f
                    val highlightCenter = center.copy(y = center.y - baseRadius * 0.3f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0.25f),
                                highlightColor.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = highlightCenter,
                            radius = highlightRadius
                        ),
                        radius = highlightRadius,
                        center = highlightCenter
                    )
                },
            shape = CircleShape,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(buttonColorTop, buttonColorBottom)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isConnected) "Arrêter la conversation" else "Démarrer la conversation",
                    modifier = Modifier.size(44.dp),
                    tint = Color.White
                )
            }
        }
    }
}


