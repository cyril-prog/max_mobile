package com.max.aiassistant.ui.tasks

import android.os.Build
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.max.aiassistant.model.*
import com.max.aiassistant.ui.common.MiniFluidOrb
import com.max.aiassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ÉCRAN GAUCHE : Tâches & Planning
 *
 * Affiche :
 * - Mini calendrier de la semaine en haut (toujours visible)
 * - Onglets pour basculer entre Tâches et Agenda
 * - Contenu selon l'onglet sélectionné
 */
@Composable
fun TasksScreen(
    tasks: List<Task>,
    events: List<Event>,
    isRefreshing: Boolean,
    isRefreshingEvents: Boolean,
    onRefresh: () -> Unit,
    onRefreshEvents: () -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskDelete: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Mini calendrier de la semaine (toujours visible)
        Column(modifier = Modifier.padding(16.dp)) {
            WeekCalendar(onNavigateToHome = onNavigateToHome)

            Spacer(modifier = Modifier.height(16.dp))

            // Sélecteur d'onglets (Tâches / Événements)
            TabSelector(
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contenu selon l'onglet sélectionné
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> TasksContent(
                    tasks = tasks,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onTaskClick = { selectedTask = it },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> AgendaContent(
                    events = events,
                    isRefreshing = isRefreshingEvents,
                    onRefresh = onRefreshEvents,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Dialog des détails de la tâche
    selectedTask?.let { task ->
        TaskDetailsDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onStatusChange = { newStatus ->
                onTaskStatusChange(task.id, newStatus)
                selectedTask = null
            },
            onDelete = {
                onTaskDelete(task.id)
                selectedTask = null
            }
        )
    }
}

/**
 * Sélecteur d'onglets moderne (Tâches / Événements)
 * Avec indicateur bleu sur l'onglet actif
 */
@Composable
fun TabSelector(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Onglet Tâches
        TabItem(
            text = "Tâches",
            isSelected = selectedIndex == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )

        // Onglet Événements
        TabItem(
            text = "Événements",
            isSelected = selectedIndex == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Item d'onglet individuel
 */
@Composable
fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AccentBlue else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Contenu de l'onglet Tâches
 */
@Composable
fun TasksContent(
    tasks: List<Task>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        // Liste des tâches
        if (tasks.isEmpty() && !isRefreshing) {
            // État vide
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune tâche",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Glissez vers le bas pour actualiser",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemCompact(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

/**
 * Contenu de l'onglet Agenda
 */
@Composable
fun AgendaContent(
    events: List<Event>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        // Liste des événements
        if (events.isEmpty() && !isRefreshing) {
            // État vide
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucun événement",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Glissez vers le bas pour actualiser",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventItem(
                        event = event,
                        onClick = { selectedEvent = event }
                    )
                }
            }
        }
    }

    // Dialog des détails de l'événement
    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedEvent = null }
        )
    }
}

/**
 * Mini calendrier affichant les 7 jours de la semaine
 * Le jour actuel est mis en évidence
 */
@Composable
fun WeekCalendar(onNavigateToHome: () -> Unit) {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)

    // Calcule le lundi de la semaine actuelle
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val weekDays = (0..6).map { offset ->
        val day = calendar.clone() as Calendar
        day.add(Calendar.DAY_OF_MONTH, offset)
        day
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Espace pour éviter que l'orbe soit coupé
        Spacer(modifier = Modifier.height(8.dp))

        // Orbe bleu miniature (lien visuel avec les autres écrans)
        MiniFluidOrb(
            onClick = onNavigateToHome,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekDays.forEach { day ->
                val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
                val dayName = SimpleDateFormat("EEE", Locale.FRENCH)
                    .format(day.time)
                    .uppercase()

                CalendarDayItem(
                    dayName = dayName,
                    dayNumber = dayOfMonth,
                    isToday = dayOfMonth == today
                )
            }
        }
    }
}

/**
 * Item d'un jour dans le calendrier
 */
@Composable
fun CalendarDayItem(
    dayName: String,
    dayNumber: Int,
    isToday: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) AccentBlue else Color.Transparent)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) Color.White else TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) Color.White else TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Item compact d'une tâche (vue liste)
 * Affiche le titre, deadline et badge de statut/priorité
 */
@Composable
fun TaskItemCompact(
    task: Task,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône deadline
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary
                )
                Text(
                    text = task.deadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Catégorie si disponible
                if (task.category.isNotEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Badge de statut/priorité
        val (badgeColor, badgeIcon) = when {
            task.status == TaskStatus.COMPLETED -> CompletedGreen to Icons.Default.CheckCircle
            task.status == TaskStatus.IN_PROGRESS -> AccentBlue to Icons.Default.Refresh
            task.priority == TaskPriority.URGENT -> UrgentRed to Icons.Default.PriorityHigh
            else -> NormalOrange to Icons.Default.Circle
        }

        Surface(
            shape = CircleShape,
            color = badgeColor.copy(alpha = 0.2f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Dialog affichant tous les détails d'une tâche (version compacte)
 */
@Composable
fun TaskDetailsDialog(
    task: Task,
    onDismiss: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = task.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Métadonnées compactes (format horizontal)
                CompactMetadata(task = task)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Onglets Description / Notes
                if (task.description.isNotEmpty() || task.note.isNotEmpty()) {
                    TaskContentTabSelector(
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        hasDescription = task.description.isNotEmpty(),
                        hasNote = task.note.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Contenu selon l'onglet sélectionné
                    when (selectedTab) {
                        0 -> {
                            if (task.description.isNotEmpty()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    lineHeight = 20.sp
                                )
                            } else {
                                Text(
                                    text = "Aucune description",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                        1 -> {
                            if (task.note.isNotEmpty()) {
                                Text(
                                    text = task.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    lineHeight = 20.sp
                                )
                            } else {
                                Text(
                                    text = "Aucune note",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Bouton de changement de statut
            TextButton(
                onClick = {
                    val newStatus = when (task.status) {
                        TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                        TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                        TaskStatus.COMPLETED -> TaskStatus.TODO
                    }
                    onStatusChange(newStatus)
                }
            ) {
                Text(
                    text = when (task.status) {
                        TaskStatus.TODO -> "Démarrer"
                        TaskStatus.IN_PROGRESS -> "Terminer"
                        TaskStatus.COMPLETED -> "Réactiver"
                    },
                    color = AccentBlue
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bouton supprimer
                TextButton(onClick = onDelete) {
                    Text("Supprimer", color = UrgentRed)
                }
                // Bouton fermer
                TextButton(onClick = onDismiss) {
                    Text("Fermer", color = TextSecondary)
                }
            }
        }
    )
}

/**
 * Métadonnées compactes affichées horizontalement
 * Format: "À faire • Normale • 6 jours • Travail • 2h"
 */
@Composable
fun CompactMetadata(task: Task) {
    val statusText = when (task.status) {
        TaskStatus.TODO -> "À faire"
        TaskStatus.IN_PROGRESS -> "En cours"
        TaskStatus.COMPLETED -> "Terminé"
    }
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> CompletedGreen
        TaskStatus.IN_PROGRESS -> AccentBlue
        else -> NormalOrange
    }

    val priorityText = when (task.priority) {
        TaskPriority.URGENT -> "Haute"
        TaskPriority.NORMAL -> "Normale"
        TaskPriority.LOW -> "Basse"
    }
    val priorityColor = when (task.priority) {
        TaskPriority.URGENT -> UrgentRed
        TaskPriority.NORMAL -> NormalOrange
        TaskPriority.LOW -> CompletedGreen
    }

    // Construction de la chaîne de métadonnées
    val metadata = buildList {
        add(statusText to statusColor)
        add(priorityText to priorityColor)
        add(task.deadline to TextPrimary)
        if (task.category.isNotEmpty()) add(task.category to TextPrimary)
        if (task.estimatedDuration.isNotEmpty()) add(task.estimatedDuration to TextPrimary)
    }

    // Affichage sur deux lignes si nécessaire
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Première ligne (statut, priorité, deadline)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            metadata.take(3).forEachIndexed { index, (text, color) ->
                if (index > 0) {
                    Text(
                        text = "•",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = text,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Deuxième ligne si nécessaire (catégorie, durée)
        if (metadata.size > 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                metadata.drop(3).forEachIndexed { index, (text, color) ->
                    if (index > 0) {
                        Text(
                            text = "•",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = text,
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Sélecteur d'onglets pour Description / Notes
 */
@Composable
fun TaskContentTabSelector(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hasDescription: Boolean,
    hasNote: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (hasDescription) {
            TaskContentTabItem(
                text = "Description",
                isSelected = selectedIndex == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
        }

        if (hasNote) {
            TaskContentTabItem(
                text = "Notes",
                isSelected = selectedIndex == 1 || (!hasDescription && selectedIndex == 0),
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Item d'onglet pour le contenu de tâche
 */
@Composable
fun TaskContentTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) AccentBlue else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Ligne de détail simple pour les événements
 */
@Composable
fun EventDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

/**
 * Item d'un événement du planning
 */
@Composable
fun EventItem(
    event: Event,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${event.startTime} • ${event.endTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }

        // Icône de l'événement
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Calendar event",
            tint = AccentBlue,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .padding(6.dp)
        )
    }
}

/**
 * Composable pour afficher du HTML formaté
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    fontSize: Float = 14f
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = fontSize
                setTextColor(color.toArgb())
                // Utiliser le thème pour les liens, etc.
                setLinkTextColor(AccentBlue.toArgb())
            }
        },
        update = { textView ->
            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
            textView.text = spanned
        }
    )
}

/**
 * Dialog affichant tous les détails d'un événement
 */
@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = event.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horaires
                EventDetailRow(
                    icon = Icons.Default.Schedule,
                    value = "${event.startTime} - ${event.endTime}"
                )

                // Lieu si disponible
                if (event.location.isNotEmpty()) {
                    EventDetailRow(
                        icon = Icons.Default.Place,
                        value = event.location
                    )
                }

                // Description si disponible
                if (event.description.isNotEmpty()) {
                    HorizontalDivider(
                        color = DarkSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        HtmlText(
                            html = event.description,
                            color = TextPrimary,
                            fontSize = 14f
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = AccentBlue)
            }
        }
    )
}
