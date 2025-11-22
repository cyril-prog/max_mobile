package com.max.aiassistant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.max.aiassistant.model.TaskStatus
import com.max.aiassistant.ui.chat.ChatScreen
import com.max.aiassistant.ui.tasks.TasksScreen
import com.max.aiassistant.ui.theme.MaxTheme
import com.max.aiassistant.ui.voice.VoiceScreen
import com.max.aiassistant.viewmodel.MainViewModel

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
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val isListening by viewModel.isListening.collectAsState()
                val voiceTranscript by viewModel.voiceTranscript.collectAsState()

                // État du pager (3 pages, commence à la page 0 = Voice)
                val pagerState = rememberPagerState(
                    initialPage = 0, // Démarre sur VoiceScreen (écran principal)
                    pageCount = { 3 }
                )

                // Scope pour les animations de navigation
                val coroutineScope = rememberCoroutineScope()

                // HorizontalPager pour le swipe entre les écrans
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        // PAGE 0 : Écran Voice (écran principal)
                        0 -> {
                            VoiceScreen(
                                isListening = isListening,
                                transcript = voiceTranscript,
                                onToggleListening = {
                                    viewModel.toggleListening()
                                },
                                onNavigateToChat = {
                                    // Navigation fluide vers ChatScreen (page 1)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
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
