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
import com.max.aiassistant.ui.notes.NotesScreen
import com.max.aiassistant.ui.tasks.TasksScreen
import com.max.aiassistant.ui.theme.MaxTheme
import com.max.aiassistant.ui.voice.VoiceScreen
import com.max.aiassistant.ui.voice.FluidOrbVisualizer
import com.max.aiassistant.ui.weather.WeatherScreen
import com.max.aiassistant.viewmodel.MainViewModel
import kotlin.math.sqrt

/**
 * Activité principale de l'application Max - AI Assistant
 *
 * Single-Activity architecture avec Jetpack Compose
 *
 * Gère la navigation entre 5 écrans via HorizontalPager :
 * - Page 0 : VoiceScreen (Voice to Voice) - ÉCRAN PAR DÉFAUT
 * - Page 1 : ChatScreen (Messenger)
 * - Page 2 : TasksScreen (Tâches & Planning)
 * - Page 3 : WeatherScreen (Météo)
 * - Page 4 : NotesScreen (Prise de notes)
 *
 * L'utilisateur peut naviguer entre les pages via les boutons de navigation
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
                val weatherData by viewModel.weatherData.collectAsState()
                val isLoadingWeather by viewModel.isLoadingWeather.collectAsState()
                val notes by viewModel.notes.collectAsState()
                val cityName by viewModel.cityName.collectAsState()
                val citySearchResults by viewModel.citySearchResults.collectAsState()

                // État du pager (5 pages, commence à la page 0 = Voice)
                val pagerState = rememberPagerState(
                    initialPage = 0, // Démarre sur VoiceScreen (écran principal)
                    pageCount = { 5 }
                )

                // Scope pour les animations de navigation
                val coroutineScope = rememberCoroutineScope()

                // État pour l'animation de transition futuriste
                var isTransitioning by remember { mutableStateOf(false) }
                var targetPage by remember { mutableIntStateOf(0) }
                var contentAlpha by remember { mutableFloatStateOf(1f) }

                // Recharge les données quand on change de page
                LaunchedEffect(pagerState.currentPage) {
                    when (pagerState.currentPage) {
                        1 -> {
                            // Page 1 : ChatScreen - recharge les messages
                            viewModel.loadRecentMessages()
                        }
                        2 -> {
                            // Page 2 : TasksScreen - recharge les tâches et événements
                            viewModel.refreshTasks()
                            viewModel.refreshCalendarEvents()
                        }
                        3 -> {
                            // Page 3 : WeatherScreen - recharge la météo
                            viewModel.refreshWeather()
                        }
                    }
                }

                // Box pour superposer l'animation de transition
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // Fond noir pour que le fade révèle du noir
                ) {
                    // HorizontalPager pour le swipe entre les écrans
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(contentAlpha), // Applique l'alpha au contenu pour le fondu
                        userScrollEnabled = false // Désactive le swipe - navigation uniquement par boutons
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
                                    // Déclenche l'animation de transition vers les tâches
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    // Déclenche l'animation de transition vers la météo
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                    // Déclenche l'animation de transition vers les notes
                                    targetPage = 4
                                    isTransitioning = true
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
                                    // Déclenche l'animation de transition vers l'écran principal
                                    targetPage = 0
                                    isTransitioning = true
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
                                },
                                onNavigateToHome = {
                                    // Déclenche l'animation de transition vers l'écran principal
                                    targetPage = 0
                                    isTransitioning = true
                                }
                            )
                        }

                        // PAGE 3 : Écran Météo
                        3 -> {
                            WeatherScreen(
                                weatherData = weatherData,
                                cityName = cityName,
                                citySearchResults = citySearchResults,
                                isRefreshing = isLoadingWeather,
                                onRefresh = { viewModel.refreshWeather() },
                                onSearchCity = { query -> viewModel.searchCity(query) },
                                onSelectCity = { city -> viewModel.selectCity(city) },
                                onNavigateBack = {
                                    // Déclenche l'animation de transition vers l'écran principal
                                    targetPage = 0
                                    isTransitioning = true
                                }
                            )
                        }

                        // PAGE 4 : Écran Notes
                        4 -> {
                            NotesScreen(
                                notes = notes,
                                onAddNote = { title, content ->
                                    viewModel.addNote(title, content)
                                },
                                onDeleteNote = { noteId ->
                                    viewModel.deleteNote(noteId)
                                },
                                onNavigateBack = {
                                    // Déclenche l'animation de transition vers l'écran principal
                                    targetPage = 0
                                    isTransitioning = true
                                }
                            )
                        }
                    }
                    }

                    // Overlay d'animation de transition futuriste
                    if (isTransitioning) {
                        FuturisticTransition(
                            onAlphaChange = { alpha ->
                                contentAlpha = alpha
                            },
                            onPageChange = {
                                // Appelé quand le cercle est au maximum (milieu de l'animation)
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetPage)
                                }
                            },
                            onTransitionComplete = {
                                // Appelé à la fin complète de l'animation
                                contentAlpha = 1f
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
 * Animation de transition sobre en deux phases
 * Phase 1 : Fade out de l'écran actuel vers le noir (contenu devient transparent)
 * Phase 2 : Fade in du nouvel écran depuis le noir (contenu redevient opaque)
 */
@Composable
fun FuturisticTransition(
    onAlphaChange: (Float) -> Unit,
    onPageChange: () -> Unit,
    onTransitionComplete: () -> Unit
) {
    // États de l'animation
    var animationPhase by remember { mutableStateOf(0) }
    // 0 = fade out (l'écran actuel disparaît)
    // 1 = changement de page
    // 2 = fade in (le nouvel écran apparaît)

    // Alpha cible en fonction de la phase
    val targetAlpha = when (animationPhase) {
        0 -> 0f    // Phase fade out : transparent (le contenu disparaît)
        else -> 1f // Phase fade in : opaque (le contenu apparaît)
    }

    // Durée différente selon la phase (fade out plus lent)
    val animationDuration = when (animationPhase) {
        0 -> 2000 // Fade out très lent (2400ms) pour une disparition progressive
        else -> 1200 // Fade in standard (1200ms)
    }

    // Animation d'alpha pour le contenu avec durée variable
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = EaseInOut // EaseInOut pour une animation douce
        ),
        label = "contentAlpha",
        finishedListener = {
            // Callback appelé quand l'animation est terminée
        }
    )

    // Mise à jour de l'alpha du contenu à chaque frame
    LaunchedEffect(alpha) {
        onAlphaChange(alpha)
    }

    // Gestion de la séquence d'animation
    LaunchedEffect(Unit) {
        // Phase 1 : Fade out (le contenu devient transparent) - 2400ms
        animationPhase = 0
        delay(0) // Attendre la fin du fade out

        // Phase 2 : Changer de page et commencer immédiatement le fade in
        onPageChange() // Change de page
        animationPhase = 1 // Démarre immédiatement le fade in sans délai

        // Phase 3 : Fade in (le contenu redevient opaque) - 1200ms
        delay(1200) // Attendre la fin du fade in

        // Fin de l'animation
        onTransitionComplete()
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
