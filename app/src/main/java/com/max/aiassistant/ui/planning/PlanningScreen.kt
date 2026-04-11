package com.max.aiassistant.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.max.aiassistant.model.Event
import com.max.aiassistant.ui.common.BannerTone
import com.max.aiassistant.ui.common.EmptyStateView
import com.max.aiassistant.ui.common.InlineStatusBanner
import com.max.aiassistant.ui.common.LoadingStateView
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState

private val PlanInk = Color(0xFF090C12)
private val PlanPanel = Color(0xFF141A28)
private val PlanSoft = Color(0xFF1B2438)
private val PlanBlue = Color(0xFF7EB2FF)
private val PlanMint = Color(0xFF79E7C1)
private val PlanDim = Color(0xFF97A3BC)

private enum class PlanTab { TODAY, NEXT, ALL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    events: List<Event>,
    isRefreshing: Boolean,
    errorMessage: String? = null,
    isOffline: Boolean = false,
    onRefresh: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToActu: () -> Unit = {},
    showChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = { PlanningContent(events, isRefreshing, errorMessage, isOffline, onRefresh, showChrome, modifier) }
    if (showChrome) {
        val sidebarState = rememberNavigationSidebarState()
        NavigationSidebarScaffold(
            currentScreen = NavigationScreen.PLANNING,
            onNavigateToScreen = { screen -> when (screen) {
                NavigationScreen.HOME -> onNavigateToHome()
                NavigationScreen.VOICE -> onNavigateToHome()
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> Unit
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
                NavigationScreen.ACTU -> onNavigateToActu()
            } },
            sidebarState = sidebarState,
            content = content
        )
    } else content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanningContent(events: List<Event>, isRefreshing: Boolean, errorMessage: String?, isOffline: Boolean, onRefresh: () -> Unit, showChrome: Boolean, modifier: Modifier) {
    var tab by remember { mutableStateOf(PlanTab.TODAY) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val today = events.filter { isTodayEvent(it) }
    val next = events.filter { !isTodayEvent(it) }.sortedBy { it.startDateTime.ifBlank { it.date + it.startTime } }
    val visible = when (tab) { PlanTab.TODAY -> today; PlanTab.NEXT -> next.take(12); PlanTab.ALL -> events.sortedBy { it.startDateTime.ifBlank { it.date + it.startTime } } }
    Box(
        modifier
            .fillMaxSize()
            .background(PlanInk)
            .then(if (showChrome) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
    ) {
        Column(Modifier.fillMaxSize()) {
            if (showChrome) Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("ORGANISER", color = PlanBlue, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text("Planning", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                when {
                    isRefreshing && events.isEmpty() -> LoadingStateView("Chargement du planning", "La timeline se met en place.", Modifier.fillMaxSize())
                    else -> LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                        item { PlanSummary(events, today.firstOrNull()) }
                        if (errorMessage != null) item { InlineStatusBanner("Agenda en retrait", errorMessage, BannerTone.Error) } else if (isOffline) item { InlineStatusBanner("Consultation locale", "Les evenements affiches seront resynchronises plus tard.", BannerTone.Offline) }
                        item { PlanTabs(tab, today.size, next.size) { tab = it } }
                        if (visible.isEmpty()) item { EmptyStateView(Icons.Default.CalendarMonth, PlanBlue, "Aucun evenement", "Le planning reste degage tant qu'aucune plage n'est reservee.") }
                        items(visible, key = { it.id }) { event -> EventLane(event) { selectedEvent = event } }
                    }
                }
            }
        }
    }
    selectedEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            containerColor = PlanPanel,
            title = { Text(event.title, color = Color.White, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (event.startTime == "Toute la journee") event.date else "${event.date} - ${event.startTime} a ${event.endTime}", color = PlanDim)
                    if (event.location.isNotBlank()) Text(event.location, color = Color.White)
                    if (event.description.isNotBlank()) Text(event.description, color = PlanDim)
                }
            },
            confirmButton = { TextButton(onClick = { selectedEvent = null }) { Text("Fermer", color = PlanBlue) } }
        )
    }
}

@Composable private fun PlanSummary(events: List<Event>, nextEvent: Event?) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = PlanPanel)) {
        Column(Modifier.padding(20.dp)) {
            Text("Timeline", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Un agenda plus editorial, moins spreadsheet, pour lire la journee d'un coup d'oeil.", color = PlanDim, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanMetric(events.size, "total", PlanBlue, Modifier.weight(1f))
                PlanMetric(events.count { isTodayEvent(it) }, "aujourd'hui", PlanMint, Modifier.weight(1f))
            }
            if (nextEvent != null) {
                Spacer(Modifier.height(16.dp))
                Surface(color = PlanSoft, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Prochain rendez-vous", color = PlanBlue, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(nextEvent.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(if (nextEvent.startTime == "Toute la journee") nextEvent.date else "${nextEvent.date} - ${nextEvent.startTime}", color = PlanDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable private fun PlanMetric(value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = PlanSoft, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) { Text(value.toString(), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(label, color = tint, style = MaterialTheme.typography.labelLarge) } }
}

@Composable private fun PlanTabs(tab: PlanTab, today: Int, next: Int, onSelect: (PlanTab) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PlanTabChip("Aujourd'hui", today, tab == PlanTab.TODAY, Modifier.weight(1f)) { onSelect(PlanTab.TODAY) }
        PlanTabChip("A venir", next, tab == PlanTab.NEXT, Modifier.weight(1f)) { onSelect(PlanTab.NEXT) }
        PlanTabChip("Tout", -1, tab == PlanTab.ALL, Modifier.weight(1f)) { onSelect(PlanTab.ALL) }
    }
}

@Composable private fun PlanTabChip(label: String, count: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), color = if (selected) PlanBlue.copy(alpha = 0.18f) else PlanPanel, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = if (selected) Color.White else PlanDim, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            if (count >= 0) Surface(color = if (selected) PlanBlue else PlanSoft, shape = CircleShape) { Text(count.toString(), color = if (selected) Color(0xFF081120) else Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
        }
    }
}

@Composable private fun EventLane(event: Event, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = PlanPanel)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = PlanBlue.copy(alpha = 0.18f), shape = CircleShape) { Icon(Icons.Default.Schedule, contentDescription = null, tint = PlanBlue, modifier = Modifier.padding(12.dp).size(18.dp)) }
                Spacer(Modifier.height(8.dp))
                Text(if (event.startTime == "Toute la journee") "Jour" else event.startTime, color = PlanBlue, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(event.date, color = PlanDim, style = MaterialTheme.typography.bodySmall)
                if (event.location.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PlanMint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(event.location, color = PlanDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun isTodayEvent(event: Event): Boolean = event.startDateTime.startsWith(java.time.LocalDate.now().toString()) || event.date.contains("Aujourd", ignoreCase = true)
