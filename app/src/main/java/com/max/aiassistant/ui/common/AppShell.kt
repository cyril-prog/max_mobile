package com.max.aiassistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.theme.DarkBackground
import com.max.aiassistant.ui.theme.TextPrimary
import com.max.aiassistant.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val ShellBackdrop = Color(0xFF081018)
private val ShellHeaderGradient = Brush.horizontalGradient(
    listOf(
        Color(0xFF16263D),
        Color(0xFF0F1A2A),
        Color(0xFF0A111B)
    )
)
private val ShellDrawerGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF182B45),
        Color(0xFF101C2D),
        Color(0xFF09111B)
    )
)
private val ShellDrawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
private val ShellDockGradient = Brush.horizontalGradient(
    listOf(
        Color(0xFF1B2D46),
        Color(0xFF132033),
        Color(0xFF1A2B43)
    )
)
private val ShellMenuSurface = Color(0xFF1A2940)

enum class AppShellRoute(val title: String, val shortLabel: String, val icon: ImageVector, val primary: Boolean = false) {
    HOME("Accueil", "Accueil", Icons.Default.Home, true),
    CHAT("Chat", "Chat", Icons.AutoMirrored.Filled.Message, true),
    TASKS("Organiser", "Orga", Icons.Default.CheckCircle, true),
    NOTES("Notes", "Notes", Icons.Default.Description, false),
    WEATHER("Meteo", "Meteo", Icons.Default.WbSunny, true),
    VOICE("Vocal", "Vocal", Icons.Default.Mic, false),
    PLANNING("Planning", "Agenda", Icons.Default.CalendarMonth, false),
    ACTU("Actualités", "Actu", Icons.Default.Newspaper, true),
    RADAR("Vue satellite", "Satellite", Icons.Default.SatelliteAlt, false)
}

private val primaryRoutes = listOf(
    AppShellRoute.HOME,
    AppShellRoute.CHAT,
    AppShellRoute.TASKS,
    AppShellRoute.ACTU,
    AppShellRoute.WEATHER
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaxAppShell(
    currentRoute: AppShellRoute,
    drawerSelectionRoute: AppShellRoute = currentRoute,
    isOffline: Boolean,
    isSyncing: Boolean,
    hasLocalData: Boolean,
    onNavigate: (AppShellRoute) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues, () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var queuedNavigation by remember { mutableStateOf<AppShellRoute?>(null) }
    val isImeVisible = WindowInsets.isImeVisible

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(316.dp)
                    .clip(ShellDrawerShape),
                drawerContainerColor = Color.Transparent,
                drawerContentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(ShellDrawerShape)
                        .background(brush = ShellDrawerGradient)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = ShellDrawerShape
                        )
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Navigation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tous les ecrans sont accessibles ici",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            AppShellRoute.HOME,
                            AppShellRoute.CHAT,
                            AppShellRoute.TASKS,
                            AppShellRoute.NOTES,
                            AppShellRoute.WEATHER,
                            AppShellRoute.VOICE,
                            AppShellRoute.PLANNING,
                            AppShellRoute.ACTU,
                            AppShellRoute.RADAR
                        ).forEach { route ->
                            DrawerEntry(
                                route = route,
                                selected = drawerSelectionRoute == route,
                                onClick = {
                                    queuedNavigation = route
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }

                queuedNavigation?.let { target ->
                    onNavigate(target)
                    queuedNavigation = null
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (currentRoute != AppShellRoute.CHAT) {
                    MaxShellTopBar(
                        currentRoute = currentRoute,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            },
            bottomBar = {
                if (!isImeVisible) {
                    MaxBottomDock(
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content(
                    PaddingValues(),
                    { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

@Composable
private fun MaxShellTopBar(
    currentRoute: AppShellRoute,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = ShellHeaderGradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onMenuClick),
            color = ShellMenuSurface,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = currentRoute.title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MaxBottomDock(
    currentRoute: AppShellRoute,
    onNavigate: (AppShellRoute) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ShellBackdrop)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(brush = ShellDockGradient)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryRoutes.forEach { route ->
                    DockItem(
                        route = route,
                        selected = currentRoute == route,
                        onClick = { onNavigate(route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    route: AppShellRoute,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) AccentBlue.copy(alpha = 0.16f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = route.icon,
                contentDescription = route.title,
                tint = if (selected) AccentBlue else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = route.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) TextPrimary else TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DrawerEntry(
    route: AppShellRoute,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = if (selected) AccentBlue.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = route.icon,
                        contentDescription = route.title,
                        tint = if (selected) AccentBlue else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = route.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ecran",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
