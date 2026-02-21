package com.max.aiassistant.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
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
    onNavigateToActu: () -> Unit,
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
                NavigationScreen.ACTU -> onNavigateToActu()
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
            onNavigateToActu = onNavigateToActu,
            modifier = modifier
        )
    }
}

/**
 * Contenu du HomeScreen — layout fixe sans scroll, tout tient en hauteur
 *
 * Structure verticale avec weight() pour répartir l'espace disponible :
 *   - Météo      : hauteur fixe (wrap_content)
 *   - Résumé     : weight(1f) — prend l'espace restant
 *   - Raccourcis : weight(2f) — prend 2× plus d'espace que le résumé
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
    onNavigateToActu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                start = Spacing.md.dp,
                end = Spacing.md.dp,
                top = Spacing.xl.dp,
                bottom = Spacing.sm.dp
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
    ) {
        // ── Bloc Météo (hauteur naturelle) ──────────────────────────────
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

        // ── Résumé du jour (s'étire pour remplir) ───────────────────────
        DailySummarySection(
            tasks = tasks,
            events = events,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToPlanning = onNavigateToPlanning,
            modifier = Modifier.weight(1f)
        )

        // ── Grille de raccourcis (prend 2× l'espace du résumé) ──────────
        QuickAccessGrid(
            onNavigateToVoice = onNavigateToVoice,
            onNavigateToChat = onNavigateToChat,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToPlanning = onNavigateToPlanning,
            onNavigateToNotes = onNavigateToNotes,
            onNavigateToActu = onNavigateToActu,
            modifier = Modifier.weight(2f)
        )
    }
}

/**
 * Grille de raccourcis — 3 lignes × 2 boutons, hauteur identique pour chaque bouton.
 *
 * Ligne 1 : Vocal      | Chat
 * Ligne 2 : Tâches     | Planning
 * Ligne 3 : Notes      | Actualités
 */
@Composable
private fun QuickAccessGrid(
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToActu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
    ) {
        // ── Ligne 1 : Vocal + Chat ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            QuickAccessCard(
                icon = Icons.Default.Mic,
                title = "Assistant vocal",
                subtitle = "Parler avec Max",
                gradientColors = GradientVoice,
                onClick = onNavigateToVoice,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            QuickAccessCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "Chat",
                subtitle = "Discuter avec Max",
                gradientColors = GradientChat,
                onClick = onNavigateToChat,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // ── Ligne 2 : Tâches + Planning ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            QuickAccessCard(
                icon = Icons.Default.CheckCircle,
                title = "Tâches",
                subtitle = "Gérer mes tâches",
                gradientColors = GradientTasks,
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            QuickAccessCard(
                icon = Icons.Default.CalendarToday,
                title = "Planning",
                subtitle = "Voir mon agenda",
                gradientColors = GradientPlanning,
                onClick = onNavigateToPlanning,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // ── Ligne 3 : Notes + Actualités ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            QuickAccessCard(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Notes",
                subtitle = "Prises de notes",
                gradientColors = GradientNotes,
                onClick = onNavigateToNotes,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            QuickAccessCard(
                icon = Icons.Default.Newspaper,
                title = "Actualités",
                subtitle = "Actualité du jour & IA",
                gradientColors = GradientActu,
                onClick = onNavigateToActu,
                modifier = Modifier.weight(1f).fillMaxHeight()
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
 * Section résumé du jour — version enrichie
 * - En-tête avec la date du jour
 * - Tâche prioritaire mise en avant dans le sous-titre
 * - Compteur d'événements + prochain événement nommé
 * - Liens "Voir tout" vers Tâches et Planning
 * - Barre gauche avec IntrinsicSize pour éviter la hauteur 0
 */
@Composable
private fun DailySummarySection(
    tasks: List<Task>,
    events: List<Event>,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Calendar.getInstance()
    val now = Calendar.getInstance()

    // ── Tâches ───────────────────────────────────────────────────────────
    val pendingTasks = tasks.filter { it.status != TaskStatus.COMPLETED }
    val highPriorityTask = pendingTasks.firstOrNull {
        it.priority == TaskPriority.P1 || it.priority == TaskPriority.P2
    }
    val taskSubtitle = when {
        pendingTasks.isEmpty() -> "Profitez de votre journée !"
        highPriorityTask != null -> "Priorité : ${highPriorityTask.title}"
        else -> "${pendingTasks.size} tâche${if (pendingTasks.size > 1) "s" else ""} en attente"
    }

    // ── Événements ───────────────────────────────────────────────────────
    val todayEvents = events.filter { event ->
        try {
            if (event.startDateTime.isNotEmpty()) {
                val eventCal = parseIsoDate(event.startDateTime)
                eventCal != null && isSameDay(eventCal, today)
            } else {
                event.date.isNotEmpty()
            }
        } catch (e: Exception) { false }
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
        } catch (e: Exception) { false }
    } ?: todayEvents.find { event ->
        try {
            if (event.startDateTime.isEmpty()) return@find true
            val startCal = parseIsoDate(event.startDateTime)
            startCal != null && now.timeInMillis < startCal.timeInMillis
        } catch (e: Exception) { false }
    } ?: todayEvents.firstOrNull()

    val isEventOngoing = currentOrNextEvent != null && try {
        val startCal = parseIsoDate(currentOrNextEvent.startDateTime)
        startCal != null && now.timeInMillis >= startCal.timeInMillis
    } catch (e: Exception) { false }

    val eventSubtitle = when {
        currentOrNextEvent == null -> "Journée libre"
        isEventOngoing -> "En cours"
        currentOrNextEvent.startTime == "Toute la journée" -> "Toute la journée"
        else -> {
            val cal = parseIsoDate(currentOrNextEvent.startDateTime)
            if (cal != null) {
                val day = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
                val month = "%02d".format(cal.get(Calendar.MONTH) + 1)
                val hour = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
                val min = "%02d".format(cal.get(Calendar.MINUTE))
                "$day/$month à ${hour}h$min"
            } else {
                currentOrNextEvent.startTime
            }
        }
    }

    // ── En-tête : date du jour ────────────────────────────────────────────
    val dayName = android.text.format.DateFormat.format("EEEE d MMMM", today).toString()
        .replaceFirstChar { it.uppercase() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        // IntrinsicSize.Min garantit que la barre gauche prend bien la hauteur du contenu
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {
            // Barre verticale gradient à gauche
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(AccentBlue, Color(0xFF3B3BFF))
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = Spacing.md.dp, end = Spacing.md.dp, bottom = Spacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md.dp)
            ) {
                // En-tête : titre + date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Résumé du jour",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    thickness = 1.dp
                )

                // ── Ligne tâches ──────────────────────────────────────────
                SummaryItem(
                    icon = if (pendingTasks.isEmpty()) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    title = if (pendingTasks.isEmpty()) "Aucune tâche en attente"
                            else "${pendingTasks.size} tâche${if (pendingTasks.size > 1) "s" else ""} à faire",
                    subtitle = taskSubtitle,
                    iconTint = if (pendingTasks.isEmpty()) CompletedGreen else HighOrange,
                    actionLabel = if (pendingTasks.isNotEmpty()) "Voir tout" else null,
                    onAction = onNavigateToTasks
                )

                // ── Ligne événements ──────────────────────────────────────
                SummaryItem(
                    icon = Icons.Default.Event,
                    title = currentOrNextEvent?.title ?: "Aucun événement prévu",
                    subtitle = if (todayEvents.size > 1)
                        "$eventSubtitle  ·  ${todayEvents.size} événements"
                    else
                        eventSubtitle,
                    iconTint = when {
                        currentOrNextEvent == null -> TextTertiary
                        isEventOngoing -> CompletedGreen
                        else -> Color(0xFF9C27B0)
                    },
                    actionLabel = if (todayEvents.isNotEmpty()) "Agenda" else null,
                    onAction = onNavigateToPlanning
                )
            }
        }
    }
}

/**
 * Item de résumé (icône + texte + lien optionnel)
 */
@Composable
private fun SummaryItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icône avec fond circulaire coloré
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Lien "Voir tout" / "Agenda"
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Carte de raccourci rapide avec animation au tap.
 * La hauteur est contrôlée par le modifier extérieur (fillMaxHeight + weight).
 * iconSize : taille de l'icône en dp (défaut 22, hero = 28)
 */
@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    iconSize: Int = 22,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
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
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(horizontal = Spacing.md.dp, vertical = Spacing.sm.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
            ) {
                // Fond circulaire semi-transparent autour de l'icône
                Box(
                    modifier = Modifier
                        .size((iconSize + 20).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
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
