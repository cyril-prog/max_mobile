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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.max.aiassistant.model.*
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
            WeekCalendar()
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        // Navigation en bas de l'écran (accessible au pouce)
        BottomNavigationBar(
            selectedIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
            tasksCount = tasks.size,
            eventsCount = events.size
        )
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
 * Barre de navigation en bas pour basculer entre Tâches et Agenda
 * Positionnée en bas pour un meilleur accès au pouce
 */
@Composable
fun BottomNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tasksCount: Int,
    eventsCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Bouton Tâches
            NavigationButton(
                icon = Icons.Default.CheckCircle,
                label = "Tâches",
                count = tasksCount,
                isSelected = selectedIndex == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Bouton Agenda
            NavigationButton(
                icon = Icons.Default.CalendarToday,
                label = "Agenda",
                count = eventsCount,
                isSelected = selectedIndex == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Bouton de navigation individuel
 */
@Composable
fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentBlue else DarkSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "$count",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
fun WeekCalendar() {
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
        Text(
            text = "WOD", // Work Of Day ou Week Overview Display
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
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
 * Dialog affichant tous les détails d'une tâche
 */
@Composable
fun TaskDetailsDialog(
    task: Task,
    onDismiss: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = task.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statut actuel
                DetailRow(
                    icon = Icons.Default.Info,
                    label = "Statut",
                    value = when (task.status) {
                        TaskStatus.TODO -> "À faire"
                        TaskStatus.IN_PROGRESS -> "En cours"
                        TaskStatus.COMPLETED -> "Terminé"
                    },
                    valueColor = when (task.status) {
                        TaskStatus.COMPLETED -> CompletedGreen
                        TaskStatus.IN_PROGRESS -> AccentBlue
                        else -> NormalOrange
                    }
                )

                // Priorité
                DetailRow(
                    icon = Icons.Default.PriorityHigh,
                    label = "Priorité",
                    value = when (task.priority) {
                        TaskPriority.URGENT -> "Haute"
                        TaskPriority.NORMAL -> "Normale"
                        TaskPriority.LOW -> "Basse"
                    },
                    valueColor = when (task.priority) {
                        TaskPriority.URGENT -> UrgentRed
                        TaskPriority.NORMAL -> NormalOrange
                        TaskPriority.LOW -> CompletedGreen
                    }
                )

                // Deadline
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Échéance",
                    value = task.deadline
                )

                // Catégorie
                if (task.category.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Default.Label,
                        label = "Catégorie",
                        value = task.category
                    )
                }

                // Durée estimée
                if (task.estimatedDuration.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Default.Timer,
                        label = "Durée estimée",
                        value = task.estimatedDuration
                    )
                }

                // Description
                if (task.description.isNotEmpty()) {
                    Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }

                // Note
                if (task.note.isNotEmpty()) {
                    Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = task.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
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
 * Ligne de détail pour le dialog
 */
@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Horaires
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Horaires",
                    value = "${event.startTime} - ${event.endTime}"
                )

                // Lieu si disponible
                if (event.location.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Default.Place,
                        label = "Lieu",
                        value = event.location
                    )
                }

                // Source
                DetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Source",
                    value = event.source
                )

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
