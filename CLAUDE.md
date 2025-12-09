# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Max - AI Assistant** is a modern Android mobile application built with Kotlin and Jetpack Compose. It's an AI assistant with multiple interaction modes: voice-to-voice (OpenAI Realtime API), text chat with vision, task/calendar management, weather forecasts, and note-taking.

- **Project Name**: Max-AI-Assistant
- **Package**: com.max.aiassistant
- **Architecture**: Single-Activity with MVVM pattern
- **UI Framework**: 100% Jetpack Compose (no XML layouts)
- **Language**: Kotlin with StateFlow for reactive state management
- **Persistence**: DataStore for preferences and notes (no Room database)

## Build Commands

### Windows (using gradlew.bat)
```bash
powershell -Command "& '.\gradlew.bat' clean assembleDebug"
powershell -Command "& '.\gradlew.bat' assembleDebug && '.\gradlew.bat' installDebug"
```

### Linux/Mac (using gradlew)
```bash
./gradlew clean build
./gradlew assembleDebug
./gradlew installDebug
```

### Specific Build Tasks
```bash
./gradlew :app:assembleDebug      # Build debug APK
./gradlew :app:assembleRelease    # Build release APK
./gradlew clean                   # Clean build artifacts
```

### Running Tests
Tests are not yet implemented, but the structure is in place for:
```bash
./gradlew test                     # Unit tests
./gradlew connectedAndroidTest     # Instrumented tests
```

### Important Build Notes
- **JDK 17 required** - Ensure `JAVA_HOME` points to JDK 17
- **OpenAI API Key** - Create `local.properties` with `OPENAI_API_KEY=sk-proj-...` before building
- **Windows users** - Use PowerShell commands shown above to avoid path issues with gradlew.bat

## Key Architecture Details

### Single-Activity Navigation
The app uses a **HorizontalPager** for navigation instead of traditional fragments or navigation components. **Seven screens** are accessible via button navigation (swipe disabled):
- **Page 0: VoiceScreen (default start)** - Voice-to-voice with OpenAI Realtime API and audio visualization
- **Page 1: ChatScreen** - AI chat with vision (camera + gallery image support)
- **Page 2: TasksScreen** - Task management with subtasks and priority levels
- **Page 3: PlanningScreen** - Calendar events display
- **Page 4: WeatherScreen** - Weather forecasts with Open-Meteo API
- **Page 5: NotesScreen** - Note-taking with DataStore persistence
- **Page 6: RadarScreen** - Weather radar visualization

Navigation is handled via sidebar buttons with futuristic fade transitions. The pager is configured in [MainActivity.kt:116-120](app/src/main/java/com/max/aiassistant/MainActivity.kt#L116-L120) with `initialPage = 0` and `userScrollEnabled = false`.

### State Management
All application state lives in [MainViewModel.kt](app/src/main/java/com/max/aiassistant/viewmodel/MainViewModel.kt) and is exposed via StateFlow:
- `messages: StateFlow<List<Message>>` - Chat messages loaded from API
- `tasks: StateFlow<List<Task>>` - Task list with subtasks fetched from API
- `events: StateFlow<List<Event>>` - Calendar events from API
- `isRealtimeConnected: StateFlow<Boolean>` - OpenAI Realtime WebSocket connection state
- `voiceTranscript: StateFlow<String>` - Real voice transcription from OpenAI Realtime
- `weatherData: StateFlow<WeatherData?>` - Weather data from Open-Meteo API
- `notes: StateFlow<List<Note>>` - Notes persisted in DataStore
- `cityName: StateFlow<String>` - Current weather city
- `isLoadingTasks/Events/Weather: StateFlow<Boolean>` - Loading states

Each screen observes its required state using `collectAsState()` in the composable.

### API Integration Architecture

The app integrates with **three external APIs**:

#### 1. N8N Backend API (Retrofit)
**Service**: [MaxApiService.kt](app/src/main/java/com/max/aiassistant/data/api/MaxApiService.kt)
- Base URL: `https://n8n.srv1086212.hstgr.cloud/`
- Uses OkHttp with logging interceptor for debugging
- 30-second timeouts for all requests
- Gson converter for JSON serialization

**Endpoints**:
- `POST /webhook/max_mobile` - Send chat message with optional Base64 image (vision support)
- `GET /webhook/get_recent_messages` - Load recent chat history
- `GET /webhook/get_tasks` - Fetch task list with subtasks
- `POST /webhook/create_task` - Create new task
- `POST /webhook/upd_task` - Update task fields (title, description, priority, etc.)
- `DELETE /webhook/del_task?id={taskId}` - Delete task
- `POST /webhook/create_subtask` - Create subtask for a task
- `POST /webhook/upd_subtask` - Update subtask (text, completion status)
- `DELETE /webhook/del_subtask` - Delete subtask
- `GET /webhook/get_calendar` - Fetch calendar events
- `GET /webhook/get_memory` - Load user context/memory for voice prompts
- `POST /webhook/save_conv` - Save voice conversation messages

#### 2. OpenAI Realtime API (WebSocket)
**Service**: [RealtimeApiService.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeApiService.kt)
- WebSocket URL: `wss://api.openai.com/v1/realtime?model=gpt-realtime`
- Real-time bidirectional audio streaming
- Automatic audio recording via [RealtimeAudioManager.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeAudioManager.kt)
- Enriched with system context (tasks, memory, calendar) from N8N API
- **Requires**: `OPENAI_API_KEY` in `local.properties` file

#### 3. Open-Meteo Weather API (Retrofit)
**Service**: [WeatherApiService.kt](app/src/main/java/com/max/aiassistant/data/api/WeatherApiService.kt)
- Base URL: `https://api.open-meteo.com/`
- No API key required (free tier)
- Returns hourly weather forecasts, pollen data, and current conditions
- Geocoding via [GeocodingApiService.kt](app/src/main/java/com/max/aiassistant/data/api/GeocodingApiService.kt)

**API Models**: [ApiModels.kt](app/src/main/java/com/max/aiassistant/data/api/ApiModels.kt), [MessagesApiModels.kt](app/src/main/java/com/max/aiassistant/data/api/MessagesApiModels.kt), [WeatherApiModels.kt](app/src/main/java/com/max/aiassistant/data/api/WeatherApiModels.kt), [SystemContextModels.kt](app/src/main/java/com/max/aiassistant/data/api/SystemContextModels.kt)
- Response models with extension functions (e.g., `toTask()`, `toEvent()`, `toWeatherData()`) to convert API models to domain models

**ViewModel Integration**: All API calls run in `viewModelScope` with proper error handling and loading states.

### Data Flow
1. **App Launch**: `MainViewModel.init` automatically loads:
   - Recent chat messages from N8N
   - Tasks with subtasks from N8N
   - Calendar events from N8N
   - Weather data from Open-Meteo
   - System context (memory, tasks, calendar) for voice enrichment
   - Persisted notes from DataStore
2. **Chat**: User sends message (text + optional image) → `sendMessage()` → N8N API → AI response added to messages
3. **Voice**: Toggle button → WebSocket connects to OpenAI Realtime → Audio recording starts → Real-time transcription & AI audio response
4. **Tasks**: Create/update/delete operations → Optimistic UI updates → API sync → Refresh on success
5. **Weather**: City search → Geocoding API → Select city → Save to DataStore → Refresh weather from Open-Meteo
6. **Notes**: Add/update/delete → Immediate StateFlow update → Save to DataStore for persistence

## Implementation Status & TODOs

### ✅ Fully Implemented Features

1. **OpenAI Realtime API Voice-to-Voice** ([RealtimeApiService.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeApiService.kt), [RealtimeAudioManager.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeAudioManager.kt))
   - WebSocket connection to OpenAI Realtime API
   - Real-time audio recording (16kHz PCM) and Base64 streaming
   - Real-time transcription (both user and AI)
   - Audio playback of AI responses
   - System context enrichment (tasks, memory, calendar)
   - Conversation persistence to N8N
   - Runtime RECORD_AUDIO permission handling ([PermissionHelper.kt](app/src/main/java/com/max/aiassistant/utils/PermissionHelper.kt))

2. **Chat with Vision** ([ChatScreen.kt](app/src/main/java/com/max/aiassistant/ui/chat/ChatScreen.kt))
   - Text messaging with N8N backend
   - Image attachment from camera or gallery
   - Base64 image encoding for vision API
   - Runtime CAMERA permission handling
   - Text sharing from other apps (Intent filter in AndroidManifest)

3. **Weather Integration** ([WeatherScreen.kt](app/src/main/java/com/max/aiassistant/ui/weather/WeatherScreen.kt), [RadarScreen.kt](app/src/main/java/com/max/aiassistant/ui/weather/RadarScreen.kt))
   - Open-Meteo API for hourly forecasts
   - City search with geocoding
   - DataStore persistence of selected city
   - Pollen/allergy data
   - Weather radar iframe integration

4. **Task Management with Subtasks** ([TasksScreen.kt](app/src/main/java/com/max/aiassistant/ui/tasks/TasksScreen.kt))
   - CRUD operations for tasks (create, update priority/deadline/category/duration, delete)
   - Subtask creation, editing, completion toggling, deletion
   - Optimistic UI updates with API sync
   - Pull-to-refresh

5. **Notes with Local Persistence** ([NotesScreen.kt](app/src/main/java/com/max/aiassistant/ui/notes/NotesScreen.kt), [NotesPreferences.kt](app/src/main/java/com/max/aiassistant/data/preferences/NotesPreferences.kt))
   - DataStore-backed note storage
   - Create, edit, delete notes
   - Persists across app restarts

### 🔜 Remaining TODOs

1. **Google Calendar Integration**
   - **Current**: Events fetched from N8N API (custom backend)
   - **To implement**: Read device calendar using CalendarContract
   - **File**: [MainViewModel.kt:932-956](app/src/main/java/com/max/aiassistant/viewmodel/MainViewModel.kt#L932-L956) - `refreshCalendarEvents()`
   - **Required**: Uncomment `READ_CALENDAR` permission in [AndroidManifest.xml:27](app/src/main/AndroidManifest.xml#L27)

2. **Local API Key Security**
   - **Current**: OpenAI API key stored in `local.properties` and exposed via BuildConfig
   - **To improve**: Move API key to backend proxy to avoid client-side exposure
   - **File**: [build.gradle.kts:43-44](app/build.gradle.kts#L43-L44)

## Theme and Design System

Located in `ui/theme/`:
- **Color.kt**: Dark mode palette (default theme)
  - Primary: #0A84FF (blue accent)
  - Background: #000000
  - Surface: #1C1C1E
- **Type.kt**: Typography definitions using default font family
- **Theme.kt**: Material 3 configuration with edge-to-edge enabled

## Important Caveats

### Permissions
All required permissions are now **enabled** in [AndroidManifest.xml](app/src/main/AndroidManifest.xml):
- ✅ **INTERNET** - for API calls (N8N, Open-Meteo, OpenAI)
- ✅ **RECORD_AUDIO** - for OpenAI Realtime voice recording (runtime permission handled by [PermissionHelper.kt](app/src/main/java/com/max/aiassistant/utils/PermissionHelper.kt))
- ✅ **CAMERA** - for chat image capture (runtime permission)
- ✅ **READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE** - for gallery image selection
- ⏸️ **READ_CALENDAR** (commented) - uncomment when implementing local calendar sync

### Partial Persistence Layer
- **DataStore** implemented for:
  - Weather city preferences ([WeatherPreferences.kt](app/src/main/java/com/max/aiassistant/data/preferences/WeatherPreferences.kt))
  - Notes ([NotesPreferences.kt](app/src/main/java/com/max/aiassistant/data/preferences/NotesPreferences.kt))
- **No Room database** - Messages, tasks, and events are NOT persisted locally:
  - All data reloaded from N8N API on each app restart
  - ViewModel does not survive process death (Android System limitation)

### Gradle Configuration
- **Kotlin version**: 2.1.0
- **Compose compiler**: 2.1.0 (plugin-based with `org.jetbrains.kotlin.plugin.compose`)
- **compileSdk**: 35, **minSdk**: 26, **targetSdk**: 35
- **JDK**: 17 required
- **Android Gradle Plugin**: 8.9.0

### Dependencies Already Installed
The following dependencies are already in [app/build.gradle.kts](app/build.gradle.kts):
- **Retrofit 2.9.0** - HTTP client for N8N and weather APIs
- **Gson converter** - JSON serialization/deserialization
- **OkHttp 4.12.0** - WebSocket support for OpenAI Realtime + logging interceptor
- **Jetpack Compose BOM 2024.09.00** - UI framework
- **Material 3 1.2.0** - Material Design components
- **Material Icons Extended** - Icon library
- **Accompanist SwipeRefresh 0.32.0** - Pull-to-refresh
- **DataStore Preferences 1.1.1** - Local persistence for settings and notes
- **Coil 2.5.0** - Image loading for chat attachments
- **Navigation Compose 2.7.7** - Optional navigation (not currently used)

## Code Style Notes

- **All code and comments are in French** to match the original implementation
- Each file contains detailed inline comments explaining the code
- Search for "TODO:" in code to find integration points for remaining implementations
- **No XML layouts** - 100% Jetpack Compose declarative UI
- ViewModel uses optimistic UI updates for better UX (tasks, subtasks, notes)
- StateFlow for reactive state management, collected as State in Composables

## Key Features & Architectural Patterns

### 1. Real-Time Voice with WebSocket
The voice assistant uses OpenAI's Realtime API via WebSocket for bidirectional audio streaming:
- **Audio Pipeline**: AndroidAudioRecord (16kHz PCM) → Base64 encoding → WebSocket → OpenAI → Audio response → AndroidAudioTrack playback
- **Context Enrichment**: System context (tasks, calendar, user memory) loaded from N8N and injected into session instructions
- **Conversation Persistence**: Each voice exchange (user + AI) is automatically saved to N8N backend via `POST /webhook/save_conv`
- **Key Files**: [RealtimeApiService.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeApiService.kt), [RealtimeAudioManager.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeAudioManager.kt), [RealtimeModels.kt](app/src/main/java/com/max/aiassistant/data/realtime/RealtimeModels.kt)

### 2. Vision-Enabled Chat
Chat supports multimodal input with camera or gallery images:
- **Image Flow**: Camera/Gallery → URI → Base64 encoding → N8N API alongside text
- **Permissions**: Runtime permission handling for CAMERA and READ_MEDIA_IMAGES via [PermissionHelper.kt](app/src/main/java/com/max/aiassistant/utils/PermissionHelper.kt)
- **Share Intent**: App can receive shared text/URLs from other apps (configured in AndroidManifest)

### 3. Optimistic UI with API Sync
Tasks, subtasks, and notes use optimistic updates for instant feedback:
- **Pattern**: Update StateFlow immediately → Show change in UI → Sync to API in background → Revert if API fails
- **Example**: Creating a subtask shows it instantly with a temporary ID, then refreshes to get the real ID from backend
- **Benefits**: Feels fast and responsive even on slow networks

### 4. Futuristic Page Transitions
Navigation uses a custom fade-in/fade-out animation instead of standard Material transitions:
- **Two-phase animation**: Fade out current screen → Change page → Fade in new screen
- **Implementation**: [MainActivity.kt:497-555](app/src/main/java/com/max/aiassistant/MainActivity.kt#L497-L555) - `FuturisticTransition` composable
- **Timing**: 2000ms fade out + 1200ms fade in for a smooth, deliberate transition

### 5. Multi-API Coordination
The app orchestrates data from three independent APIs:
- **N8N**: Chat messages, tasks with subtasks, calendar events, user memory
- **OpenAI Realtime**: Voice-to-voice with real-time transcription
- **Open-Meteo**: Weather forecasts, pollen data, geocoding
- All coordinated in a single ViewModel with proper loading states and error handling

## Setup & Configuration

### Required: OpenAI API Key
To use the voice-to-voice feature, you **must** create a `local.properties` file in the project root:

```properties
# local.properties (not committed to git)
OPENAI_API_KEY=sk-proj-...your-key-here...
```

This key is loaded at build time and exposed via `BuildConfig.OPENAI_API_KEY`. The app will compile without it, but the voice feature will fail at runtime.

### Optional: Custom N8N Backend URL
The N8N backend URL is hardcoded in [MaxApiService.kt:224](app/src/main/java/com/max/aiassistant/data/api/MaxApiService.kt#L224). To use a different backend, modify the `BASE_URL` constant.

## Debugging

### API Logging
All API requests and responses are logged via OkHttp's `HttpLoggingInterceptor` at BODY level. Check Logcat with tag `MainViewModel` for:
- **N8N API**: `"Chargement des messages récents..."`, `"Réponse reçue: ..."`, `"Erreur lors de ..."`
- **OpenAI Realtime**: `"Connexion à l'API Realtime..."`, `"Transcription utilisateur reçue: ..."`, `"Transcription IA reçue: ..."`
- **Weather API**: `"Récupération des données météo pour ..."`
- **Task sync**: `"Synchronisation de la tâche ... avec l'API..."`

### Common Issues
1. **"Permission microphone requise" error**: RECORD_AUDIO permission not granted. Tap button again to retry permission request.
2. **Empty chat messages**: Check Logcat for API response structure from N8N (`response.text.data` array)
3. **Voice feature not working**: Verify `OPENAI_API_KEY` is set in `local.properties` and project is rebuilt
4. **Network errors**: Ensure emulator/device has internet access and can reach:
   - `https://n8n.srv1086212.hstgr.cloud/` (N8N backend)
   - `wss://api.openai.com/v1/realtime` (OpenAI WebSocket)
   - `https://api.open-meteo.com/` (Weather API)
5. **Build issues**: Run `./gradlew clean` or `.\gradlew.bat clean` and sync project with Gradle files
