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

### Clean and Build
```bash
./gradlew clean build
```

### Install Debug Build
```bash
./gradlew installDebug
```

### Run Specific Build Tasks
```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

### Clean Project
```bash
./gradlew clean
```

## Running Tests

Tests are not yet implemented, but the structure is in place for:
```bash
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## Key Architecture Details

### Single-Activity Navigation
The app uses a **HorizontalPager** for navigation instead of traditional fragments or navigation components. Three screens are navigable via horizontal swipe:
- Page 0: TasksScreen (left) - Task management and calendar
- Page 1: ChatScreen (center, default) - AI chat messenger
- Page 2: VoiceScreen (right) - Voice-to-voice interaction

The pager is configured in `MainActivity.kt:54-63` with `initialPage = 1` to start on the chat screen.

### State Management
All application state lives in `MainViewModel.kt` and is exposed via StateFlow:
- `messages: StateFlow<List<Message>>` - Chat messages
- `tasks: StateFlow<List<Task>>` - Task list with status/priority
- `events: StateFlow<List<Event>>` - Calendar events
- `isListening: StateFlow<Boolean>` - Voice recording state
- `voiceTranscript: StateFlow<String>` - Voice transcription

Each screen observes its required state using `collectAsState()` in the composable.

### Mock Data Architecture
All features currently use mocked data with stubs ready for real implementations:
- **AI responses**: `MainViewModel.kt:69` - `simulateAIResponse()` provides pattern-matched responses
- **Voice input**: `MainActivity.kt:86-94` - Shows a Toast, ready for SpeechRecognizer integration
- **Calendar events**: `MainViewModel.kt:118-124` - `refreshCalendarEvents()` stub for Google Calendar API

## Critical Implementation TODOs

### 1. AI API Integration (`MainViewModel.kt:56`)
Replace `simulateAIResponse()` with real API calls:
- Use Retrofit or Ktor for HTTP calls
- Target: OpenAI API, Claude API, or similar
- Handle network errors and timeouts
- Add loading states during API calls

Required dependencies (not yet added):
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
```

### 2. Speech Recognition (`MainActivity.kt:86-94`)
Implement real voice input:
- Use Android's SpeechRecognizer or Google Cloud Speech-to-Text
- Uncomment permission in AndroidManifest.xml line 14
- Request RECORD_AUDIO permission at runtime
- Connect to `onVoiceInput` callback

### 3. Google Calendar Integration (`MainViewModel.kt:118`)
Implement calendar sync:
- Use CalendarContract with ContentResolver
- Configure OAuth 2.0 authentication
- Uncomment permission in AndroidManifest.xml line 17
- Read events and map to Event model

## Theme and Design System

Located in `ui/theme/`:
- **Color.kt**: Dark mode palette (default theme)
  - Primary: #0A84FF (blue accent)
  - Background: #000000
  - Surface: #1C1C1E
- **Type.kt**: Typography definitions using default font family
- **Theme.kt**: Material 3 configuration with edge-to-edge enabled

Note: `window.statusBarColor` deprecation warning in Theme.kt:55 is non-critical.

## Important Caveats

### Permissions
Three permissions are defined in AndroidManifest.xml but two are commented out:
- ✅ INTERNET (enabled) - for API calls
- ⏸️ RECORD_AUDIO (line 14, commented) - uncomment when implementing voice
- ⏸️ READ_CALENDAR (line 17, commented) - uncomment when implementing calendar

### No Persistence Layer
- No Room database or DataStore implemented
- All state is held in memory (resets on app restart)
- ViewModel does not survive process death

### Gradle Configuration
- Kotlin version: 2.1.0
- Compose compiler: 2.1.0 (plugin-based)
- compileSdk: 35, minSdk: 26, targetSdk: 35
- JDK: 17 required

## Code Style Notes

- All code and comments are in French to match the original implementation
- Each file contains detailed inline comments explaining the code
- Search for "TODO:" in code to find integration points for real implementations
- Swipe gestures on tasks use Compose's SwipeToDismiss pattern (see TasksScreen.kt)

## Known Issues

- Deprecation warning for `window.statusBarColor` in Theme.kt:55 - non-blocking
- Tests are not implemented (structure exists but empty)
- Icon warning was previously resolved by creating adaptive icons in `res/mipmap-anydpi-v26/`