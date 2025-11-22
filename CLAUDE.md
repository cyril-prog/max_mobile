# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Max - AI Assistant** is a modern Android mobile application built with Kotlin and Jetpack Compose. It's an AI assistant with three interaction modes: text chat, voice-to-voice, and task/calendar management.

- **Project Name**: Max-AI-Assistant
- **Package**: com.max.aiassistant
- **Architecture**: Single-Activity with MVVM pattern
- **UI Framework**: 100% Jetpack Compose (no XML layouts)
- **Language**: Kotlin with StateFlow for reactive state management

## Build Commands

### Windows (using gradlew.bat)
```bash
.\gradlew.bat clean build
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
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

## Key Architecture Details

### Single-Activity Navigation
The app uses a **HorizontalPager** for navigation instead of traditional fragments or navigation components. Three screens are navigable via horizontal swipe:
- **Page 0: VoiceScreen (default start)** - Voice-to-voice interaction with audio visualization
- **Page 1: ChatScreen** - AI chat messenger
- **Page 2: TasksScreen** - Task management and calendar events

The pager is configured in `MainActivity.kt:63-66` with `initialPage = 0` to start on the VoiceScreen.

### State Management
All application state lives in `MainViewModel.kt` and is exposed via StateFlow:
- `messages: StateFlow<List<Message>>` - Chat messages loaded from API
- `tasks: StateFlow<List<Task>>` - Task list fetched from API
- `events: StateFlow<List<Event>>` - Calendar events from API
- `isListening: StateFlow<Boolean>` - Voice recording state
- `voiceTranscript: StateFlow<String>` - Voice transcription (currently mocked)
- `isLoadingTasks: StateFlow<Boolean>` - Loading state for tasks
- `isLoadingEvents: StateFlow<Boolean>` - Loading state for events

Each screen observes its required state using `collectAsState()` in the composable.

### API Integration Architecture

The app is **fully integrated with a real backend API** using Retrofit:

**API Service**: `data/api/MaxApiService.kt`
- Base URL: `https://n8n.srv1086212.hstgr.cloud/`
- Uses OkHttp with logging interceptor for debugging
- 30-second timeouts for all requests
- Gson converter for JSON serialization

**API Endpoints**:
1. `GET /webhook/max_mobile?text={message}` - Send chat message, returns AI response
2. `GET /webhook/get_recent_messages` - Load recent chat history
3. `GET /webhook/get_tasks` - Fetch task list
4. `GET /webhook/get_calendar` - Fetch calendar events

**API Models**: `data/api/ApiModels.kt` and `data/api/MessagesApiModels.kt`
- Response models map API data to app models (Message, Task, Event)
- Extension functions (e.g., `toTask()`, `toEvent()`) convert API models to domain models

**ViewModel Integration**: `viewmodel/MainViewModel.kt`
- `init` block calls `loadRecentMessages()`, `refreshTasks()`, and `refreshCalendarEvents()` on startup
- `sendMessage()` posts user messages to API and displays responses in real-time
- Error handling shows user-friendly error messages in chat
- All API calls run in `viewModelScope` for lifecycle-aware coroutines

### Data Flow
1. **App Launch**: `MainViewModel.init` automatically loads messages, tasks, and events from API
2. **Chat**: User sends message → `sendMessage()` → API call → AI response added to message list
3. **Tasks**: Pull-to-refresh → `refreshTasks()` → API call → tasks updated
4. **Events**: Pull-to-refresh → `refreshCalendarEvents()` → API call → events updated

## Critical Implementation TODOs

### 1. Speech Recognition (Voice Input)
**Status**: Currently mocked with hardcoded transcript

**File**: `MainViewModel.kt:232-243` - `toggleListening()`

**What to implement**:
- Use Android's SpeechRecognizer or Google Cloud Speech-to-Text
- Uncomment permission in `AndroidManifest.xml:14`
- Request RECORD_AUDIO permission at runtime
- Replace the 2-second delay simulation with real audio capture
- Update `_voiceTranscript` StateFlow with real transcription results

**Permission needed**:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### 2. Google Calendar Integration
**Status**: Currently fetches events from custom API, not Google Calendar

**File**: `MainViewModel.kt:193-212` - `refreshCalendarEvents()`

**What to implement**:
- Use CalendarContract with ContentResolver to read device calendar
- Configure OAuth 2.0 authentication for Google Calendar API
- Uncomment permission in `AndroidManifest.xml:17`
- Replace API call with local calendar read or Google Calendar API

**Permission needed**:
```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
```

### 3. Voice-to-Voice (Full Voice Assistant)
**Status**: Voice visualization UI exists, but no speech synthesis

**What to implement**:
- Integrate Text-to-Speech (TTS) for AI responses
- Send transcribed voice input to chat API
- Play AI response as audio instead of text
- Add audio playback controls and visualization

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
Three permissions are defined in AndroidManifest.xml but two are commented out:
- ✅ **INTERNET** (enabled) - for API calls (actively used)
- ⏸️ **RECORD_AUDIO** (line 14, commented) - uncomment when implementing voice input
- ⏸️ **READ_CALENDAR** (line 17, commented) - uncomment when implementing local calendar sync

### No Persistence Layer
- No Room database or DataStore implemented
- All state is held in memory (resets on app restart)
- Messages, tasks, and events are reloaded from API each time app starts
- ViewModel does not survive process death

### Gradle Configuration
- **Kotlin version**: 2.1.0
- **Compose compiler**: 2.1.0 (plugin-based with `org.jetbrains.kotlin.plugin.compose`)
- **compileSdk**: 35, **minSdk**: 26, **targetSdk**: 35
- **JDK**: 17 required
- **Android Gradle Plugin**: 8.9.0

### Dependencies Already Installed
The following dependencies are already in `app/build.gradle.kts`:
- Retrofit 2.9.0 (for API calls)
- Gson converter (for JSON parsing)
- OkHttp logging interceptor (for debugging)
- Jetpack Compose BOM 2024.09.00
- Material 3 and Material Icons Extended
- Accompanist SwipeRefresh 0.32.0

## Code Style Notes

- **All code and comments are in French** to match the original implementation
- Each file contains detailed inline comments explaining the code
- Search for "TODO:" in code to find integration points for remaining implementations
- Swipe gestures on tasks use Compose's SwipeToDismiss pattern (see `TasksScreen.kt`)

## Debugging

### API Logging
API requests and responses are logged via OkHttp's `HttpLoggingInterceptor` at BODY level. Check Logcat with tag `MainViewModel` for:
- Message loading: `"Chargement des messages récents..."`
- API responses: `"Réponse reçue: ..."`
- Errors: `"Erreur lors de ..."`

### Common Issues
- **Network errors**: Ensure emulator/device has internet access and can reach `https://n8n.srv1086212.hstgr.cloud/`
- **Empty message list**: Check Logcat for API response structure, ensure `response.text.data` array is not empty
- **Build issues**: Run `./gradlew clean` and sync project with Gradle files
