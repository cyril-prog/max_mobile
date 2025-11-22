# 🔧 Fix - Problème "Connecting to Emulator" résolu

## Problème identifié

L'émulateur était en mode **"offline"** - il n'était pas complètement démarré ou était bloqué.

## Solution appliquée

```bash
# 1. Arrêt des daemons Gradle
./gradlew --stop

# 2. Redémarrage d'ADB (Android Debug Bridge)
adb kill-server && adb start-server

# 3. Kill de l'émulateur bloqué
adb -s emulator-5554 emu kill
```

L'émulateur a été **tué proprement**. Il est maintenant prêt à être redémarré.

## Étapes pour redémarrer l'application

### Méthode 1 : Via Android Studio (Recommandé)

1. **Redémarrer l'émulateur :**
   - Tools → Device Manager
   - Sélectionnez votre émulateur Android 13
   - Cliquez sur ▶️ pour le démarrer
   - **IMPORTANT** : Attendez 30-60 secondes que l'écran d'accueil Android soit complètement visible

2. **Vérifier que l'émulateur est prêt :**
   - L'écran d'accueil doit être affiché
   - Pas de message "Android is starting..."
   - L'émulateur doit être réactif (vous pouvez swiper l'écran)

3. **Lancer l'application :**
   - Cliquez sur **Run** ▶️ dans Android Studio
   - Attendez que "Launching app" apparaisse
   - L'app devrait se lancer en 10-15 secondes

### Méthode 2 : Via ligne de commande

Si vous voulez vérifier que tout fonctionne en ligne de commande :

```bash
# 1. Vérifier qu'aucun émulateur n'est en cours
adb devices
# Résultat attendu : "List of devices attached" (vide)

# 2. Démarrer l'émulateur depuis Android Studio (Tools → Device Manager)

# 3. Attendre puis vérifier la connexion
adb devices
# Résultat attendu : emulator-5554   device (PAS "offline")

# 4. Installer l'app
cd "C:\Users\cyril\IdeaProjects\Max_mobile"
./gradlew installDebug

# 5. Lancer l'app
adb shell am start -n com.max.aiassistant/.MainActivity
```

## Vérifier que l'émulateur est prêt

Avant de lancer l'app, assurez-vous que l'émulateur affiche :

✅ Écran d'accueil Android visible
✅ Pas de "Android is starting..."
✅ L'émulateur répond au touch/swipe
✅ Dans le terminal : `adb devices` affiche "device" (PAS "offline")

## Si le problème persiste

### Option A : Effacer les données de l'émulateur

1. Tools → Device Manager
2. Menu ⋮ à côté de l'émulateur → **Wipe Data**
3. Redémarrez l'émulateur
4. Relancez l'app

### Option B : Créer un nouvel émulateur

Les émulateurs Android 13 peuvent être lourds. Créez un émulateur optimisé :

1. **Device Manager → Create Device**
2. Sélectionnez **Pixel 5** (taille moyenne, bon équilibre)
3. Choisissez **API Level 33** (Android 13) comme votre téléphone
4. Cliquez sur **Advanced Settings** :
   - RAM : **2048 MB** minimum (4096 recommandé si vous avez assez de RAM PC)
   - VM Heap : **512 MB**
   - Internal Storage : **2048 MB**
   - SD Card : **512 MB**
5. Graphics : **Hardware - GLES 2.0** (plus performant)
6. Finish

### Option C : Utiliser votre téléphone physique (Plus fiable)

Si votre téléphone est Android 13 comme vous l'avez mentionné :

1. **Activer le mode développeur :**
   - Paramètres → À propos du téléphone
   - Appuyez 7 fois sur "Numéro de build"

2. **Activer le débogage USB :**
   - Paramètres → Options pour les développeurs
   - Activez "Débogage USB"

3. **Connecter le téléphone :**
   - Branchez en USB
   - Autorisez le débogage sur le téléphone
   - Vérifiez : `adb devices` devrait afficher votre téléphone

4. **Lancer l'app :**
   - Dans Android Studio, sélectionnez votre téléphone dans la liste
   - Cliquez sur Run ▶️

## Diagnostic rapide

Pour vérifier l'état actuel :

```bash
# État d'ADB
adb devices

# Si vous voyez "offline" :
adb kill-server && adb start-server
adb devices

# Si l'émulateur est bloqué :
adb -s emulator-5554 emu kill

# Logs de l'émulateur (si besoin de débugger)
adb logcat | grep "Max"
```

## Astuce : Démarrage rapide

Pour éviter ce problème à l'avenir :

1. **Ne fermez pas brutalement l'émulateur** - Utilisez le bouton Power virtuel
2. **Laissez l'émulateur ouvert** pendant vos sessions de dev
3. **Utilisez "Cold Boot"** occasionnellement pour nettoyer :
   - Device Manager → ⋮ → Cold Boot Now

## APK déjà disponible

Rappel : Votre APK est déjà compilé et prêt !

📦 **APK** : `app\build\outputs\apk\debug\app-debug.apk` (16 MB)

Vous pouvez l'installer manuellement :
```bash
adb install -r "C:\Users\cyril\IdeaProjects\Max_mobile\app\build\outputs\apk\debug\app-debug.apk"
```

---

**Résumé** : L'émulateur a été nettoyé. Redémarrez-le proprement depuis Android Studio, attendez qu'il soit complètement démarré (écran d'accueil visible), puis lancez l'app avec Run ▶️.
