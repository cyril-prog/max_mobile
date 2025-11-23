package com.max.aiassistant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.max.aiassistant.utils.PermissionHelper
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.max.aiassistant.model.TaskStatus
import com.max.aiassistant.ui.chat.ChatScreen
import com.max.aiassistant.ui.tasks.TasksScreen
import com.max.aiassistant.ui.theme.MaxTheme
import com.max.aiassistant.ui.voice.VoiceScreen
import com.max.aiassistant.ui.voice.FluidOrbVisualizer
import com.max.aiassistant.viewmodel.MainViewModel
import kotlin.math.sqrt

/**
 * Activité principale de l'application Max - AI Assistant
 *
 * Single-Activity architecture avec Jetpack Compose
 *
 * Gère la navigation entre 3 écrans via HorizontalPager :
 * - Page 0 : VoiceScreen (Voice to Voice) - ÉCRAN PAR DÉFAUT
 * - Page 1 : ChatScreen (Messenger)
 * - Page 2 : TasksScreen (Tâches & Planning)
 *
 * L'utilisateur peut swiper horizontalement entre les pages
 * ou utiliser les boutons de navigation dans Voice et Chat
 */
class MainActivity : ComponentActivity() {

    // Gestionnaire de permissions
    private lateinit var permissionHelper: PermissionHelper

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialise le gestionnaire de permissions
        permissionHelper = PermissionHelper(
            activity = this,
            onPermissionGranted = {
                // Permission accordée, rien à faire (le toggle se fera normalement)
            },
            onPermissionDenied = {
                // Permission refusée, affiche un message
                Toast.makeText(
                    this,
                    "Permission microphone requise pour utiliser l'API Realtime",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
        permissionHelper.initPermissionLauncher()

        setContent {
            MaxTheme {
                // ViewModel partagé par tous les écrans
                val viewModel: MainViewModel = viewModel()

                // Collecte de l'état réactif depuis le ViewModel
                val messages by viewModel.messages.collectAsState()
                val tasks by viewModel.tasks.collectAsState()
                val events by viewModel.events.collectAsState()
                val isLoadingTasks by viewModel.isLoadingTasks.collectAsState()
                val isLoadingEvents by viewModel.isLoadingEvents.collectAsState()
                val isRealtimeConnected by viewModel.isRealtimeConnected.collectAsState()
                val voiceTranscript by viewModel.voiceTranscript.collectAsState()

                // État du pager (3 pages, commence à la page 0 = Voice)
                val pagerState = rememberPagerState(
                    initialPage = 0, // Démarre sur VoiceScreen (écran principal)
                    pageCount = { 3 }
                )

                // Scope pour les animations de navigation
                val coroutineScope = rememberCoroutineScope()

                // État pour l'animation de transition futuriste
                var isTransitioning by remember { mutableStateOf(false) }
                var targetPage by remember { mutableIntStateOf(0) }

                // Box pour superposer l'animation de transition
                Box(modifier = Modifier.fillMaxSize()) {
                    // HorizontalPager pour le swipe entre les écrans
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isTransitioning // Désactive le swipe pendant la transition
                    ) { page ->
                    when (page) {
                        // PAGE 0 : Écran Voice (écran principal)
                        0 -> {
                            VoiceScreen(
                                isRealtimeConnected = isRealtimeConnected,
                                transcript = voiceTranscript,
                                onToggleRealtime = {
                                    // Vérifie la permission avant de basculer
                                    if (permissionHelper.hasRecordAudioPermission()) {
                                        viewModel.toggleRealtimeConnection()
                                    } else {
                                        // Demande la permission
                                        permissionHelper.requestRecordAudioPermission()
                                    }
                                },
                                onNavigateToChat = {
                                    // Déclenche l'animation de transition futuriste vers le chat
                                    targetPage = 1
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    // Navigation fluide vers TasksScreen (page 2)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                }
                            )
                        }

                        // PAGE 1 : Écran Chat
                        1 -> {
                            ChatScreen(
                                messages = messages,
                                onSendMessage = { message ->
                                    viewModel.sendMessage(message)
                                },
                                onVoiceInput = {
                                    // Navigation fluide vers VoiceScreen (page 0)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }
                            )
                        }

                        // PAGE 2 : Écran Tâches & Planning
                        2 -> {
                            TasksScreen(
                                tasks = tasks,
                                events = events,
                                isRefreshing = isLoadingTasks,
                                isRefreshingEvents = isLoadingEvents,
                                onRefresh = { viewModel.refreshTasks() },
                                onRefreshEvents = { viewModel.refreshCalendarEvents() },
                                onTaskStatusChange = { taskId, newStatus ->
                                    viewModel.updateTaskStatus(taskId, newStatus)
                                },
                                onTaskDelete = { taskId ->
                                    viewModel.deleteTask(taskId)
                                }
                            )
                        }
                    }
                    }

                    // Overlay d'animation de transition futuriste
                    if (isTransitioning) {
                        FuturisticTransition(
                            onPageChange = {
                                // Appelé quand le cercle est au maximum (milieu de l'animation)
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetPage)
                                }
                            },
                            onTransitionComplete = {
                                // Appelé à la fin complète de l'animation
                                isTransitioning = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animation de transition futuriste en deux phases
 * Phase 1 : Le cercle bleu s'agrandit progressivement jusqu'à remplir l'écran
 * Phase 2 : Le cercle rétrécit et disparaît, révélant le nouvel écran
 */
@Composable
fun FuturisticTransition(
    onPageChange: () -> Unit,
    onTransitionComplete: () -> Unit
) {
    // Configuration de l'écran
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val screenDiagonal = sqrt(
        (screenWidth.value * screenWidth.value) + (screenHeight.value * screenHeight.value)
    )

    // États de l'animation
    var animationPhase by remember { mutableStateOf(-1) }
    // -1 = initial (taille normale du cercle)
    // 0 = expansion (cercle grandit)
    // 1 = page change (cercle au maximum)
    // 2 = contraction (cercle rétrécit)

    // Offset vertical initial (position du cercle dans VoiceScreen)
    val initialVerticalOffset = -screenHeight * 0.22f // -22% vers le haut

    // État de l'offset avec valeur initiale explicite
    var verticalOffset by remember { mutableStateOf(initialVerticalOffset) }

    // Échelle cible en fonction de la phase
    val targetScale = when (animationPhase) {
        -1 -> 1f                     // Phase initiale : taille normale (280dp)
        0 -> screenDiagonal / 140f   // Phase expansion : grandit jusqu'à remplir l'écran
        else -> 0f                   // Phase contraction : rétrécit jusqu'à disparaître
    }

    // Animation de scale pour le cercle
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = 900, // 900ms par phase pour une animation plus douce
            easing = FastOutSlowInEasing
        ),
        label = "circleScale"
    )

    // Animation de l'offset uniquement pendant l'expansion (phase 0)
    LaunchedEffect(animationPhase) {
        if (animationPhase == 0) {
            // Anime l'offset de sa position actuelle (en haut) vers le centre
            androidx.compose.animation.core.animate(
                initialValue = initialVerticalOffset.value,
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 900,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                verticalOffset = value.dp
            }
        }
    }

    // Gestion de la séquence d'animation
    LaunchedEffect(Unit) {
        // Phase initiale : cercle à sa taille normale
        animationPhase = -1
        delay(50) // Court délai pour s'assurer que le rendu initial est fait

        // Phase 1 : Expansion (cercle grandit)
        animationPhase = 0
        delay(900) // Attendre la fin de l'expansion

        // Phase 2 : Changer de page (quand le cercle est au maximum)
        animationPhase = 1
        onPageChange() // Change de page (mais garde l'overlay visible)
        delay(100) // Court délai pour que le changement de page soit effectif

        // Phase 3 : Contraction (cercle rétrécit)
        animationPhase = 2
        delay(900) // Attendre la fin de la contraction

        // Fin de l'animation : masquer l'overlay
        onTransitionComplete()
    }

    // Overlay avec le cercle animé
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Affiche le cercle pendant toutes les phases sauf quand complètement disparu
        if (scale > 0.01f) {
            Box(
                modifier = Modifier
                    .size(280.dp) // Taille de base de l'orbe
                    .scale(scale)
                    .offset(y = verticalOffset), // Offset animé : part d'en haut, puis se centre
                contentAlignment = Alignment.Center
            ) {
                // Utilise le même visualiseur fluide que dans VoiceScreen
                FluidOrbVisualizer(
                    isActive = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * NOTES POUR L'IMPLÉMENTATION FUTURE :
 *
 * 1. RECONNAISSANCE VOCALE (Voice-to-Text)
 *    - Ajouter la permission dans AndroidManifest.xml :
 *      <uses-permission android:name="android.permission.RECORD_AUDIO" />
 *    - Utiliser android.speech.SpeechRecognizer
 *    - Ou intégrer Google Cloud Speech-to-Text API
 *    - Gérer les demandes de permissions runtime
 *
 * 2. APPEL API IA
 *    - Ajouter Retrofit ou Ktor pour les appels HTTP
 *    - Configurer l'endpoint de l'API (OpenAI, Claude, etc.)
 *    - Stocker la clé API de manière sécurisée (BuildConfig ou service backend)
 *    - Implémenter la gestion d'erreurs réseau
 *    - Ajouter un indicateur de chargement pendant les requêtes
 *
 * 3. INTÉGRATION GOOGLE CALENDAR
 *    - Ajouter les dépendances Google Play Services
 *    - Configurer OAuth 2.0 pour l'authentification
 *    - Utiliser CalendarContract pour lire les événements
 *    - Permissions requises :
 *      <uses-permission android:name="android.permission.READ_CALENDAR" />
 *
 * 4. PERSISTENCE DES DONNÉES
 *    - Implémenter Room Database pour sauvegarder les messages et tâches
 *    - Ou utiliser DataStore pour les préférences
 *
 * 5. NOTIFICATIONS
 *    - Ajouter des rappels pour les tâches urgentes
 *    - Notification lors de nouveaux messages de Max
 *
 * 6. TESTS
 *    - Ajouter des tests unitaires pour le ViewModel
 *    - Tests UI avec Compose Testing
 */
