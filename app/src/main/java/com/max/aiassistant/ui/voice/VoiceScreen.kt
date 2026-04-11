package com.max.aiassistant.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.ui.common.BannerTone
import com.max.aiassistant.ui.common.InlineStatusBanner
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberMotionEnabled
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import kotlin.math.sin

private val VoiceBackgroundTop = Color(0xFF09111F)
private val VoiceBackgroundBottom = Color(0xFF030509)
private val VoicePanel = Color(0xFF0D1726)
private val VoicePanelSoft = Color(0xFF131F31)
private val VoicePanelStrong = Color(0xFF18263B)
private val VoiceOutline = Color(0xFF223754)
private val VoiceText = Color(0xFFF4F7FB)
private val VoiceMuted = Color(0xFF90A1B8)
private val VoiceAccent = Color(0xFF61D6FF)
private val VoiceAccentStrong = Color(0xFF0DA6FF)
private val VoiceAccentSoft = Color(0xFF7CF2D3)
private val VoiceAlert = Color(0xFFFF8A65)

@Composable
fun VoiceScreen(
    isRealtimeConnected: Boolean,
    errorMessage: String? = null,
    isOffline: Boolean = false,
    transcript: String,
    onToggleRealtime: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToWeather: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToActu: () -> Unit = {},
    showChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (showChrome) {
        val sidebarState = rememberNavigationSidebarState()

        NavigationSidebarScaffold(
            currentScreen = NavigationScreen.VOICE,
            onNavigateToScreen = { screen ->
                when (screen) {
                    NavigationScreen.HOME -> onNavigateToHome()
                    NavigationScreen.VOICE -> Unit
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
                errorMessage = errorMessage,
                isOffline = isOffline,
                transcript = transcript,
                onToggleRealtime = onToggleRealtime,
                modifier = modifier
            )
        }
    } else {
        VoiceScreenContent(
            isRealtimeConnected = isRealtimeConnected,
            errorMessage = errorMessage,
            isOffline = isOffline,
            transcript = transcript,
            onToggleRealtime = onToggleRealtime,
            modifier = modifier
        )
    }
}

@Composable
private fun VoiceScreenContent(
    isRealtimeConnected: Boolean,
    errorMessage: String?,
    isOffline: Boolean,
    transcript: String,
    onToggleRealtime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transcriptLines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(transcript) {
        val cleaned = transcript.trim()
        if (cleaned.isNotEmpty() && (transcriptLines.isEmpty() || transcriptLines.last() != cleaned)) {
            transcriptLines.add(cleaned)
            listState.animateScrollToItem(transcriptLines.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(VoiceBackgroundTop, VoiceBackgroundBottom)
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        VoiceBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            VoiceTopLabel()
            Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
                InlineStatusBanner(
                    title = "Mode vocal indisponible",
                    subtitle = errorMessage,
                    tone = BannerTone.Error,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else if (isOffline && !isRealtimeConnected) {
                InlineStatusBanner(
                    title = "Connexion requise",
                    subtitle = "Le studio vocal a besoin d'une connexion stable avant de lancer l'ecoute.",
                    tone = BannerTone.Offline,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            VoiceHeroStage(
                isRealtimeConnected = isRealtimeConnected,
                onToggleRealtime = onToggleRealtime
            )

            Spacer(modifier = Modifier.height(18.dp))

            VoiceMetricsRow(isRealtimeConnected = isRealtimeConnected)

            Spacer(modifier = Modifier.height(18.dp))

            TranscriptConsole(
                lines = transcriptLines,
                listState = listState,
                isRealtimeConnected = isRealtimeConnected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VoiceBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 28.dp)
                .size(180.dp)
                .blur(36.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VoiceAccent.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(width = 140.dp, height = 220.dp)
                .blur(48.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VoiceAccentSoft.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(80.dp)
                )
        )
    }
}

@Composable
private fun VoiceTopLabel() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Mode vocal",
            style = MaterialTheme.typography.headlineMedium,
            color = VoiceText,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Parlez, laissez Max ecouter, puis reprenez la main instantanement.",
            style = MaterialTheme.typography.bodyMedium,
            color = VoiceMuted
        )
    }
}

@Composable
private fun VoiceHeroStage(
    isRealtimeConnected: Boolean,
    onToggleRealtime: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            VoicePanelStrong,
                            VoicePanel
                        )
                    )
                )
                .drawBehind {
                    drawRoundRect(
                        color = VoiceOutline,
                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VoiceStageHeader(isRealtimeConnected = isRealtimeConnected)
            Spacer(modifier = Modifier.height(18.dp))
            VoicePulseCore(
                isRealtimeConnected = isRealtimeConnected,
                modifier = Modifier.size(244.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRealtimeConnected) {
                    "Le micro est ouvert. Parlez naturellement."
                } else {
                    "Pret pour une conversation vocale nette et continue."
                },
                style = MaterialTheme.typography.titleMedium,
                color = VoiceText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRealtimeConnected) {
                    "Max transcrit et repond en direct sans quitter cet ecran."
                } else {
                    "Un appui lance la session. Un second coupe immediatement l'ecoute."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = VoiceMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            VoicePrimaryControl(
                isRealtimeConnected = isRealtimeConnected,
                onToggleRealtime = onToggleRealtime
            )
        }
    }
}

@Composable
private fun VoiceStageHeader(isRealtimeConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Studio temps reel",
                style = MaterialTheme.typography.titleMedium,
                color = VoiceText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isRealtimeConnected) "Session en direct" else "En attente",
                style = MaterialTheme.typography.bodySmall,
                color = VoiceMuted
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isRealtimeConnected) VoiceAccent.copy(alpha = 0.14f)
                    else VoicePanelSoft
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isRealtimeConnected) VoiceAccentSoft else VoiceMuted.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            )
            Text(
                text = if (isRealtimeConnected) "Ouvert" else "Ferme",
                style = MaterialTheme.typography.labelLarge,
                color = VoiceText
            )
        }
    }
}

@Composable
private fun VoicePulseCore(
    isRealtimeConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val motionEnabled = rememberMotionEnabled()
    val transition = if (motionEnabled) rememberInfiniteTransition(label = "voice_core") else null
    val ringRotation = if (motionEnabled && transition != null) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_rotation"
        ).value
    } else {
        42f
    }
    val pulse = if (motionEnabled && transition != null) {
        transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    } else {
        1f
    }
    val waveformDrift = if (motionEnabled && transition != null) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_drift"
        ).value
    } else {
        0.3f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val radius = size.minDimension / 2f
            val center = center
            val liveScale = if (isRealtimeConnected) pulse else 0.94f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VoiceAccent.copy(alpha = 0.28f),
                        VoiceAccentStrong.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                radius = radius * 0.98f * liveScale,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF16314B),
                        VoicePanel,
                        Color(0xFF08101B)
                    )
                ),
                radius = radius * 0.78f,
                center = center
            )

            drawCircle(
                color = VoiceOutline.copy(alpha = 0.9f),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        VoiceAccent.copy(alpha = 0.2f),
                        VoiceAccentStrong,
                        VoiceAccentSoft,
                        VoiceAccent.copy(alpha = 0.2f)
                    )
                ),
                startAngle = ringRotation,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
                size = Size(radius * 1.72f, radius * 1.72f),
                style = Stroke(
                    width = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.cornerPathEffect(24.dp.toPx())
                )
            )

            val bars = 20
            val barWidth = radius * 0.045f
            repeat(bars) { index ->
                val x = center.x - radius * 0.48f + index * (barWidth * 1.45f)
                val wave = ((sin((index * 0.42f) + waveformDrift * 6f) + 1f) / 2f)
                val barHeight = radius * (if (isRealtimeConnected) 0.2f + wave * 0.36f else 0.12f)
                drawRoundRect(
                    color = if (isRealtimeConnected) VoiceAccent.copy(alpha = 0.92f) else VoiceMuted.copy(alpha = 0.42f),
                    topLeft = Offset(x, center.y - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth, barWidth)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isRealtimeConnected) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                contentDescription = null,
                tint = VoiceText,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isRealtimeConnected) "Live" else "Standby",
                style = MaterialTheme.typography.titleLarge,
                color = VoiceText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRealtimeConnected) "Detection ouverte" else "Session fermee",
                style = MaterialTheme.typography.bodySmall,
                color = VoiceMuted
            )
        }
    }
}

@Composable
private fun VoicePrimaryControl(
    isRealtimeConnected: Boolean,
    onToggleRealtime: () -> Unit
) {
    Surface(
        onClick = onToggleRealtime,
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isRealtimeConnected) {
                            listOf(Color(0xFF14324A), Color(0xFF0B2238))
                        } else {
                            listOf(VoiceAccentStrong, Color(0xFF0A7FDF))
                        }
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = Color.White.copy(alpha = if (isRealtimeConnected) 0.08f else 0.18f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = if (isRealtimeConnected) "Couper le micro" else "Lancer la session",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isRealtimeConnected) "Arret immediat de l'ecoute" else "Active Max en mode vocal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun VoiceMetricsRow(isRealtimeConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VoiceMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.GraphicEq,
            label = "Capture",
            value = if (isRealtimeConnected) "Active" else "Pause",
            accent = if (isRealtimeConnected) VoiceAccentSoft else VoiceMuted
        )
        VoiceMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Tune,
            label = "Clarte",
            value = if (isRealtimeConnected) "Direct" else "Prete",
            accent = VoiceAccent
        )
    }
}

@Composable
private fun VoiceMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(VoicePanelSoft)
            .drawBehind {
                drawRoundRect(
                    color = VoiceOutline.copy(alpha = 0.85f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = VoiceMuted
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = VoiceText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TranscriptConsole(
    lines: List<String>,
    listState: LazyListState,
    isRealtimeConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0xFF07111C))
            .drawBehind {
                drawRoundRect(
                    color = VoiceOutline.copy(alpha = 0.78f),
                    cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transcription",
                    style = MaterialTheme.typography.titleMedium,
                    color = VoiceText,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isRealtimeConnected) "Le flux s'ajoute ici en continu." else "Le journal reste visible entre deux sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VoiceMuted
                )
            }
            Text(
                text = "${lines.size} lignes",
                style = MaterialTheme.typography.labelLarge,
                color = VoiceMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = VoiceOutline.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(14.dp))

        if (lines.isEmpty()) {
            VoiceEmptyConsole(isRealtimeConnected = isRealtimeConnected)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(lines) { line ->
                    VoiceTranscriptBubble(text = line)
                }
            }
        }
    }
}

@Composable
private fun VoiceEmptyConsole(isRealtimeConnected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(VoicePanelSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = VoiceAccent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (isRealtimeConnected) "Ecoute en cours" else "Aucune phrase captee",
            style = MaterialTheme.typography.titleMedium,
            color = VoiceText,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isRealtimeConnected) {
                "Commencez a parler. Les phrases apparaitront ici au fil de la session."
            } else {
                "Lancez la session pour ouvrir le micro et construire l'historique vocal."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = VoiceMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceTranscriptBubble(text: String) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VoicePanelSoft,
                            VoicePanel
                        )
                    )
                )
                .drawBehind {
                    drawRoundRect(
                        color = VoiceOutline.copy(alpha = 0.85f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = VoiceText,
                lineHeight = 24.sp
            )
        }
    }
}
