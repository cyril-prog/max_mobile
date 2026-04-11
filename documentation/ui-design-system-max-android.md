# Design System UI Max Android

## Objectif
Créer une base UI cohérente, rassurante et mobile-first alignée sur Material 3, One UI et les guidelines d'accessibilité Android.

## Principes
- Prioriser la tâche principale de chaque écran.
- Garder les actions fréquentes à portée du pouce.
- Montrer explicitement les états `loading`, `empty`, `error`, `offline`, `sync`.
- Réduire la densité visuelle des cartes et dialog complexes.
- Utiliser une motion courte et utile, jamais purement décorative.

## Tokens
- Couleurs: [Color.kt](c:/Users/cyril/projet/max_mobile/app/src/main/java/com/max/aiassistant/ui/theme/Color.kt)
- Typographie: [Type.kt](c:/Users/cyril/projet/max_mobile/app/src/main/java/com/max/aiassistant/ui/theme/Type.kt)
- Thème Material: [Theme.kt](c:/Users/cyril/projet/max_mobile/app/src/main/java/com/max/aiassistant/ui/theme/Theme.kt)

## Composants standardisés
- `MaxAppShell`: structure globale avec `TopAppBar`, navigation principale et destinations secondaires.
- `EmptyStateView`: état vide illustré.
- `LoadingStateView`: état de chargement central.
- `ErrorStateView`: état d'erreur avec action de reprise.
- `InlineStatusBanner`: message de contexte court dans un écran.
- `QuickActionChip`: action secondaire légère.
- `SectionHeader`: titre de section + action optionnelle.

## Navigation principale
- Destinations persistantes: `Accueil`, `Chat`, `Tâches`, `Notes`, `Météo`.
- Destinations secondaires: `Vocal`, `Planning`, `Actualités`.
- Le radar reste une destination immersive plein écran.

## Etats globaux
- `Mode local`
- `Mode cloud`
- `Synchronisation`
- `Mode hors ligne`
- `Reprise locale`

## Règles de composition
- Un seul shell visible à la fois.
- Les écrans intégrés dans `MaxAppShell` doivent désactiver leur chrome secondaire.
- Les écrans avec une action critique doivent fournir un feedback visible.
- Les écrans sans données ne doivent jamais afficher seulement un vide silencieux.

## Ecrans principaux
- `Accueil`: hub, statut, raccourcis, résumé du jour.
- `Chat`: session active, actions contextuelles sur messages IA.
- `Tâches`: tri par horizon temporel, création rapide, édition guidée.
- `Notes`: création rapide, lecture, édition distincte.
- `Météo`: aujourd'hui en premier, prévisions ensuite, fallback explicite.
