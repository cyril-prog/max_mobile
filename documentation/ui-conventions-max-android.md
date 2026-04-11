# Conventions UI Max Android

## Nommage
- Ecrans: `XxxScreen`
- Contenu sans chrome: `XxxScreenContent`
- Etat visuel partagé: `XxxStateView` ou composant dans `ui/common`
- Sections: `SectionHeader`

## Layout
- Padding horizontal principal: `Spacing.md`
- Espacement vertical standard: `Spacing.sm` ou `Spacing.md`
- Cards principales: coins arrondis larges, fond `DarkSurface`

## Navigation
- `showChrome = false` quand un écran est rendu dans `MaxAppShell`
- Pas de sidebar locale concurrente quand le shell principal est présent

## Typographie
- `titleLarge`: titre d'écran
- `titleMedium`: titre de section ou carte principale
- `titleSmall`: sous-section et éléments importants
- `bodyLarge`: contenu principal
- `bodyMedium`: texte secondaire
- `labelLarge` et `labelSmall`: chips, badges et méta-infos

## Etats
- `LoadingStateView` pour chargement bloquant
- `EmptyStateView` pour absence de contenu
- `ErrorStateView` pour échec de récupération ou d'action
- `InlineStatusBanner` pour permissions, offline, sync et confirmations

## Feedback
- Actions critiques: confirmation ou bannière explicite
- Suppression: confirmation avant exécution
- Sauvegarde ou création: feedback court visible
- Rafraîchissement impossible: message d'erreur avec reprise

## Accessibilité
- Toutes les actions significatives doivent avoir un `contentDescription`
- Les icônes purement décoratives peuvent garder `contentDescription = null`
- Les éléments interactifs doivent conserver une cible tactile confortable
- Les animations décoratives doivent pouvoir être atténuées si le système les désactive

## Mapping principal
- `home`
- `chat`
- `tasks`
- `notes`
- `weather`
