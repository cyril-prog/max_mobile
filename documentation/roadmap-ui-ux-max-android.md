# Roadmap UX/UI – Max Android

## Objectif
Élever l’interface actuelle vers un niveau “production” avec une expérience cohérente, lisible et fiable en s’alignant sur :
- Material Design / Android Developers
- One UI (Samsung)
- Adobe Spectrum (philosophie de composants consistants)
- Apple Human Interface Guidelines (lisibilité, accessibilité, organisation visuelle)

Le tout sans changer la logique métier, uniquement sur la couche expérience utilisateur.

## Principes directeurs (à respecter partout)
1. **Cohérence structurelle**
   - Même `TopAppBar`, rythme d’espacement, hiérarchie typographique.
   - Navigation principale claire (3 à 5 destinations), navigation secondaire explicite.
2. **Lisibilité d’abord**
   - Contraste suffisant, tailles de texte lisibles, cibles de touch >= 48dp.
   - Espacement régulier, lignes courtes, groupe de contenu logique.
3. **États explicites**
   - Chargement, vide, erreur, hors ligne, synchronisation : états visuels prévisibles.
4. **Motion utile**
   - Animations courtes, ciblées, cohérentes, désactivables selon les préférences OS.
5. **Actionnable**
   - Chaque écran propose un chemin principal clair, puis des actions secondaires.

## Références design utilisées
- Material Design (layout, composants, motion, accessibilité, offline)
- Android Material components (app bars, navigation, cards, chips, snackbar, bottom nav)
- One UI (mise en avant de la tâche, ergonomie mobile, hiérarchie de contenu)
- Spectrum (système visuel cohérent, composants standardisés)
- Apple HIG (alignement, touch controls, hit targets, contraste, proximité contrôle/contenu)

---

## État cible (vision)
- **Navigation**:  
  - Barre inférieure persistante avec 4–5 destinations principales.
  - Sidebar / drawer comme navigation secondaire et paramétrage.
- **Design system local**:  
 - Token de couleur, typographie, espacements, radius, élévation, boutons, chips, états.
- **Home comme hub**:  
  - Statut IA global + 3 actions prioritaires + 1 zone modules secondaires.
- **Offline/Sync visuel**:  
  - Distinction permanente local / cloud avec badges et libellés.
- **UX mobile rassurante**:
  - États chargement/erreur/vides homogènes sur tous les écrans.

---

## Roadmap détaillée (12 semaines)

### Sprint 0 (S0) – Préparation (2 jours)
#### Objectifs
- Figer le scope visuel et fonctionnel.
- S’aligner avec les principes de navigation et d’accessibilité.

#### Livrables
- `docs/` du design system initial (couleurs, typo, spacing, composants).
- Fichier de conventions UI (noms des tokens et usage).
- Mapping des 4–5 écrans principaux.

#### Critères d’acceptation
- Équipe d’un seul écran = même système visuel.
- Liste des composants standardisés validée (Bar, Card, Chip, Fab, SnackBar, EmptyState).

---

### Sprint 1 (S1) – Fondations de navigation et shell (P0)
#### Objectifs
- Réduire le chaos structurel actuel.

#### Tâches
1. Construire un `AppShell` commun :
   - `TopAppBar` unifié.
   - `NavigationBar` / `BottomAppBar` pour 4–5 destinations.
2. Sortir le mode de navigation “tous à la même priorité”.
3. Mettre en place la structure visuelle de base:
   - espacement, cartes, titres, icônes, actions.
4. Ajouter le “mode état global” dans la barre:
   - `online / offline / local mode / sync`.

#### Critères d’acceptation
- Navigation cohérente sur les écrans principaux.
- Pas d’écran “orphelin” sans barres structurelles.
- L’utilisateur peut comprendre les destinations principales en 3 secondes.

---

### Sprint 2 (S2) – Coeur expérience (P0 / P1)
#### Objectifs
- Rendre chaque écran lisible et orienté tâche.

#### Tâches par écran
- **Accueil**
  - Hub simple: statut IA, 3 actions principales.
  - Sections secondaires repliables.
- **Chat**
  - Header de session: modèle actif + mode de fonctionnement.
  - Actions de message visibles (copier, relancer, résumer).
- **Notes**
  - Création rapide + consultation simplifiée.
  - Distinguer création, aperçu, édition.
- **Tâches**
  - Structuration “Aujourd’hui / Planifié / Plus tard”.
  - Réduire le nombre de modales imbriquées.
- **Météo**
  - Priorité “aujourd’hui”, puis prévisions.
  - Fallback clair si données indisponibles.

#### Critères d’acceptation
- Le besoin principal de chaque écran est exécutable en moins de 2 actions.
- Aucune carte principale ne dépasse la densité recommandée (lisibilité préservée sur mobile).

---

### Sprint 3 (S3) – États, feedback, robustesse (P1)
#### Objectifs
- Apporter du confort et de la confiance.

#### Tâches
1. Standardiser les états:
   - `EmptyState`, `Loading`, `Erreur`, `Offline`, `Sync`.
2. Standardiser feedback:
   - `Snackbar` actions et messages courts.
   - Confirmation de suppression/sauvegarde.
3. Ajouter indicateurs statutaires:
   - Source données locale vs distante.
   - Permissions/erreurs en Voice.
4. Optimiser accessibilité:
   - Labels content description, contraste, cibles de touch, hiérarchie claire.

#### Critères d’acceptation
- Tous les écrans ont un `EmptyState` et une variante `Erreur`.
- Aucun appel d’action critique sans feedback visuel.
- Contraste vérifié pour les zones de texte de base.

---

### Sprint 4 (S4) – Motion et finition visuelle (P2)
#### Objectifs
- Donner une identité pro, sobre, premium.

#### Tâches
1. Motion système:
   - Transitions entre écran et modales courtes et uniformes.
2. Rationaliser les effets:
   - Réduire gradients et glow excessifs au profit de contraste fonctionnel.
3. Harmoniser le rythme des composants:
   - corners, shadows, elevation légère, chips et boutons uniformes.
4. Ajuster la réactivité:
   - Comportements cohérents en portrait/paysage et sur tailles de texte.

#### Critères d’acceptation
- Animation perçue comme “utile”, pas décorative.
- Identité visuelle uniforme, sans écran “hors charte” visible.

---

### Sprint 5 (S5) – Validation finale et qualité
#### Objectifs
- Vérifier la conformité UX et fermer les écarts.

#### Tâches
1. Revue écran par écran avec grille d’évaluation.
2. Vérification d’accessibilité (lecture, contrastes, cible touch).
3. Ajustements finaux sur labels, hiérarchie, densité.
4. Documenter les usages et pattern de design système à conserver.

#### Critères d’acceptation
- Score de cohérence visuelle stable sur tous les écrans.
- Documentation prête à devenir la base du design system projet.

---

## Backlog P0 / P1 / P2

### P0 (obligatoire pour passer en version beta interne)
- AppShell avec navigation principale persistante.
- Hub Home simplifié.
- États globaux (offline/local/sync).
- Réduction de la surcharge navigationnelle.
- États d’erreur/chargement/vides uniformisés.

### P1 (amélioration UX forte)
- Structuration claire par écran + sections logiques.
- Accessibilité et cibles tactiles.
- Feedback systématique des actions.
- Distinction local/cloud visuelle généralisée.
- Motion cohérente et option de réduction.

### P2 (qualité premium)
- Raffinement visuel final.
- Micro-interactions cohérentes.
- Détails de parcours (édition, retour en erreur, reprise d’activité).
- Ajustements adaptatifs finaux mobile/compact.

---

## Métriques de succès
- **Compréhension navigation**: 80 % des clics vers les destinations principales en < 2 écrans.
- **Temps vers la tâche principale**:
  - Chat: < 3 taps pour démarrer une discussion.
  - Notes: < 2 taps pour créer une note.
  - Tâches: < 4 taps pour ajouter une tâche.
- **Perception qualité** (mini-test interne):
  - > 85 % de retours “on comprend où je suis / quoi faire”.
- **Ergonomie**:
  - 0 écran présentant ambiguïté bloquante sur état erreur/chargement.
  - 100 % des actions critiques ont feedback visuel.

---

## Risques et dépendances
- Risque de régression visuelle si refonte navigation trop rapide.
- Dépendance avec les flux existants de données/états.
- Réglages de style peut impacter des écrans annexes (voice, actu, radar).

### Mesures d’atténuation
- Migration écran par écran avec validation visuelle.
- Règles de composition strictes dans le design system.
- Révisions hebdomadaires de cohérence.

---

## Prochaine étape (immédiate)
1. Décider du nom de la cible de navigation (ex: `home`, `chat`, `notes`, `tasks`, `weather`).
2. Valider la liste des composants minimaux pour le design system.
3. Démarrer le Sprint 1 avec la structure `AppShell` + bottom navigation.

