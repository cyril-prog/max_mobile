package com.max.aiassistant.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.max.aiassistant.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Identifiants des écrans de navigation
 */
enum class NavigationScreen {
    VOICE,    // Page 0 - Écran principal voice to voice
    CHAT,     // Page 1 - Chat messenger
    TASKS,    // Page 2 - Tâches & Planning
    WEATHER,  // Page 3 - Météo
    NOTES     // Page 4 - Notes
}

/**
 * État de la sidebar de navigation
 */
@Composable
fun rememberNavigationSidebarState(): NavigationSidebarState {
    return remember { NavigationSidebarState() }
}

class NavigationSidebarState {
    var isOpen by mutableStateOf(false)
    var dragOffset by mutableStateOf(0f)
    
    fun open() {
        isOpen = true
        dragOffset = 0f
    }
    
    fun close() {
        isOpen = false
        dragOffset = 0f
    }
}

/**
 * Conteneur avec sidebar de navigation
 * Englobe le contenu de l'écran et gère le geste de swipe vers la gauche
 * pour afficher la sidebar
 */
@Composable
fun NavigationSidebarScaffold(
    currentScreen: NavigationScreen,
    onNavigateToScreen: (NavigationScreen) -> Unit,
    sidebarState: NavigationSidebarState = rememberNavigationSidebarState(),
    content: @Composable () -> Unit
) {
    val sidebarWidth = 80.dp
    val dragThreshold = 60f
    
    // Animation d'ouverture/fermeture avec rebond
    val sidebarOffset by animateDpAsState(
        targetValue = if (sidebarState.isOpen) 0.dp else sidebarWidth + 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sidebarOffset"
    )
    
    // Animation de l'overlay
    val overlayAlpha by animateFloatAsState(
        targetValue = if (sidebarState.isOpen) 0.5f else 0f,
        animationSpec = tween(250),
        label = "overlayAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Contenu principal
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        
        // Zone de détection du swipe sur le bord droit (prioritaire)
        // Cette zone invisible capture les gestes avant le HorizontalPager
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(40.dp)
                .pointerInput(Unit) {
                    var isDragging = false
                    
                    detectHorizontalDragGestures(
                        onDragStart = { _ ->
                            isDragging = true
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (isDragging && dragAmount < 0) {
                                // Drag vers la gauche
                                sidebarState.dragOffset -= dragAmount
                            }
                        },
                        onDragEnd = {
                            if (isDragging) {
                                if (sidebarState.dragOffset >= dragThreshold) {
                                    sidebarState.open()
                                } else {
                                    sidebarState.close()
                                }
                            }
                            isDragging = false
                            sidebarState.dragOffset = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            sidebarState.dragOffset = 0f
                        }
                    )
                }
        )
        
        // Overlay sombre quand la sidebar est ouverte
        if (sidebarState.isOpen || overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(overlayAlpha)
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        sidebarState.close()
                    }
            )
        }
        
        // Sidebar flottante
        FloatingNavigationSidebar(
            currentScreen = currentScreen,
            onNavigateToScreen = { screen ->
                sidebarState.close()
                onNavigateToScreen(screen)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(sidebarOffset.roundToPx(), 0) }
        )
    }
}

/**
 * Sidebar de navigation simple et élégante
 * Design harmonisé avec la sphère et les boutons principaux
 */
@Composable
private fun FloatingNavigationSidebar(
    currentScreen: NavigationScreen,
    onNavigateToScreen: (NavigationScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val screens = listOf(
        Pair(NavigationScreen.VOICE, Icons.Default.Mic),
        Pair(NavigationScreen.CHAT, Icons.AutoMirrored.Filled.Message),
        Pair(NavigationScreen.TASKS, Icons.Default.CheckCircle),
        Pair(NavigationScreen.WEATHER, Icons.Default.WbSunny),
        Pair(NavigationScreen.NOTES, Icons.Default.Edit)
    )

    // Container avec fond bleu profond harmonisé
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A).copy(alpha = 0.95f), // Bleu très foncé
                        Color(0xFF1B2838).copy(alpha = 0.95f)  // Bleu foncé
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        screens.forEach { (screen, icon) ->
            SimpleNavButton(
                icon = icon,
                isSelected = screen == currentScreen,
                onClick = { onNavigateToScreen(screen) }
            )
        }
    }
}

/**
 * Bouton de navigation harmonisé avec le style de l'app
 * Mêmes couleurs que les boutons principaux et la sphère
 */
@Composable
private fun SimpleNavButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // État hover/press
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    // Animation de scale fluide
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isHovered -> 1.15f
            isSelected -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    
    // Couleurs harmonisées avec la sphère et les boutons principaux
    val backgroundColor = if (isSelected) {
        Color(0xFF1E3A5F) // Bleu profond (même que MaxMessageBg et la sphère)
    } else {
        Color(0xFF152238) // Bleu foncé subtil
    }
    
    val iconColor = if (isSelected) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        isHovered = true
                        tryAwaitRelease()
                        isPressed = false
                        isHovered = false
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
