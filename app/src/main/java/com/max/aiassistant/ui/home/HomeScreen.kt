package com.max.aiassistant.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.data.api.WeatherData
import com.max.aiassistant.model.Event
import com.max.aiassistant.model.Task
import com.max.aiassistant.model.TaskPriority
import com.max.aiassistant.model.TaskStatus
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.theme.*
import com.max.aiassistant.ui.weather.CurrentWeatherCard
import java.util.*

/**
 * ÉCRAN D'ACCUEIL : Dashboard de synthèse
 *
 * Affiche :
 * - Sidebar de navigation (swipe depuis le bord droit)
 * - Bloc météo (widget compact)
 * - Résumé du jour (tâches, événements)
 * - Raccourcis rapides vers les 6 fonctions principales
 */
@Composable
fun HomeScreen(
    tasks: List<Task>,
    events: List<Event>,
    weatherData: WeatherData?,
    cityName: String,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()

    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.HOME,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.HOME -> { /* Déjà ici */ }
                NavigationScreen.VOICE -> onNavigateToVoice()
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
            }
        },
        sidebarState = sidebarState
    ) {
        HomeScreenContent(
            tasks = tasks,
            events = events,
            weatherData = weatherData,
            cityName = cityName,
            onNavigateToVoice = onNavigateToVoice,
            onNavigateToChat = onNavigateToChat,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToPlanning = onNavigateToPlanning,
            onNavigateToWeather = onNavigateToWeather,
            onNavigateToNotes = onNavigateToNotes,
            modifier = modifier
        )
    }
}

/**
 * Contenu scrollable du HomeScreen
 */
@Composable
private fun HomeScreenContent(
    tasks: List<Task>,
    events: List<Event>,
    weatherData: WeatherData?,
    cityName: String,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Spacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md.dp)
    ) {
        // Espace en haut
        item {
            Spacer(modifier = Modifier.height(Spacing.xxl.dp))
        }

        // Bloc Météo
        item {
            if (weatherData != null) {
                CurrentWeatherCard(
                    weatherData = weatherData,
                    cityName = cityName,
                    onOpenSettings = { },
                    onRadarClick = onNavigateToWeather
                )
            } else {
                WeatherPlaceholderCard(onClick = onNavigateToWeather)
            }
        }

        // Résumé du jour
        item {
            DailySummarySection(
                tasks = tasks,
                events = events
            )
        }

        // Grille de raccourcis
        item {
            QuickAccessGrid(
                onNavigateToVoice = onNavigateToVoice,
                onNavigateToChat = onNavigateToChat,
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToPlanning = onNavigateToPlanning,
                onNavigateToWeather = onNavigateToWeather,
                onNavigateToNotes = onNavigateToNotes
            )
        }

        // Espace en bas
        item {
            Spacer(modifier = Modifier.height(Spacing.xl.dp))
        }
    }
}

/**
 * Grille 2×3 de raccourcis rapides
 */
@Composable
private fun QuickAccessGrid(
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToNotes: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            QuickAccessCard(
                icon = Icons.Default.Mic,
                title = "Assistant vocal",
                subtitle = "Parler avec Max",
                gradientColors = GradientVoice,
                onClick = onNavigateToVoice,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "Chat",
                subtitle = "Discuter par texte",
                gradientColors = GradientChat,
                onClick = onNavigateToChat,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            QuickAccessCard(
                icon = Icons.Default.CheckCircle,
                title = "Tâches",
                subtitle = "Gérer mes tâches",
                gradientColors = GradientTasks,
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                icon = Icons.Default.CalendarToday,
                title = "Planning",
                subtitle = "Voir mon agenda",
                gradientColors = GradientPlanning,
                onClick = onNavigateToPlanning,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            QuickAccessCard(
                icon = Icons.Default.WbSunny,
                title = "Météo",
                subtitle = "Voir les prévisions",
                gradientColors = GradientWeather,
                onClick = onNavigateToWeather,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Notes",
                subtitle = "Créer une note",
                gradientColors = GradientNotes,
                onClick = onNavigateToNotes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Carte placeholder quand la météo n'est pas chargée
 */
@Composable
private fun WeatherPlaceholderCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors = GradientWeather)
                )
                .padding(Spacing.md.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Météo",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = "Chargement météo...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Appuyez pour voir les prévisions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Section résumé du jour
 */
@Composable
private fun DailySummarySection(
    tasks: List<Task>,
    events: List<Event>
) {
    val today = Calendar.getInstance()

    val todayTasks = tasks.filter { it.status != TaskStatus.COMPLETED }
    val highPriorityTasks = todayTasks.filter { it.priority == TaskPriority.P1 || it.priority == TaskPriority.P2 }
    val mediumPriorityTasks = todayTasks.filter { it.priority == TaskPriority.P3 }

    val now = Calendar.getInstance()
    val todayEvents = events.filter { event ->
        try {
            if (event.startDateTime.isNotEmpty()) {
                val eventCal = parseIsoDate(event.startDateTime)
                eventCal != null && isSameDay(eventCal, today)
            } else if (event.date.isNotEmpty()) {
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    val currentOrNextEvent = todayEvents.find { event ->
        try {
            if (event.startDateTime.isEmpty()) return@find false
            val startCal = parseIsoDate(event.startDateTime)
            val endCal = if (event.endDateTime.isNotEmpty()) parseIsoDate(event.endDateTime) else null
            if (startCal != null) {
                val hasStarted = now.timeInMillis >= startCal.timeInMillis
                val hasEnded = endCal != null && now.timeInMillis >= endCal.timeInMillis
                hasStarted && !hasEnded
            } else false
        } catch (e: Exception) {
            false
        }
    } ?: todayEvents.find { event ->
        try {
            if (event.startDateTime.isEmpty()) return@find true
            val startCal = parseIsoDate(event.startDateTime)
            startCal != null && now.timeInMillis < startCal.timeInMillis
        } catch (e: Exception) {
            false
        }
    } ?: todayEvents.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md.dp)
        ) {
            Text(
                text = "Résumé du jour",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (todayTasks.isNotEmpty()) {
                SummaryItem(
                    icon = Icons.Default.CheckCircle,
                    title = "${todayTasks.size} tâche${if (todayTasks.size > 1) "s" else ""} à faire",
                    subtitle = buildString {
                        if (highPriorityTasks.isNotEmpty()) append("${highPriorityTasks.size} priorité haute")
                        if (mediumPriorityTasks.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("${mediumPriorityTasks.size} priorité moyenne")
                        }
                    },
                    iconTint = HighOrange
                )
            } else {
                SummaryItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Aucune tâche en attente",
                    subtitle = "Profitez de votre journée !",
                    iconTint = CompletedGreen
                )
            }

            if (currentOrNextEvent != null) {
                val isOngoing = try {
                    val startCal = parseIsoDate(currentOrNextEvent.startDateTime)
                    startCal != null && now.timeInMillis >= startCal.timeInMillis
                } catch (e: Exception) {
                    false
                }

                val timeInfo = when {
                    isOngoing -> "En cours"
                    currentOrNextEvent.startTime == "Toute la journée" -> "Toute la journée"
                    else -> {
                        val timeUntil = calculateTimeUntil(currentOrNextEvent.startDateTime)
                        if (timeUntil.isNotEmpty()) "dans $timeUntil" else currentOrNextEvent.startTime
                    }
                }

                SummaryItem(
                    icon = Icons.Default.Event,
                    title = currentOrNextEvent.title,
                    subtitle = timeInfo,
                    iconTint = if (isOngoing) CompletedGreen else Color(0xFF9C27B0)
                )
            } else {
                SummaryItem(
                    icon = Icons.Default.Event,
                    title = "Aucun événement prévu",
                    subtitle = "Journée libre",
                    iconTint = TextTertiary
                )
            }
        }
    }
}

/**
 * Item de résumé (icône + texte)
 */
@Composable
private fun SummaryItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
    ) {
        // Icône avec fond coloré subtil
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Carte de raccourci rapide avec animation au tap
 */
@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .height(80.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(horizontal = Spacing.md.dp, vertical = Spacing.sm.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }
        }
    }
}

// ─── Utilitaires de date ─────────────────────────────────────────────

private fun parseIsoDate(isoDate: String): Calendar? {
    return try {
        val cal = Calendar.getInstance()
        val parts = isoDate.split("T")
        val datePart = parts[0]
        val dateParts = datePart.split("-")
        if (dateParts.size == 3) {
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()
            if (parts.size > 1) {
                val timePart = parts[1].split("+", "-", "Z")[0]
                val timeParts = timePart.split(":")
                if (timeParts.size >= 2) {
                    cal.set(year, month, day, timeParts[0].toInt(), timeParts[1].toInt(), 0)
                } else {
                    cal.set(year, month, day, 0, 0, 0)
                }
            } else {
                cal.set(year, month, day, 0, 0, 0)
            }
            cal.set(Calendar.MILLISECOND, 0)
            cal
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

private fun calculateTimeUntil(isoDateTime: String): String {
    return try {
        val eventCal = parseIsoDate(isoDateTime) ?: return ""
        val now = Calendar.getInstance()
        val diffMillis = eventCal.timeInMillis - now.timeInMillis
        if (diffMillis < 0) return "en cours"
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60
        when {
            hours > 0 && minutes > 0 -> "${hours}h${minutes}min"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}min"
            else -> "maintenant"
        }
    } catch (e: Exception) {
        ""
    }
}
