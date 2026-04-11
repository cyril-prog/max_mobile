# Chat on-device pour Max Android

Le chat texte de l'application est maintenant branche sur un moteur local Android base sur `com.google.mediapipe:tasks-genai`.

## Pourquoi pas llama.cpp

Les fichiers presents dans [`model/`](../model/) sont:

- `gemma-4-E4B-it.litertlm`
- `gemma-4-E4B-it-web.task`

Ils ne sont pas au format `GGUF`, donc ils ne sont pas directement compatibles avec `llama.cpp`.
Le runtime retenu cote Android est donc la pile LiteRT / MediaPipe compatible avec les artefacts `litertlm` et `task`.

## Etat actuel

- Le chat Compose appelle maintenant le moteur local au lieu du webhook distant.
- L'application verifie au lancement si le modele est deja disponible.
- Si le modele manque, l'application le telecharge automatiquement une seule fois.
- Le fichier telecharge est verifie via SHA-256 avant activation.
- Le modele est stocke dans `getExternalFilesDir(null)/models/`.
- L'analyse d'image n'est pas encore recablee sur la session multimodale LiteRT.

## Emplacements de modele supportes

L'application cherche le modele dans l'ordre suivant:

- `${context.getExternalFilesDir(null)}/models/gemma-4-E2B-it.litertlm`
- `/data/local/tmp/llm/gemma-4-E2B-it.litertlm`
- `/data/local/tmp/llm/gemma-4-E4B-it.task`
- `/data/local/tmp/llm/model_version.task`
- `${context.getExternalFilesDir(null)}/models/gemma-4-E4B-it.task`

## Workflow de test recommande

En usage normal, aucun prechargement manuel n'est necessaire: le chat declenche automatiquement le provisioning du modele si besoin.

Pour un device de dev, la voie la plus simple reste `adb push`.

```powershell
adb shell mkdir -p /data/local/tmp/llm
adb push model/gemma-4-E2B-it.litertlm /data/local/tmp/llm/gemma-4-E2B-it.litertlm
```

Ensuite:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n com.max.aiassistant/.MainActivity
```

## Details du telechargement automatique

- URL du modele: `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true`
- Nom local: `gemma-4-E2B-it.litertlm`
- Verification SHA-256: `AB7838CDFC8F77E54D8CA45EADCEB20452D9F01E4BFADE03E5DCE27911B27E42`
- Le telechargement n'est pas relance si un fichier deja present passe la verification.
- En cas d'echec ou de fichier corrompu, l'application propose une relance depuis l'ecran de chat.

## Limites pratiques

- Le modele fait plusieurs Go: il ne doit pas etre embarque dans un APK classique.
- Les performances viseront surtout des telephones haut de gamme.
- Pour une distribution production, il faudra telecharger le modele au runtime, ou basculer vers un modele plus compact.
