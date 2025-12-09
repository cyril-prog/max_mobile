package com.max.aiassistant.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Identifiants des écrans de navigation
 */
enum class NavigationScreen {
    HOME,     // Page 0 - Dashboard d'accueil
    VOICE,    // Page 1 - Écran principal voice to voice
    CHAT,     // Page 2 - Chat messenger
    TASKS,    // Page 3 - Tâches
    PLANNING, // Page 4 - Planning/Agenda
    WEATHER,  // Page 5 - Météo
    NOTES     // Page 6 - Notes
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
 * Sidebar de navigation avec effet de transition liquide
 * L'indicateur de sélection "coule" entre les boutons
 * Le tracking du doigt suit le mouvement même entre les boutons
 */
@Composable
private fun FloatingNavigationSidebar(
    currentScreen: NavigationScreen,
    onNavigateToScreen: (NavigationScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val screens = listOf(
        Pair(NavigationScreen.HOME, Icons.Default.Home),
        Pair(NavigationScreen.VOICE, Icons.Default.Mic),
        Pair(NavigationScreen.CHAT, Icons.AutoMirrored.Filled.Message),
        Pair(NavigationScreen.TASKS, Icons.Default.CheckCircle),
        Pair(NavigationScreen.PLANNING, Icons.Default.CalendarMonth),
        Pair(NavigationScreen.WEATHER, Icons.Default.WbSunny),
        Pair(NavigationScreen.NOTES, Icons.Default.Edit)
    )
    
    val currentIndex = screens.indexOfFirst { it.first == currentScreen }
    val coroutineScope = rememberCoroutineScope()
    
    // État pour tracker le bouton pressé (preview)
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Position cible de l'indicateur (pressé ou sélectionné)
    val targetIndex = pressedIndex ?: currentIndex
    
    // Animation de la position de l'indicateur liquide
    val indicatorPosition by animateFloatAsState(
        targetValue = targetIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquidIndicator"
    )
    
    // Animation d'étirement pendant le mouvement (effet blob)
    val isMoving = pressedIndex != null && pressedIndex != currentIndex
    val stretchFactor by animateFloatAsState(
        targetValue = if (isMoving) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "stretch"
    )
    
    // Pulsation du halo
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val buttonSize = 48.dp
    val buttonSpacing = 8.dp
    val density = LocalDensity.current
    
    // Fonction pour naviguer avec délai d'animation
    fun navigateWithAnimation(targetScreenIndex: Int) {
        if (targetScreenIndex in screens.indices) {
            val distance = abs(targetScreenIndex - currentIndex)
            // Délai basé sur la distance (environ 150ms par bouton)
            val animationDelay = (distance * 150L).coerceIn(100L, 400L)
            
            coroutineScope.launch {
                delay(animationDelay)
                onNavigateToScreen(screens[targetScreenIndex].first)
                // Réinitialiser pressedIndex seulement après la navigation
                pressedIndex = null
            }
        }
    }

    Box(
        modifier = modifier
            .padding(end = 8.dp)
    ) {
        // Container avec fond et gestion globale du touch
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1B2A).copy(alpha = 0.95f),
                            Color(0xFF1B2838).copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(vertical = 12.dp, horizontal = 10.dp)
                .pointerInput(screens.size) {
                    val buttonSizePx = buttonSize.toPx()
                    val spacingPx = buttonSpacing.toPx()
                    val totalHeightPerButton = buttonSizePx + spacingPx
                    
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            // Calculer quel bouton est sous le doigt
                            val index = (offset.y / totalHeightPerButton).toInt()
                                .coerceIn(0, screens.size - 1)
                            pressedIndex = index
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            // Mettre à jour l'index selon la position Y du doigt
                            val index = (change.position.y / totalHeightPerButton).toInt()
                                .coerceIn(0, screens.size - 1)
                            pressedIndex = index
                        },
                        onDragEnd = {
                            // Navigation vers le bouton sous le doigt au release
                            // Pour le drag, la lumière a déjà suivi le doigt, navigation immédiate
                            pressedIndex?.let { index ->
                                if (index in screens.indices && index != currentIndex) {
                                    onNavigateToScreen(screens[index].first)
                                }
                            }
                            pressedIndex = null
                            isDragging = false
                        },
                        onDragCancel = {
                            // Annulation : retour à la position initiale
                            pressedIndex = null
                            isDragging = false
                        }
                    )
                }
                .pointerInput(screens.size) {
                    val buttonSizePx = buttonSize.toPx()
                    val spacingPx = buttonSpacing.toPx()
                    val totalHeightPerButton = buttonSizePx + spacingPx
                    
                    // Détection des taps simples (clic rapide sans drag)
                    detectTapGestures(
                        onPress = { offset ->
                            // Calculer quel bouton est sous le doigt
                            val index = (offset.y / totalHeightPerButton).toInt()
                                .coerceIn(0, screens.size - 1)
                            pressedIndex = index
                        },
                        onTap = { offset ->
                            // Navigation au relâchement du tap avec délai pour l'animation
                            val index = (offset.y / totalHeightPerButton).toInt()
                                .coerceIn(0, screens.size - 1)
                            if (index != currentIndex) {
                                // pressedIndex reste actif jusqu'à la fin de navigateWithAnimation
                                navigateWithAnimation(index)
                            } else {
                                // Si on tape sur le bouton actuel, reset immédiat
                                pressedIndex = null
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            // Canvas pour dessiner l'indicateur liquide DERRIÈRE les boutons
            Box(modifier = Modifier.size(buttonSize, buttonSize * screens.size + buttonSpacing * (screens.size - 1))) {
                // Indicateur liquide animé
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val buttonSizePx = buttonSize.toPx()
                    val spacingPx = buttonSpacing.toPx()
                    val totalHeightPerButton = buttonSizePx + spacingPx
                    
                    // Position Y de l'indicateur
                    val indicatorY = indicatorPosition * totalHeightPerButton + buttonSizePx / 2
                    
                    // Calcul de l'étirement vertical (effet blob)
                    val baseRadius = buttonSizePx / 2 * 1.15f
                    val stretchedHeight = baseRadius * stretchFactor
                    
                    val centerX = size.width / 2
                    
                    // Halo externe (glow diffus)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0A84FF).copy(alpha = 0.3f * haloPulse),
                                Color(0xFF0A84FF).copy(alpha = 0.15f * haloPulse),
                                Color.Transparent
                            ),
                            center = Offset(centerX, indicatorY),
                            radius = baseRadius * 1.8f
                        ),
                        center = Offset(centerX, indicatorY),
                        radius = baseRadius * 1.8f
                    )
                    
                    // Forme blob liquide (ellipse déformée pendant le mouvement)
                    if (stretchFactor > 1.05f) {
                        // Pendant le mouvement : forme étirée type blob
                        val path = Path().apply {
                            val topY = indicatorY - stretchedHeight * 0.6f
                            val bottomY = indicatorY + stretchedHeight * 0.6f
                            val controlOffset = baseRadius * 0.8f
                            
                            moveTo(centerX, topY)
                            
                            cubicTo(
                                centerX + controlOffset * 1.3f, topY + stretchedHeight * 0.2f,
                                centerX + controlOffset * 1.3f, bottomY - stretchedHeight * 0.2f,
                                centerX, bottomY
                            )
                            
                            cubicTo(
                                centerX - controlOffset * 1.3f, bottomY - stretchedHeight * 0.2f,
                                centerX - controlOffset * 1.3f, topY + stretchedHeight * 0.2f,
                                centerX, topY
                            )
                            close()
                        }
                        
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A84FF).copy(alpha = 0.5f),
                                    Color(0xFF1E5AAF).copy(alpha = 0.4f)
                                )
                            )
                        )
                        
                        drawPath(
                            path = path,
                            color = Color(0xFF0A84FF).copy(alpha = 0.7f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    } else {
                        // Au repos : cercle avec halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0A84FF).copy(alpha = 0.45f),
                                    Color(0xFF1E5AAF).copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, indicatorY),
                                radius = baseRadius
                            ),
                            center = Offset(centerX, indicatorY),
                            radius = baseRadius
                        )
                        
                        drawCircle(
                            color = Color(0xFF0A84FF).copy(alpha = 0.6f * haloPulse),
                            center = Offset(centerX, indicatorY),
                            radius = baseRadius * 0.95f,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                
                // Boutons superposés (sans gestion du touch, juste affichage)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    screens.forEachIndexed { index, (screen, icon) ->
                        LiquidNavButton(
                            icon = icon,
                            isSelected = index == currentIndex,
                            isPressed = index == pressedIndex
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bouton de navigation pour l'effet liquide
 * Affichage uniquement - le touch est géré au niveau parent
 */
@Composable
private fun LiquidNavButton(
    icon: ImageVector,
    isSelected: Boolean,
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation de scale
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isSelected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    
    // Couleur de fond
    val backgroundColor = Color(0xFF152238)
    
    // Couleur de l'icône
    val iconColor = when {
        isPressed || isSelected -> Color.White
        else -> Color.White.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor),
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

