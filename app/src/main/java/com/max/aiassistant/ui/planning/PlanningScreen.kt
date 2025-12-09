package com.max.aiassistant.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.max.aiassistant.model.Event
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.tasks.AgendaContent
import com.max.aiassistant.ui.theme.*

/**
 * ÉCRAN PLANNING : Agenda et événements
 *
 * Affiche :
 * - Vue semaine ou mois des événements
 * - Navigation entre les semaines/mois
 * - Sidebar de navigation accessible par swipe vers la gauche depuis le bord droit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    events: List<Event>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()
    
    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.PLANNING,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.HOME -> onNavigateToHome()
                NavigationScreen.VOICE -> onNavigateToHome() // Retourne à Home pour accéder à Voice
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> { /* Déjà sur cet écran */ }
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
            }
        },
        sidebarState = sidebarState
    ) {
        PlanningScreenContent(
            events = events,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier
        )
    }
}

/**
 * Contenu de l'écran Planning
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanningScreenContent(
    events: List<Event>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Espace en haut (aligné avec l'écran Tâches)
        Spacer(modifier = Modifier.height(48.dp))

        // En-tête avec titre
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Planning",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contenu du planning (réutilise AgendaContent de TasksScreen)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            AgendaContent(
                events = events,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
