# 🚀 Quick Start - Max AI Assistant

## Structure complète du projet générée

```
Max_mobile/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/max/aiassistant/
│   │       │   │
│   │       │   ├── model/                 # 📦 Modèles de données
│   │       │   │   ├── Message.kt        # Messages du chat
│   │       │   │   ├── Task.kt           # Tâches avec priorités et statuts
│   │       │   │   └── Event.kt          # Événements calendrier
│   │       │   │
│   │       │   ├── viewmodel/            # 🧠 Logique métier
│   │       │   │   └── MainViewModel.kt  # ViewModel avec stubs API
│   │       │   │
│   │       │   ├── ui/
│   │       │   │   ├── chat/             # 💬 Écran central (messenger)
│   │       │   │   │   └── ChatScreen.kt
│   │       │   │   │
│   │       │   │   ├── voice/            # 🎤 Écran voice-to-voice
│   │       │   │   │   └── VoiceScreen.kt
│   │       │   │   │
│   │       │   │   ├── tasks/            # ✅ Écran tâches & planning
│   │       │   │   │   └── TasksScreen.kt
│   │       │   │   │
│   │       │   │   └── theme/            # 🎨 Design system
│   │       │   │       ├── Color.kt      # Palette dark mode
│   │       │   │       ├── Type.kt       # Typographie
│   │       │   │       └── Theme.kt      # Configuration Material 3
│   │       │   │
│   │       │   └── MainActivity.kt       # 🏠 Point d'entrée + HorizontalPager
│   │       │
│   │       ├── res/
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   └── themes.xml
│   │       │   └── xml/
│   │       │       ├── backup_rules.xml
│   │       │       └── data_extraction_rules.xml
│   │       │
│   │       └── AndroidManifest.xml       # Configuration app
│   │
│   ├── build.gradle.kts                  # 📋 Dépendances du module
│   └── proguard-rules.pro
│
├── build.gradle.kts                      # 🔧 Configuration racine
├── settings.gradle.kts                   # ⚙️ Configuration projet
├── gradle.properties                     # 🛠️ Propriétés Gradle
│
└── README.md                             # 📖 Documentation complète
```

## 🎯 Premiers pas

### 1. Ouvrir le projet

1. Lancez **Android Studio**
2. File → Open → Sélectionnez le dossier `Max_mobile`
3. Attendez la synchronisation Gradle (première fois : 2-5 minutes)

### 2. Vérifier la configuration

Android Studio devrait automatiquement :
- ✅ Télécharger les dépendances Compose
- ✅ Configurer le SDK Android
- ✅ Préparer l'émulateur

**Si erreurs** :
- Vérifiez que JDK 17 est installé
- File → Project Structure → SDK Location : Android SDK doit être configuré
- Tools → SDK Manager : Installer Android 13+ (API 33+)

### 3. Lancer l'application

**Option A : Émulateur**
1. Tools → Device Manager
2. Créer un nouveau device (ex: Pixel 7, Android 13+)
3. Cliquer sur Run ▶️

**Option B : Appareil physique**
1. Activer le mode développeur sur votre téléphone
2. Activer le débogage USB
3. Brancher en USB
4. Cliquer sur Run ▶️

### 4. Tester les fonctionnalités

Une fois l'app lancée :

- **Swipe gauche/droite** pour naviguer entre les 3 écrans
- **Écran central (Chat)** :
  - Tapez un message et envoyez-le
  - Observez la réponse simulée de Max
  - Cliquez sur le micro (affiche un Toast pour l'instant)

- **Écran gauche (Tasks)** :
  - Swipe à gauche sur une tâche → Supprime
  - Swipe à droite sur une tâche → Marque comme fait
  - Observez le calendrier de la semaine en haut

- **Écran droite (Voice)** :
  - Cliquez sur le bouton rouge "Écouter"
  - Observez l'onde audio qui s'anime
  - Lisez la transcription simulée en bas

## 🔧 Prochaines étapes (développement)

### Priorité 1 : Intégration API IA

**Fichier** : `viewmodel/MainViewModel.kt`

Recherchez `simulateAIResponse()` et remplacez par un vrai appel API.

**Exemple avec Retrofit** :

```kotlin
// 1. Ajouter dans build.gradle.kts
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// 2. Créer AiService.kt
interface AiService {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

// 3. Appeler depuis le ViewModel
viewModelScope.launch {
    val response = aiService.chat(ChatRequest(message))
    _messages.value = _messages.value + response.toMessage()
}
```

### Priorité 2 : Reconnaissance vocale

**Fichier** : `MainActivity.kt:69`

Décommentez la permission dans `AndroidManifest.xml` :
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Ajoutez le SpeechRecognizer :
```kotlin
val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
```

### Priorité 3 : Google Calendar

**Fichier** : `MainViewModel.kt:109`

Suivez la documentation Google Calendar API.

## 📚 Ressources d'apprentissage

- **Jetpack Compose** : https://developer.android.com/jetpack/compose
- **Material 3 Design** : https://m3.material.io/
- **Kotlin Flow** : https://kotlinlang.org/docs/flow.html
- **ViewModel** : https://developer.android.com/topic/libraries/architecture/viewmodel

## 🐛 Dépannage

### Erreur de build

```
FAILURE: Build failed with an exception.
```

**Solutions** :
1. File → Invalidate Caches → Restart
2. Build → Clean Project → Rebuild Project
3. Supprimez le dossier `.gradle` et `.idea`, puis rouvrez le projet

### L'app crash au démarrage

Vérifiez dans Logcat (filtre : `com.max.aiassistant`) pour voir les erreurs.

**Causes fréquentes** :
- Émulateur trop ancien (minSdk = 26)
- Manque de mémoire sur l'émulateur

### Le swipe ne fonctionne pas

- Vérifiez que vous êtes bien sur un émulateur/device avec gesture navigation
- Le swipe doit être horizontal (gauche/droite), pas vertical

## 💡 Conseils pour débutants

1. **Explorez les fichiers dans l'ordre** :
   - Commencez par `MainActivity.kt` pour comprendre la structure
   - Puis `MainViewModel.kt` pour la logique
   - Enfin les écrans individuels

2. **Tous les fichiers sont commentés** en français pour faciliter la compréhension

3. **Cherchez "TODO:"** dans le code pour trouver où ajouter vos fonctionnalités

4. **Modifiez et testez** :
   - Changez une couleur dans `Color.kt` → Observez le changement
   - Ajoutez un message dans `getMockMessages()` → Voyez-le apparaître
   - Changez le texte dans `VoiceScreen.kt` → Testez l'UI

5. **Utilisez Android Studio** :
   - Ctrl+Clic sur une fonction → Va à sa définition
   - Alt+Enter → Suggestions de correction
   - Ctrl+Espace → Autocomplétion

## ✅ Checklist de démarrage

- [ ] Projet ouvert dans Android Studio
- [ ] Gradle sync réussie (sans erreurs)
- [ ] Émulateur ou device connecté
- [ ] App lancée avec succès
- [ ] Swipe entre les 3 écrans fonctionne
- [ ] Message de test envoyé dans le chat
- [ ] Tâche swipée pour test
- [ ] Bouton voice cliqué et animation vue

Si tous ces points sont cochés, vous êtes prêt à développer ! 🎉

---

**Bon développement avec Max - AI Assistant !**
