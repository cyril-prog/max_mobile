# Verification roadmap UI/UX Max Android

Date de verification: 2026-04-11

## S0. Fondation design system

- `Fait`: tokens de couleur, type et theme centralises dans `app/src/main/java/com/max/aiassistant/ui/theme/`.
- `Fait`: design system documente dans `documentation/ui-design-system-max-android.md`.
- `Fait`: conventions UI documentees dans `documentation/ui-conventions-max-android.md`.
- `Fait`: cartographie ecran/parcours documentee dans `documentation/ui-screen-mapping-max-android.md`.

## S1. Shell et navigation

- `Fait`: shell unifie via `AppShell` et `MainActivity`.
- `Fait`: suppression du double chrome dans les ecrans routes (`Chat`, `Tasks`, `Notes`, `Weather`, `Voice`, `Planning`, `Actu`).
- `Fait`: badge global d'etat `online/offline/local/sync` visible dans `ui/common/AppShell.kt`.

## S2. Refactor des ecrans coeur

### Home

- `Fait`: hub prioritaire avec statut assistant.
- `Fait`: trois actions principales `Vocal`, `Chat`, `Taches`.
- `Fait`: modules secondaires repliables `Planning`, `Notes`, `Actu`.

### Chat

- `Fait`: header de session actif.
- `Fait`: actions par reponse IA `Copier`, `Relancer`, `Resumer`.
- `Fait`: feedback snackbars pour copie et envoi.
- `Fait`: cible tactile image ajustee a `48dp`.

### Taches

- `Fait`: organisation par horizons `Aujourd'hui`, `Planifie`, `Plus tard`.
- `Fait`: etats `loading`, `vide`, `offline`, `error`, `done`.
- `Fait`: reduction de friction via liste structuree avant ouverture des modales de detail.

### Notes

- `Fait`: creation, edition, lecture et suppression confirmable.
- `Fait`: feedback snackbars `cree`, `enregistre`, `supprime`, `checklist mise a jour`.
- `Fait`: cibles tactiles actions carte ajustees a `48dp`.

### Meteo

- `Fait`: priorite au bloc courant.
- `Fait`: fallback clair `loading`, `offline`, `error`, `aucune donnee`.

### Voice

- `Fait`: bandeaux explicites `erreur` et `hors ligne`.
- `Fait`: motion degradee quand les animations systeme sont desactivees.

### Planning et Actu

- `Fait`: bandeaux d'etat explicites branches sur les erreurs ViewModel.

## S3. Etats UI et feedback

- `Fait`: composants partages dans `ui/common/UiStates.kt`.
- `Fait`: `LoadingStateView`.
- `Fait`: `ErrorStateView`.
- `Fait`: `InlineStatusBanner`.
- `Fait`: `SectionHeader`.
- `Fait`: `QuickActionChip`.
- `Fait`: branchement des erreurs synchronisees dans `MainViewModel`.

## S4. Motion, accessibilite, finition premium

- `Fait`: prise en compte de la reduction des animations dans `EmptyStateView` et `VoiceScreen`.
- `Fait`: cibles tactiles critiques alignees sur une base `48dp` sur les actions les plus sensibles.
- `Fait`: hierarchie visuelle renforcee sur `Home`, `Chat`, `Tasks`.

## S5. Verification finale

- `Fait`: verification statique de la roadmap et tracabilite dans ce document.
- `Fait`: references d'inspiration appliquees pendant la refonte:
  - Android Compose Snackbar: `https://developer.android.com/develop/ui/compose/components/snackbar`
  - Android Compose Accessibility: `https://developer.android.com/develop/ui/compose/accessibility`
  - Samsung One UI: `https://developer.samsung.com/one-ui`
  - Apple navigation guidance: `https://developer.apple.com/documentation/swiftui/navigation`

## Conclusion

- Etat global: `roadmap couverte fonctionnellement`.
- Verification realisee sans build ni tests instrumentes.
- Si une validation runtime est voulue ensuite, la suite naturelle est un passage emulator sur `Home`, `Chat`, `Tasks`, `Notes`, `Meteo` et `Voice`.
