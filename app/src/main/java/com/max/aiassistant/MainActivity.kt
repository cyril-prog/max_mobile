package com.max.aiassistant

import android.content.Intent
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
import com.max.aiassistant.ui.home.HomeScreen
import com.max.aiassistant.ui.notes.NotesScreen
import com.max.aiassistant.ui.planning.PlanningScreen
import com.max.aiassistant.ui.tasks.TasksScreen
import com.max.aiassistant.ui.theme.MaxTheme
import com.max.aiassistant.ui.voice.VoiceScreen
import com.max.aiassistant.ui.voice.FluidOrbVisualizer
import com.max.aiassistant.ui.weather.WeatherScreen
import com.max.aiassistant.ui.weather.RadarScreen
import com.max.aiassistant.ui.actu.ActuScreen
import com.max.aiassistant.viewmodel.MainViewModel
import kotlin.math.sqrt

/**
 * Activité principale de l'application Max - AI Assistant
 *
 * Single-Activity architecture avec Jetpack Compose
 *
 * Gère la navigation entre 9 écrans via HorizontalPager :
 * - Page 0 : HomeScreen (Dashboard) - ÉCRAN PAR DÉFAUT
 * - Page 1 : VoiceScreen (Voice to Voice)
 * - Page 2 : ChatScreen (Messenger)
 * - Page 3 : TasksScreen (Tâches)
 * - Page 4 : PlanningScreen (Planning/Agenda)
 * - Page 5 : WeatherScreen (Météo)
 * - Page 6 : NotesScreen (Prise de notes)
 * - Page 7 : RadarScreen (Radar météo)
 * - Page 8 : ActuScreen (Actualités)
 *
 * L'utilisateur peut naviguer entre les pages via les boutons de navigation
 */
class MainActivity : ComponentActivity() {

    // Gestionnaire de permissions
    private lateinit var permissionHelper: PermissionHelper
    
    // État pour le texte partagé (accessible depuis onNewIntent)
    private val _sharedText = mutableStateOf<String?>(null)

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Gère le texte partagé depuis une autre application
        _sharedText.value = handleShareIntent(intent)

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
                
                // État pour le texte partagé depuis une autre application
                val sharedText by _sharedText

                // Collecte de l'état réactif depuis le ViewModel
                val messages by viewModel.messages.collectAsState()
                val isWaitingForAiResponse by viewModel.isWaitingForAiResponse.collectAsState()
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
                val showAllergies by viewModel.showAllergies.collectAsState()
                val cityLatitude by viewModel.cityLatitude.collectAsState()
                val cityLongitude by viewModel.cityLongitude.collectAsState()
                val actuArticles by viewModel.actuArticles.collectAsState()
                val rechercheArticles by viewModel.rechercheArticles.collectAsState()
                val isLoadingActu by viewModel.isLoadingActu.collectAsState()

                // État du pager (9 pages)
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { 9 }
                )

                // Scope pour les animations de navigation
                val coroutineScope = rememberCoroutineScope()
                
                // Si du texte a été partagé, naviguer vers le chat
                LaunchedEffect(sharedText) {
                    if (!sharedText.isNullOrEmpty()) {
                        pagerState.scrollToPage(2) // Naviguer vers ChatScreen
                    }
                }

                // Charge les données initiales au démarrage (page 0 = HomeScreen)
                LaunchedEffect(Unit) {
                    viewModel.refreshTasks()
                    viewModel.refreshCalendarEvents()
                    viewModel.refreshWeather()
                }

                // État pour l'animation de transition futuriste
                var isTransitioning by remember { mutableStateOf(false) }
                var targetPage by remember { mutableIntStateOf(0) }
                var contentAlpha by remember { mutableFloatStateOf(1f) }

                // Recharge les données quand on change de page
                LaunchedEffect(pagerState.currentPage) {
                    when (pagerState.currentPage) {
                        0 -> {
                            // Page 0 : HomeScreen - recharge les tâches, événements et météo
                            viewModel.refreshTasks()
                            viewModel.refreshCalendarEvents()
                            viewModel.refreshWeather()
                        }
                        2 -> {
                            // Page 2 : ChatScreen - recharge les messages
                            viewModel.loadRecentMessages()
                        }
                        3 -> {
                            // Page 3 : TasksScreen - recharge les tâches et événements
                            viewModel.refreshTasks()
                            viewModel.refreshCalendarEvents()
                        }
                        5 -> {
                            // Page 5 : WeatherScreen - recharge la météo
                            viewModel.refreshWeather()
                        }
                        8 -> {
                            // Page 8 : ActuScreen - recharge les actualités
                            viewModel.refreshActu()
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
                        // PAGE 0 : Dashboard (écran d'accueil)
                        0 -> {
                            HomeScreen(
                                tasks = tasks,
                                events = events,
                                weatherData = weatherData,
                                cityName = cityName,
                                onNavigateToVoice = {
                                    targetPage = 1
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    targetPage = 5
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                    targetPage = 6
                                    isTransitioning = true
                                },
                                onNavigateToActu = {
                                    targetPage = 8
                                    isTransitioning = true
                                }
                            )
                        }

                        // PAGE 1 : Écran Voice
                        1 -> {
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
                                onNavigateToHome = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    targetPage = 5
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                     targetPage = 6
                                     isTransitioning = true
                                 },
                                 onNavigateToActu = {
                                     targetPage = 8
                                     isTransitioning = true
                                 }
                             )
                         }

                         // PAGE 2 : Écran Chat
                        2 -> {
                            ChatScreen(
                                messages = messages,
                                isWaitingForAiResponse = isWaitingForAiResponse,
                                onSendMessage = { message, imageUri ->
                                    viewModel.sendMessage(message, imageUri)
                                },
                                onVoiceInput = {
                                    targetPage = 1
                                    isTransitioning = true
                                },
                                onNavigateToHome = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    targetPage = 5
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                     targetPage = 6
                                     isTransitioning = true
                                 },
                                 onNavigateToActu = {
                                     targetPage = 8
                                     isTransitioning = true
                                 },
                                 initialText = sharedText ?: "",
                                onInitialTextConsumed = { _sharedText.value = null }
                            )
                        }

                        // PAGE 3 : Écran Tâches
                        3 -> {
                            TasksScreen(
                                tasks = tasks,
                                isRefreshing = isLoadingTasks,
                                onRefresh = { viewModel.refreshTasks() },
                                onTaskStatusChange = { taskId, newStatus ->
                                    viewModel.updateTaskStatus(taskId, newStatus)
                                },
                                onTaskPriorityChange = { taskId, newPriority ->
                                    viewModel.updateTaskPriority(taskId, newPriority)
                                },
                                onTaskDurationChange = { taskId, newDuration ->
                                    viewModel.updateTaskDuration(taskId, newDuration)
                                },
                                onTaskDeadlineChange = { taskId, newDeadlineDate ->
                                    viewModel.updateTaskDeadline(taskId, newDeadlineDate)
                                },
                                onTaskCategoryChange = { taskId, newCategory ->
                                    viewModel.updateTaskCategory(taskId, newCategory)
                                },
                                onTaskTitleChange = { taskId, newTitle ->
                                    viewModel.updateTaskTitle(taskId, newTitle)
                                },
                                onTaskDescriptionChange = { taskId, newDescription ->
                                    viewModel.updateTaskDescription(taskId, newDescription)
                                },
                                onTaskNoteChange = { taskId, newNote ->
                                    viewModel.updateTaskNote(taskId, newNote)
                                },
                                onTaskDelete = { taskId ->
                                    viewModel.deleteTask(taskId)
                                },
                                onTaskCreate = { titre, categorie, description, priorite, dateLimite, dureeEstimee ->
                                    viewModel.createTask(
                                        titre = titre,
                                        categorie = categorie,
                                        description = description,
                                        priorite = priorite,
                                        dateLimite = dateLimite,
                                        dureeEstimee = dureeEstimee
                                    )
                                },
                                onSubTaskCreate = { taskId, text ->
                                    viewModel.createSubTask(taskId, text)
                                },
                                onSubTaskUpdate = { subTaskId, text, isCompleted ->
                                    viewModel.updateSubTask(subTaskId, text, isCompleted)
                                },
                                onSubTaskDelete = { subTaskId ->
                                    viewModel.deleteSubTask(subTaskId)
                                },
                                onNavigateToHome = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    targetPage = 5
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                     targetPage = 6
                                     isTransitioning = true
                                 },
                                 onNavigateToActu = {
                                     targetPage = 8
                                     isTransitioning = true
                                 }
                             )
                         }

                         // PAGE 4 : Écran Planning
                        4 -> {
                            PlanningScreen(
                                events = events,
                                isRefreshing = isLoadingEvents,
                                onRefresh = { viewModel.refreshCalendarEvents() },
                                onNavigateToHome = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    targetPage = 5
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                     targetPage = 6
                                     isTransitioning = true
                                 },
                                 onNavigateToActu = {
                                     targetPage = 8
                                     isTransitioning = true
                                 }
                             )
                         }

                         // PAGE 5 : Écran Météo
                        5 -> {
                            WeatherScreen(
                                weatherData = weatherData,
                                cityName = cityName,
                                citySearchResults = citySearchResults,
                                showAllergies = showAllergies,
                                isRefreshing = isLoadingWeather,
                                onRefresh = { viewModel.refreshWeather() },
                                onSearchCity = { query -> viewModel.searchCity(query) },
                                onSelectCity = { city -> viewModel.selectCity(city) },
                                onSetShowAllergies = { show -> viewModel.setShowAllergies(show) },
                                onNavigateBack = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onRadarClick = {
                                    targetPage = 7
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                     targetPage = 6
                                     isTransitioning = true
                                 },
                                 onNavigateToActu = {
                                     targetPage = 8
                                     isTransitioning = true
                                 }
                             )
                         }

                         // PAGE 6 : Écran Notes
                        6 -> {
                            NotesScreen(
                                notes = notes,
                                onAddNote = { title, content ->
                                    viewModel.addNote(title, content)
                                },
                                onUpdateNote = { noteId, title, content ->
                                    viewModel.updateNote(noteId, title, content)
                                },
                                onDeleteNote = { noteId ->
                                    viewModel.deleteNote(noteId)
                                },
                                onNavigateBack = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                     targetPage = 5
                                     isTransitioning = true
                                 },
                                 onNavigateToActu = {
                                     targetPage = 8
                                     isTransitioning = true
                                 }
                             )
                         }

                         // PAGE 7 : Écran Radar Météo
                        7 -> {
                            RadarScreen(
                                cityName = cityName,
                                latitude = cityLatitude,
                                longitude = cityLongitude,
                                onNavigateBack = {
                                    targetPage = 5
                                    isTransitioning = true
                                }
                            )
                        }

                        // PAGE 8 : Écran Actualités
                        8 -> {
                            ActuScreen(
                                actuArticles = actuArticles,
                                rechercheArticles = rechercheArticles,
                                isRefreshing = isLoadingActu,
                                onRefresh = { viewModel.refreshActu() },
                                onNavigateToHome = {
                                    targetPage = 0
                                    isTransitioning = true
                                },
                                onNavigateToVoice = {
                                    targetPage = 1
                                    isTransitioning = true
                                },
                                onNavigateToChat = {
                                    targetPage = 2
                                    isTransitioning = true
                                },
                                onNavigateToTasks = {
                                    targetPage = 3
                                    isTransitioning = true
                                },
                                onNavigateToPlanning = {
                                    targetPage = 4
                                    isTransitioning = true
                                },
                                onNavigateToWeather = {
                                    targetPage = 5
                                    isTransitioning = true
                                },
                                onNavigateToNotes = {
                                    targetPage = 6
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
    
    /**
     * Appelé quand l'activité reçoit un nouvel intent (app déjà ouverte)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("ShareIntent", "onNewIntent called")
        val newSharedText = handleShareIntent(intent)
        if (newSharedText != null) {
            _sharedText.value = newSharedText
        }
    }
    
    /**
     * Extrait le texte partagé depuis un Intent ACTION_SEND
     */
    private fun handleShareIntent(intent: Intent?): String? {
        android.util.Log.d("ShareIntent", "Action: ${intent?.action}, Type: ${intent?.type}")
        
        if (intent?.action == Intent.ACTION_SEND) {
            val type = intent.type ?: ""
            if (type.startsWith("text/")) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                android.util.Log.d("ShareIntent", "Shared text: $sharedText")
                return sharedText
            }
        }
        return null
    }
}

/**
 * Animation de transition sobre en deux phases
 * Phase 1 : Fade out de l'écran actuel vers le noir (300ms)
 * Phase 2 : Fade in du nouvel écran depuis le noir (400ms)
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

    // Durée différente selon la phase (fade out rapide)
    val animationDuration = when (animationPhase) {
        0 -> 300 // Fade out rapide (300ms)
        else -> 400 // Fade in standard (400ms)
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

        // Phase 3 : Fade in (le contenu redevient opaque) - 400ms
        delay(400) // Attendre la fin du fade in

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
