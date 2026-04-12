package com.max.aiassistant

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.max.aiassistant.ui.chat.ChatScreen
import com.max.aiassistant.ui.common.AppShellRoute
import com.max.aiassistant.ui.common.MaxAppShell
import com.max.aiassistant.ui.home.HomeScreen
import com.max.aiassistant.ui.tasks.OrgaMode
import com.max.aiassistant.ui.tasks.TasksScreen
import com.max.aiassistant.ui.theme.MaxTheme
import com.max.aiassistant.ui.voice.VoiceScreen
import com.max.aiassistant.ui.weather.WeatherScreen
import com.max.aiassistant.ui.weather.RadarScreen
import com.max.aiassistant.ui.actu.ActuScreen
import com.max.aiassistant.ui.tasks.buildTaskNoteContent
import com.max.aiassistant.viewmodel.MainViewModel
import com.max.aiassistant.utils.PermissionHelper

/**
 * Activité principale de l'application Max - AI Assistant
 */
class MainActivity : ComponentActivity() {

    // Gestionnaire de permissions
    private lateinit var permissionHelper: PermissionHelper

    // État pour le texte partagé (accessible depuis onNewIntent)
    private val _sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Texte partagé reçu au démarrage
        _sharedText.value = handleShareIntent(intent)

        // Initialisation permission
        permissionHelper = PermissionHelper(
            activity = this,
            onPermissionGranted = {
                // Permission accordée
            },
            onPermissionDenied = {
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
                val context = LocalContext.current
                val viewModel: MainViewModel = viewModel()
                val sharedText by _sharedText

                // États partagés
                val messages by viewModel.messages.collectAsState()
                val currentConversationId by viewModel.currentConversationId.collectAsState()
                val conversations by viewModel.conversations.collectAsState()
                val currentConversationTitle by viewModel.currentConversationTitle.collectAsState()
                val onDeviceAiSettings by viewModel.onDeviceAiSettings.collectAsState()
                val isWaitingForAiResponse by viewModel.isWaitingForAiResponse.collectAsState()
                val isOnDeviceModelReady by viewModel.isOnDeviceModelReady.collectAsState()
                val isConversationLimitReached by viewModel.isConversationLimitReached.collectAsState()
                val onDeviceModelStatus by viewModel.onDeviceModelStatus.collectAsState()
                val onDeviceModelProvisioningState by viewModel.onDeviceModelProvisioningState.collectAsState()
                val tasks by viewModel.tasks.collectAsState()
                val events by viewModel.events.collectAsState()
                val isLoadingTasks by viewModel.isLoadingTasks.collectAsState()
                val taskError by viewModel.taskError.collectAsState()
                val isLoadingEvents by viewModel.isLoadingEvents.collectAsState()
                val eventsError by viewModel.eventsError.collectAsState()
                val isRealtimeConnected by viewModel.isRealtimeConnected.collectAsState()
                val realtimeError by viewModel.realtimeError.collectAsState()
                val voiceTranscript by viewModel.voiceTranscript.collectAsState()
                val weatherData by viewModel.weatherData.collectAsState()
                val isLoadingWeather by viewModel.isLoadingWeather.collectAsState()
                val weatherError by viewModel.weatherError.collectAsState()
                val notes by viewModel.notes.collectAsState()
                val cityName by viewModel.cityName.collectAsState()
                val citySearchResults by viewModel.citySearchResults.collectAsState()
                val showAllergies by viewModel.showAllergies.collectAsState()
                val cityLatitude by viewModel.cityLatitude.collectAsState()
                val cityLongitude by viewModel.cityLongitude.collectAsState()
                val actuArticles by viewModel.actuArticles.collectAsState()
                val rechercheArticles by viewModel.rechercheArticles.collectAsState()
                val isLoadingActu by viewModel.isLoadingActu.collectAsState()
                val actuError by viewModel.actuError.collectAsState()

                // Etat de navigation principal (shell)
                var currentRoute by rememberSaveable { mutableStateOf(AppShellRoute.HOME) }
                var orgaMode by rememberSaveable { mutableStateOf(OrgaMode.TASK) }

                // Statuts globaux shell
                val isOffline = !isNetworkAvailable(context)
                val isSyncing = isLoadingTasks || isLoadingEvents || isLoadingWeather || isLoadingActu
                val hasLocalData =
                    tasks.isNotEmpty() ||
                    events.isNotEmpty() ||
                    notes.isNotEmpty() ||
                    weatherData != null ||
                    actuArticles.isNotEmpty() ||
                    rechercheArticles.isNotEmpty()

                LaunchedEffect(Unit) {
                    viewModel.refreshTasks()
                    viewModel.refreshCalendarEvents()
                    viewModel.refreshWeather()
                    viewModel.refreshActu()
                }

                // Lorsqu’un texte est partagé, basculer vers le chat
                LaunchedEffect(sharedText) {
                    if (!sharedText.isNullOrBlank()) {
                        currentRoute = AppShellRoute.CHAT
                    }
                }

                // Rafraîchissements à l’arrivée sur certains écrans
                LaunchedEffect(currentRoute) {
                    when (currentRoute) {
                        AppShellRoute.HOME -> {
                            viewModel.refreshTasks()
                            viewModel.refreshCalendarEvents()
                            viewModel.refreshWeather()
                            viewModel.refreshActu()
                        }
                        AppShellRoute.CHAT -> viewModel.loadRecentMessages()
                        AppShellRoute.TASKS -> {
                            viewModel.refreshTasks()
                            viewModel.refreshCalendarEvents()
                        }
                        AppShellRoute.WEATHER -> viewModel.refreshWeather()
                        AppShellRoute.ACTU -> viewModel.refreshActu()
                        else -> Unit
                    }
                }

                if (currentRoute == AppShellRoute.RADAR) {
                    RadarScreen(
                        cityName = cityName,
                        latitude = cityLatitude,
                        longitude = cityLongitude,
                        onNavigateBack = { currentRoute = AppShellRoute.WEATHER }
                    )
                } else {
                    MaxAppShell(
                        currentRoute = currentRoute,
                        drawerSelectionRoute = when {
                            currentRoute == AppShellRoute.TASKS && orgaMode == OrgaMode.PLANNING -> AppShellRoute.PLANNING
                            currentRoute == AppShellRoute.TASKS && orgaMode == OrgaMode.NOTE -> AppShellRoute.NOTES
                            else -> currentRoute
                        },
                        isOffline = isOffline,
                        isSyncing = isSyncing,
                        hasLocalData = hasLocalData,
                        onNavigate = { route ->
                            when (route) {
                                AppShellRoute.HOME -> currentRoute = AppShellRoute.HOME
                                AppShellRoute.CHAT -> currentRoute = AppShellRoute.CHAT
                                AppShellRoute.TASKS -> {
                                    orgaMode = OrgaMode.TASK
                                    currentRoute = AppShellRoute.TASKS
                                }
                                AppShellRoute.NOTES -> {
                                    orgaMode = OrgaMode.NOTE
                                    currentRoute = AppShellRoute.TASKS
                                }
                                AppShellRoute.WEATHER -> currentRoute = AppShellRoute.WEATHER
                                AppShellRoute.VOICE -> currentRoute = AppShellRoute.VOICE
                                AppShellRoute.PLANNING -> {
                                    orgaMode = OrgaMode.PLANNING
                                    currentRoute = AppShellRoute.TASKS
                                }
                                AppShellRoute.ACTU -> currentRoute = AppShellRoute.ACTU
                                AppShellRoute.RADAR -> currentRoute = AppShellRoute.RADAR
                            }
                        }
                    ) { paddingValues, openMainSidebar ->
                        val modifier = Modifier.padding(paddingValues)

                        when (currentRoute) {
                            AppShellRoute.HOME -> {
                                HomeScreen(
                                    tasks = tasks,
                                    events = events,
                                    weatherData = weatherData,
                                    headlineArticle = actuArticles.firstOrNull(),
                                    cityName = cityName,
                                    onNavigateToVoice = { currentRoute = AppShellRoute.VOICE },
                                    onNavigateToChat = { currentRoute = AppShellRoute.CHAT },
                                    onNavigateToTasks = { orgaMode = OrgaMode.TASK; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToPlanning = { orgaMode = OrgaMode.PLANNING; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToWeather = { currentRoute = AppShellRoute.WEATHER },
                                    onNavigateToRadar = { currentRoute = AppShellRoute.RADAR },
                                    onNavigateToNotes = { orgaMode = OrgaMode.NOTE; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToActu = { currentRoute = AppShellRoute.ACTU },
                                    showChrome = false,
                                    modifier = modifier
                                )
                            }
                            AppShellRoute.CHAT -> {
                                ChatScreen(
                                    messages = messages,
                                    conversations = conversations,
                                    currentConversationId = currentConversationId,
                                    conversationTitle = currentConversationTitle,
                                    onDeviceAiSettings = onDeviceAiSettings,
                                    isWaitingForAiResponse = isWaitingForAiResponse,
                                    isOnDeviceModelReady = isOnDeviceModelReady,
                                    isConversationLimitReached = isConversationLimitReached,
                                    onDeviceModelStatus = onDeviceModelStatus,
                                    onDeviceModelProvisioningState = onDeviceModelProvisioningState,
                                    onSendMessage = { message, imageUri ->
                                        viewModel.sendMessage(message, imageUri)
                                    },
                                    onStartNewConversation = {
                                        viewModel.startNewConversation()
                                    },
                                    onSelectConversation = { conversationId ->
                                        viewModel.selectConversation(conversationId)
                                    },
                                    onRenameConversation = { conversationId, title ->
                                        viewModel.renameConversation(conversationId, title)
                                    },
                                    onDeleteConversation = { conversationId ->
                                        viewModel.deleteConversation(conversationId)
                                    },
                                    onUpdateOnDeviceAiSettings = { modelVariant, maxContextTokens, systemPrompt ->
                                        viewModel.updateOnDeviceAiSettings(
                                            modelVariant = modelVariant,
                                            maxContextTokens = maxContextTokens,
                                            systemPrompt = systemPrompt
                                        )
                                    },
                                    onOpenMainSidebar = openMainSidebar,
                                    onRetryModelDownload = {
                                        viewModel.retryOnDeviceModelDownload()
                                    },
                                    onVoiceInput = {
                                        currentRoute = AppShellRoute.VOICE
                                    },
                                    onNavigateToHome = { currentRoute = AppShellRoute.HOME },
                                    onNavigateToTasks = { orgaMode = OrgaMode.TASK; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToPlanning = { orgaMode = OrgaMode.PLANNING; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToWeather = { currentRoute = AppShellRoute.WEATHER },
                                    onNavigateToNotes = { orgaMode = OrgaMode.NOTE; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToActu = { currentRoute = AppShellRoute.ACTU },
                                    showChrome = false,
                                    initialText = sharedText.orEmpty(),
                                    onInitialTextConsumed = {
                                        _sharedText.value = null
                                    }
                                )
                            }
                            AppShellRoute.TASKS -> {
                                TasksScreen(
                                    tasks = tasks,
                                    events = events,
                                    notes = notes,
                                    isRefreshing = isLoadingTasks,
                                    isEventsRefreshing = isLoadingEvents,
                                    errorMessage = taskError,
                                    eventsErrorMessage = eventsError,
                                    isOffline = isOffline,
                                    onRefresh = { viewModel.refreshTasks() },
                                    onRefreshPlanning = { viewModel.refreshCalendarEvents() },
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
                                    onAddNote = { title, content ->
                                        viewModel.addNote(title, content)
                                    },
                                    onUpdateNote = { noteId, title, content ->
                                        viewModel.updateNote(noteId, title, content)
                                    },
                                    onDeleteNote = { noteId ->
                                        viewModel.deleteNote(noteId)
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
                                    onTaskScheduleRequested = {
                                        orgaMode = OrgaMode.PLANNING
                                    },
                                    onTaskCreateNoteRequested = { task ->
                                        viewModel.addNote(task.title, buildTaskNoteContent(task))
                                        orgaMode = OrgaMode.NOTE
                                    },
                                    initialMode = orgaMode,
                                    onModeChange = { orgaMode = it },
                                    onNavigateToHome = { currentRoute = AppShellRoute.HOME },
                                    onNavigateToChat = { currentRoute = AppShellRoute.CHAT },
                                    onNavigateToPlanning = { orgaMode = OrgaMode.PLANNING },
                                    onNavigateToWeather = { currentRoute = AppShellRoute.WEATHER },
                                    onNavigateToNotes = { orgaMode = OrgaMode.NOTE },
                                    onNavigateToActu = { currentRoute = AppShellRoute.ACTU },
                                    showChrome = false
                                )
                            }
                            AppShellRoute.WEATHER -> {
                                WeatherScreen(
                                    weatherData = weatherData,
                                    cityName = cityName,
                                    citySearchResults = citySearchResults,
                                    showAllergies = showAllergies,
                                    isRefreshing = isLoadingWeather,
                                    errorMessage = weatherError,
                                    isOffline = isOffline,
                                    onRefresh = { viewModel.refreshWeather(force = true) },
                                    onSearchCity = { query -> viewModel.searchCity(query) },
                                    onSelectCity = { city -> viewModel.selectCity(city) },
                                    onSetShowAllergies = { show -> viewModel.setShowAllergies(show) },
                                    onNavigateBack = { currentRoute = AppShellRoute.HOME },
                                    onRadarClick = { currentRoute = AppShellRoute.RADAR },
                                    onNavigateToChat = { currentRoute = AppShellRoute.CHAT },
                                    onNavigateToTasks = { orgaMode = OrgaMode.TASK; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToPlanning = { orgaMode = OrgaMode.PLANNING; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToNotes = { orgaMode = OrgaMode.NOTE; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToActu = { currentRoute = AppShellRoute.ACTU },
                                    showChrome = false
                                )
                            }
                            AppShellRoute.VOICE -> {
                                VoiceScreen(
                                    isRealtimeConnected = isRealtimeConnected,
                                    errorMessage = realtimeError,
                                    isOffline = isOffline,
                                    transcript = voiceTranscript,
                                    onToggleRealtime = {
                                        if (permissionHelper.hasRecordAudioPermission()) {
                                            viewModel.toggleRealtimeConnection()
                                        } else {
                                            permissionHelper.requestRecordAudioPermission()
                                        }
                                    },
                                    onNavigateToHome = { currentRoute = AppShellRoute.HOME },
                                    onNavigateToChat = { currentRoute = AppShellRoute.CHAT },
                                    onNavigateToTasks = { orgaMode = OrgaMode.TASK; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToPlanning = { orgaMode = OrgaMode.PLANNING; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToWeather = { currentRoute = AppShellRoute.WEATHER },
                                    onNavigateToNotes = { orgaMode = OrgaMode.NOTE; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToActu = { currentRoute = AppShellRoute.ACTU },
                                    showChrome = false
                                )
                            }
                            AppShellRoute.ACTU -> {
                                ActuScreen(
                                    actuArticles = actuArticles,
                                    rechercheArticles = rechercheArticles,
                                    isRefreshing = isLoadingActu,
                                    errorMessage = actuError,
                                    isOffline = isOffline,
                                    onRefresh = { viewModel.refreshActu() },
                                    onNavigateToHome = { currentRoute = AppShellRoute.HOME },
                                    onNavigateToVoice = { currentRoute = AppShellRoute.VOICE },
                                    onNavigateToChat = { currentRoute = AppShellRoute.CHAT },
                                    onNavigateToTasks = { orgaMode = OrgaMode.TASK; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToPlanning = { orgaMode = OrgaMode.PLANNING; currentRoute = AppShellRoute.TASKS },
                                    onNavigateToWeather = { currentRoute = AppShellRoute.WEATHER },
                                    onNavigateToNotes = { orgaMode = OrgaMode.NOTE; currentRoute = AppShellRoute.TASKS },
                                    showChrome = false
                                )
                            }
                            AppShellRoute.NOTES -> Unit
                            AppShellRoute.PLANNING -> Unit
                            AppShellRoute.RADAR -> Unit
                        }
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

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        connectivityManager.activeNetworkInfo?.isConnected == true
    }
}
