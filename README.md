# Max - AI Assistant 🤖

Application mobile Android moderne construite avec **Kotlin** et **Jetpack Compose**.

## 📱 Description

Max est un assistant IA interactif qui propose 3 modes d'interaction :

1. **Chat textuel** - Conversez avec Max via une interface de messagerie
2. **Voice to Voice** - Interaction vocale avec visualisation audio en temps réel
3. **Planning & Tâches** - Gestion de vos tâches et événements du calendrier

## 🎨 Design

- **Mode sombre par défaut** avec une palette minimaliste
- **Navigation par swipe horizontal** entre les 3 écrans
- **UI moderne** inspirée d'iOS avec des transitions fluides
- **Material Design 3** pour une expérience native Android

## 🏗️ Architecture

### Stack technique

- **Langage** : Kotlin
- **UI Framework** : Jetpack Compose (100% déclaratif, pas de XML)
- **Architecture** : Single-Activity avec MVVM
- **Navigation** : HorizontalPager pour le swipe entre écrans
- **État réactif** : StateFlow et Compose State

### Structure du projet

```
com.max.aiassistant/
├── model/                  # Modèles de données
│   ├── Message.kt         # Messages du chat
│   ├── Task.kt           # Tâches avec priorités
│   └── Event.kt          # Événements du calendrier
│
├── viewmodel/             # Logique métier
│   └── MainViewModel.kt  # ViewModel central avec stubs API
│
├── ui/
│   ├── chat/             # Écran de messagerie
│   │   └── ChatScreen.kt
│   │
│   ├── voice/            # Écran voice-to-voice
│   │   └── VoiceScreen.kt
│   │
│   ├── tasks/            # Écran tâches & planning
│   │   └── TasksScreen.kt
│   │
│   └── theme/            # Thème et design system
│       ├── Color.kt
│       ├── Type.kt
│       └── Theme.kt
│
└── MainActivity.kt       # Point d'entrée avec HorizontalPager
```

## 🚀 Démarrage

### Prérequis

- Android Studio Hedgehog ou plus récent
- JDK 17
- Android SDK 26+ (minSdk)
- Android 13+ recommandé (targetSdk 35)

### Installation

1. Clonez le projet
2. Ouvrez-le dans Android Studio
3. Synchronisez les dépendances Gradle
4. Lancez l'app sur un émulateur ou appareil physique

### Configuration

Le projet est prêt à l'emploi avec des **données mockées**. Aucune configuration supplémentaire n'est requise pour tester l'interface.

## 📚 Fonctionnalités actuelles

### ✅ Implémenté (UI + Logique mock)

- Navigation horizontale par swipe entre 3 écrans
- **Chat** : Interface de messagerie avec bulles utilisateur/IA
- **Voice** : Visualiseur d'onde animé + bouton d'écoute
- **Tâches** : Calendrier hebdomadaire + liste TO DO avec badges de priorité
- **Planning** : Affichage des événements du jour
- Swipe-to-dismiss sur les tâches (marquer fait / supprimer)
- Thème sombre cohérent sur toute l'app
- Données mock pour démonstration

### 🔜 À implémenter (Stubs prêts)

#### 1. Intégration API IA

**Fichier** : `MainViewModel.kt:56` - Fonction `sendMessage()`

```kotlin
// TODO: Remplacer simulateAIResponse() par :
// - Appel HTTP vers OpenAI API / Claude API / autre
// - Utiliser Retrofit ou Ktor
// - Gérer les erreurs réseau et timeout
```

**Dépendances suggérées** :
```gradle
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
```

#### 2. Reconnaissance vocale (Speech-to-Text)

**Fichier** : `MainActivity.kt:69` - `onVoiceInput` callback

```kotlin
// TODO: Implémenter SpeechRecognizer Android
// - Demander permission RECORD_AUDIO
// - Ou intégrer Google Cloud Speech-to-Text API
```

**Permissions nécessaires** (AndroidManifest.xml) :
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

#### 3. Intégration Google Calendar

**Fichier** : `MainViewModel.kt:109` - Fonction `refreshCalendarEvents()`

```kotlin
// TODO: Utiliser CalendarContract
// - Configurer OAuth 2.0
// - Lire les événements via ContentResolver
```

**Permissions nécessaires** :
```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
```

## 🎯 Guide d'extension

### Ajouter une vraie API IA

1. Créez un service Retrofit dans `data/api/AiService.kt`
2. Ajoutez votre clé API dans `local.properties` (ne jamais commit)
3. Modifiez `simulateAIResponse()` dans `MainViewModel.kt`
4. Ajoutez la permission INTERNET (déjà présente)

### Implémenter la reconnaissance vocale

1. Ajoutez `android.speech.SpeechRecognizer` dans un nouveau composable
2. Demandez la permission RECORD_AUDIO au runtime
3. Connectez le callback `onVoiceInput` à votre recognizer
4. Mettez à jour `voiceTranscript` dans le ViewModel

### Personnaliser le thème

Modifiez les fichiers dans `ui/theme/` :

- **Colors** : `Color.kt` - Palette de couleurs
- **Typography** : `Type.kt` - Polices et tailles
- **Theme** : `Theme.kt` - Configuration Material 3

## 🧪 Tests

Structure prévue pour les tests (à implémenter) :

```
app/src/
├── test/              # Tests unitaires
│   └── viewmodel/
│       └── MainViewModelTest.kt
│
└── androidTest/       # Tests UI
    └── ChatScreenTest.kt
```

## 📝 Notes importantes

- **Données mockées** : L'app utilise des données simulées pour tous les écrans
- **Stubs commentés** : Recherchez "TODO:" dans le code pour trouver les points d'extension
- **Architecture prête** : Le ViewModel est conçu pour une intégration facile des vraies API
- **Permissions** : Les permissions pour micro et calendrier sont commentées (décommentez selon besoin)

## 🤝 Contribution

Pour contribuer :

1. Choisissez un stub à implémenter (cherchez "TODO:" dans le code)
2. Suivez les commentaires détaillés dans chaque fichier
3. Testez sur différentes tailles d'écran
4. Respectez le style de code existant (ktlint recommandé)

## 📄 Licence

Ce projet est un squelette d'apprentissage. Adaptez-le selon vos besoins.

---

**Créé avec ❤️ et Jetpack Compose**

*Pour les développeurs qui débutent en Android : chaque fichier contient des commentaires détaillés expliquant le code ligne par ligne. N'hésitez pas à explorer !*
