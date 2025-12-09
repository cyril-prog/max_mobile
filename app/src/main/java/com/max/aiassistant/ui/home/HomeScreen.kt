package com.max.aiassistant.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.data.api.WeatherData
import com.max.aiassistant.model.Event
import com.max.aiassistant.model.Task
import com.max.aiassistant.model.TaskPriority
import com.max.aiassistant.model.TaskStatus
import com.max.aiassistant.ui.theme.*
import com.max.aiassistant.ui.weather.CurrentWeatherCard
import java.util.*

/**
 * ÉCRAN D'ACCUEIL : Dashboard de synthèse
 *
 * Affiche :
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Espace en haut
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Bloc Météo (remplace la salutation)
        item {
            if (weatherData != null) {
                CurrentWeatherCard(
                    weatherData = weatherData,
                    cityName = cityName,
                    onOpenSettings = { /* Pas de settings sur le dashboard */ },
                    onRadarClick = onNavigateToWeather
                )
            } else {
                // Placeholder si pas de données météo
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

        // Grille de raccourcis (2 colonnes, 3 lignes)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        icon = Icons.Default.Mic,
                        title = "Assistant vocal",
                        subtitle = "Parler avec Max",
                        gradientColors = listOf(AccentBlue, AccentBlueDark),
                        onClick = onNavigateToVoice,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        icon = Icons.Default.Chat,
                        title = "Chat",
                        subtitle = "Discuter par texte",
                        gradientColors = listOf(Color(0xFF00C853), Color(0xFF00A843)),
                        onClick = onNavigateToChat,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Tâches",
                        subtitle = "Gérer mes tâches",
                        gradientColors = listOf(Color(0xFFFF6F00), Color(0xFFE65100)),
                        onClick = onNavigateToTasks,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        icon = Icons.Default.CalendarToday,
                        title = "Planning",
                        subtitle = "Voir mon agenda",
                        gradientColors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2)),
                        onClick = onNavigateToPlanning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        icon = Icons.Default.WbSunny,
                        title = "Météo",
                        subtitle = "Voir les prévisions",
                        gradientColors = listOf(Color(0xFF00BCD4), Color(0xFF0097A7)),
                        onClick = onNavigateToWeather,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        icon = Icons.Default.Notes,
                        title = "Notes",
                        subtitle = "Créer une note",
                        gradientColors = listOf(Color(0xFFD32F2F), Color(0xFFC62828)),
                        onClick = onNavigateToNotes,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Espace en bas
        item {
            Spacer(modifier = Modifier.height(32.dp))
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
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00BCD4), Color(0xFF0097A7))
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
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

    // Filtre les tâches du jour (à faire)
    val todayTasks = tasks.filter { it.status != TaskStatus.COMPLETED }
    val highPriorityTasks = todayTasks.filter { it.priority == TaskPriority.P1 || it.priority == TaskPriority.P2 }
    val mediumPriorityTasks = todayTasks.filter { it.priority == TaskPriority.P3 }

    // Trouve l'événement en cours ou le prochain événement du jour
    val now = Calendar.getInstance()
    
    // Filtre les événements du jour
    val todayEvents = events.filter { event ->
        try {
            // Essaie d'abord avec startDateTime
            if (event.startDateTime.isNotEmpty()) {
                val eventCal = parseIsoDate(event.startDateTime)
                eventCal != null && isSameDay(eventCal, today)
            } else if (event.date.isNotEmpty()) {
                // Fallback sur le champ date si startDateTime est vide
                // Le format de date est "Lun 9 décembre" - on considère que c'est aujourd'hui si présent
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Cherche d'abord un événement en cours, sinon le prochain
    val currentOrNextEvent = todayEvents.find { event ->
        // Événement en cours : a commencé mais pas encore fini
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
        // Sinon, prochain événement (pas encore commencé)
        try {
            if (event.startDateTime.isEmpty()) return@find true // Si pas de startDateTime, on le prend
            val startCal = parseIsoDate(event.startDateTime)
            startCal != null && now.timeInMillis < startCal.timeInMillis
        } catch (e: Exception) {
            false
        }
    } ?: todayEvents.firstOrNull() // Fallback sur le premier événement du jour

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Résumé du jour",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            // Tâches
            if (todayTasks.isNotEmpty()) {
                SummaryItem(
                    icon = Icons.Default.CheckCircle,
                    title = "${todayTasks.size} tâche${if (todayTasks.size > 1) "s" else ""} à faire",
                    subtitle = buildString {
                        if (highPriorityTasks.isNotEmpty()) {
                            append("${highPriorityTasks.size} priorité haute")
                        }
                        if (mediumPriorityTasks.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("${mediumPriorityTasks.size} priorité moyenne")
                        }
                    },
                    iconTint = Color(0xFFFF6F00)
                )
            } else {
                SummaryItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Aucune tâche en attente",
                    subtitle = "Profitez de votre journée !",
                    iconTint = Color(0xFF00C853)
                )
            }

            // Événement en cours ou prochain
            if (currentOrNextEvent != null) {
                // Vérifie si l'événement est en cours
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
                    iconTint = if (isOngoing) Color(0xFF00C853) else Color(0xFF9C27B0)
                )
            } else {
                SummaryItem(
                    icon = Icons.Default.Event,
                    title = "Aucun événement prévu",
                    subtitle = "Journée libre",
                    iconTint = TextSecondary
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
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
 * Carte de raccourci rapide (compacte)
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
    Card(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 34.dp)
                )
            }
        }
    }
}

/**
 * Parse une date ISO
 */
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
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    cal.set(year, month, day, hour, minute, 0)
                } else {
                    cal.set(year, month, day, 0, 0, 0)
                }
            } else {
                cal.set(year, month, day, 0, 0, 0)
            }

            cal.set(Calendar.MILLISECOND, 0)
            cal
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Vérifie si deux calendriers sont le même jour
 */
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * Calcule le temps jusqu'à un événement
 */
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
