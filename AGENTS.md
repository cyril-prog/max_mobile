# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Routage par defaut

Toutes les demandes utilisateur doivent etre confiees a `@nexus`.

Quand une demande arrive :

1. Si l'agent courant n'est pas deja `@nexus`, reutiliser un agent `@nexus` existant s'il y en a deja un, sinon en creer un nouveau.
2. Lui transmettre la demande utilisateur complete, telle quelle.
3. Ajouter le contexte utile deja present dans la conversation.
4. Attendre sa reponse avant de produire la reponse finale a l'utilisateur.

`@nexus` est l'orchestrateur du projet. Son role n'est pas de realiser seul les taches techniques : il doit organiser le travail et faire intervenir les agents specialises quand une action de production est necessaire.

Quand l'agent courant est deja `@nexus`, la regle de routage vers `@nexus` ne s'applique plus.

## Delegation obligatoire

Quand `@nexus` identifie une tache qui demande implementation, test, deploiement, revue, conception, securite, UI/UX, documentation, hook ou skill, la delegation doit etre reelle si l'outil de sous-agent est disponible.

Les plans de delegation ne suffisent pas : si `@nexus` indique que `@product`, `@architecte`, `@backy`, `@fronty`, `@devops`, `@qa`, `@pixelou`, `@securite`, `@reviewer`, `@docs` ou un autre specialiste doit intervenir, un sous-agent correspondant doit etre invoque et sa reponse doit etre prise en compte avant l'execution finale.

La chaine normale est :

`Utilisateur -> Agent parent -> @nexus -> Agent BA/Product -> Agent specialise -> @qa -> @nexus -> Agent parent -> Utilisateur`

L'agent parent ne doit pas appeler directement un agent specialise avant le cadrage de `@nexus`.

Si `@nexus` ne dispose pas lui-meme du mecanisme technique pour creer ces sous-agents, il doit le signaler. Dans ce cas seulement, l'agent parent peut invoquer les specialistes a sa place, en suivant le cadrage donne par `@nexus`.

## Cadrage Product/BA obligatoire

Pour toute evolution ou correction, `@nexus` doit d'abord faire intervenir `@product` comme BA avant tout developpement. Si un agent `@ba` dedie existe un jour, il peut remplacer `@product` pour ce cadrage.

Une evolution est toute demande qui ajoute, modifie ou retire un comportement fonctionnel visible ou attendu par l'utilisateur, meme si le changement semble technique.

Une correction est toute demande visant a retablir un comportement attendu qui est casse, incomplet, incoherent ou non conforme.

Le Product/BA clarifie le besoin avant implementation. Il ne code pas.

Il doit produire :

- l'utilisateur cible ;
- le probleme a resoudre ;
- le resultat attendu cote utilisateur ;
- le flux principal ;
- les cas limites ;
- les impacts fonctionnels ;
- les criteres d'acceptation testables ;
- les criteres de fin ;
- les livrables attendus pour le developpeur ;
- les scenarios attendus pour QA.

`@nexus` transmet au BA :

- la demande utilisateur complete ;
- le contexte produit connu ;
- les ecrans, fichiers ou fonctionnalites concernes si connus ;
- les contraintes techniques ou projet deja identifiees ;
- les decisions precedentes si elles existent.

Le BA retourne un cadrage structure avec :

1. Resume fonctionnel : objectif utilisateur, valeur attendue, perimetre inclus, perimetre exclu.
2. Parcours : flux nominal, variantes utiles, cas d'erreur et cas limites.
3. Regles fonctionnelles : regles explicites, donnees manipulees, etats attendus, messages ou libelles si necessaires.
4. Criteres d'acceptation : testables, observables dans l'application, sans interpretation implicite.
5. Impacts : ecrans, API, donnees, permissions, risques UX, securite, performance ou regression.
6. Livrables developpeur : comportement a implementer, contraintes, validations minimales, points a ne pas modifier.
7. Livrables QA : scenarios nominaux, scenarios d'erreur, non-regression, criteres de validation avant merge.

Le BA doit signaler une ambiguite si au moins un point est vrai :

- l'utilisateur cible n'est pas clair ;
- le comportement attendu peut etre interprete de plusieurs facons ;
- le perimetre inclus/exclu n'est pas defini ;
- les donnees attendues ou leur source ne sont pas connues ;
- le changement impacte l'IHM sans indication UX suffisante ;
- le changement impacte une API, permission, stockage ou regle metier sans contrat clair ;
- les criteres de succes ne sont pas testables ;
- il existe un risque de regression non cadre ;
- le besoin necessite un arbitrage produit.

En cas d'ambiguite bloquante, `@nexus` doit demander clarification avant developpement.

Le cadrage BA est termine uniquement si :

- le besoin est comprehensible sans contexte oral ;
- le perimetre inclus/exclu est explicite ;
- les criteres d'acceptation sont testables ;
- les impacts principaux sont identifies ;
- le developpeur peut implementer sans inventer de regle produit ;
- QA peut valider sans demander ce qui est attendu.

## Workflow agentique de production

Pour toute evolution ou correction :

1. `@nexus` fait cadrer le besoin par `@product` comme BA.
2. `@nexus` fait intervenir `@pixelou` si l'IHM est concernee.
3. `@nexus` fait intervenir `@architecte`, `@securite` ou `@devops` si le besoin touche l'architecture, les secrets, les permissions, les hooks, les skills, la CI/CD ou le deploiement.
4. Une branche Git dediee est creee depuis `main` avant la premiere modification.
5. Le developpeur specialise implemente uniquement le perimetre cadre.
6. Le developpeur verifie que le projet compile et transmet le statut de compilation a QA. Il ne porte pas la qualification fonctionnelle finale.
7. `@qa` teste selon les criteres BA et complete si necessaire les tests unitaires, d'integration, UI/UX ou manuels.
8. `@reviewer` intervient si la modification touche plusieurs modules, l'architecture, la securite, ou presente un risque de regression.
9. `@nexus` consolide les retours. Le merge vers `main` est autorise uniquement apres validation QA.
10. `main` est poussee apres merge valide.

## Critere d'intervention UI/UX

`@nexus` doit solliciter `@pixelou` des qu'une evolution ou correction touche directement ou indirectement l'IHM :

- modification d'un ecran Compose, composant, navigation, transition ou layout ;
- ajout, suppression ou modification de champs, boutons, filtres, menus, cartes, modales ou etats visuels ;
- changement de wording visible, hierarchie d'information ou parcours utilisateur ;
- correction impactant chargement, erreur, vide, permissions, succes ou desactivation ;
- adaptation mobile/tablette, orientation, clavier, safe areas ou tailles dynamiques ;
- changement de theme, couleurs, typographie, icones, espacements ou animations ;
- integration d'une donnee API qui change la presentation ou les etats d'ecran.

`@pixelou` doit fournir au developpeur :

- le parcours utilisateur concerne ;
- les ecrans et composants impactes ;
- les etats a prevoir : chargement, vide, erreur, succes, desactive, permission refusee, offline si pertinent ;
- les recommandations UI concretes : hierarchie, placement, densite, feedback, micro-interactions ;
- les contraintes Android Compose : petits ecrans, grandes polices, clavier, orientation, gestes ;
- les regles d'accessibilite : contrastes, tailles tactiles, labels, ordre de focus, lisibilite.

`@pixelou` doit fournir a QA :

- les scenarios de validation fonctionnelle du parcours ;
- les cas limites visuels : texte long, aucune donnee, erreur API, lenteur reseau, permission refusee ;
- les points responsive : petit telephone, grand telephone, tablette si applicable ;
- les criteres visuels d'acceptation : alignements, espacements, coherence composants, absence de chevauchement, feedback clair.

## Role developpeur Android

Le developpeur Android concoit, implemente et corrige le code Kotlin/Compose, puis verifie que le projet compile proprement.

Ses livrables vers QA sont :

- le perimetre exact modifie ;
- les impacts fonctionnels attendus ;
- les points de risque connus ;
- les prerequis eventuels ;
- le statut de compilation, avec la commande executee et son resultat.

Commandes de compilation utiles :

```powershell
powershell -Command "& '.\gradlew.bat' clean assembleDebug"
powershell -Command "& '.\gradlew.bat' :app:assembleDebug"
```

Le developpeur Android ne porte pas les tests fonctionnels, la non-regression manuelle, ni la qualification finale. Si la compilation n'est pas verifiable localement, il doit le signaler explicitement avec la raison bloquante.

## Role QA

`@qa` intervient apres livraison dev et verifie que le besoin est pret a merger.

Responsabilites QA :

- comprendre le besoin fonctionnel et les impacts possibles ;
- identifier les parcours utilisateur critiques Android/Compose ;
- verifier les regressions probables sur les ecrans lies ;
- completer les tests si necessaire : unitaires, integration, UI, manuel ;
- donner un verdict clair : `OK merge`, `OK avec reserve`, ou `KO`.

Niveaux de tests :

- unitaires : ViewModel, mapping API/domain, logique d'etat, validations, cas d'erreur ;
- integration : appels repository/API mockes ou controles, DataStore, persistance locale, synchronisation d'etat ;
- UI Compose : affichage, interactions, etats loading/empty/error, navigation, permissions ;
- manuel : scenario utilisateur reel sur emulateur ou device si pertinent ;
- UX/accessibilite de base : lisibilite, boutons atteignables, feedback utilisateur, rotation si concernee, tailles d'ecran.

Rapport attendu avant merge :

- resume du perimetre teste ;
- scenarios executes avec resultat ;
- tests automatises ajoutes ou lances ;
- bugs trouves, severite, reproduction ;
- risques residuels ;
- verdict final.

Seul le verdict `OK merge` autorise le merge vers `main`. `OK avec reserve` doit etre traite comme un blocage jusqu'a arbitrage explicite de `@nexus`, correction ou transformation en `OK merge`.
Tant qu'un bug bloquant ou majeur reste ouvert, le verdict reste `KO`.

## Workflow Git obligatoire

Pour toute demande qui implique du developpement, une correction, un refactor, une documentation versionnee, un deploiement ou une modification de fichiers :

1. Partir de `main` a jour.
2. Si le worktree courant est sale, creer un `git worktree` separe depuis `main` avant toute modification.
3. Creer une branche Git dediee avant la premiere modification, avec un nom explicite : `feature/...`, `fix/...`, `chore/...`, `docs/...` ou `codex/...`.
4. Verifier `git status --short --branch` avant d'editer.
5. Stager uniquement les fichiers intentionnels avec des chemins explicites.
6. Executer les validations pertinentes avant commit.
7. Committer les changements avec un message explicite.
8. Pousser la branche.
9. Merger vers `main` uniquement si les validations sont OK et si QA donne `OK merge`.
10. Pousser `main` apres merge.

Ne pas committer `dist/`, `dist-admin/`, `node_modules/`, fichiers `.env*`, logs, secrets, `local.properties`, `.codex/config.toml`, APK, keystores, ni `POC/`.
Ne pas utiliser `git reset`, force-push, rebase ou suppression de branche sans demande explicite.
Ne jamais utiliser `git add .` si des fichiers sensibles ou non suivis existent.

## Hooks, skills et automatisation

Si une tache necessite ou beneficie d'un hook, d'un skill, d'un script reutilisable ou d'une automatisation :

- `@nexus` demande un cadrage a `@devops` et une revue a `@securite`.
- La solution doit rester proportionnee au besoin reel.
- Les hooks doivent etre versionnes dans `tools/git-hooks/`, pas modifies directement dans `.git/hooks`.
- L'installation des hooks doit viser le worktree courant, sans casser les worktrees freres.
- Les hooks ne doivent pas bloquer le developpement local sans message d'erreur clair.
- Les hooks ne doivent pas lire `.env*`, `local.properties`, `.codex/config.toml`, cles SSH, tokens ou dossiers utilisateurs.
- Tout hook modifiant le working tree doit etre explicitement approuve.
- Les skills doivent documenter leur declencheur, leur perimetre et leurs limites.

Installation des hooks versionnes :

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\install-git-hooks.ps1
```

Le hook versionne fourni est un `pre-commit` de garde-fous. Les builds et tests restent des validations explicites pilotees par les agents, pas des actions automatiques au push.

## Securite des skills externes

Ne jamais installer ou executer un skill externe sans revue prealable de son contenu.

Pour toute source externe, y compris `https://skills.sh/`, verifier avant installation :

- l'origine exacte du skill ;
- les fichiers modifies ou installes ;
- la presence de commandes reseau, shell, Git, SSH, token, exfiltration ou ecriture hors repo ;
- les hooks ou scripts automatiques ajoutes.

Interdire l'execution automatique de scripts d'installation non lus.
Ne pas installer de skill qui demande acces a des secrets, cles API, SSH, navigateur authentifie ou fichiers personnels sans validation explicite.
Preferer les skills versionnes, audites et limites au besoin immediat.

## Agents specialises

Utiliser les agents selon leur domaine :

- `@nexus` : orchestration, arbitrage, consolidation.
- `@product` : BA, cadrage du besoin, impacts, criteres d'acceptation.
- `@architecte` : architecture, decoupage technique, choix structurants.
- `@backy` : backend, API, base de donnees, logique serveur.
- `@fronty` : frontend React/Compose, integration UI, etats, appels API.
- `@devops` : scripts, build, Docker, CI/CD, deploiement, hooks.
- `@qa` : tests, validation fonctionnelle, scenarios de non-regression.
- `@reviewer` : revue de code, risques, regressions, dette technique.
- `@pixelou` : UI/UX, coherence visuelle, experience utilisateur.
- `@docs` : documentation, README, guides, notes techniques.
- `@securite` : secrets, permissions, surface d'attaque, dependances.

## Project Overview

**Max - AI Assistant** is a modern Android mobile application built with Kotlin and Jetpack Compose.

- **Project Name**: Max-AI-Assistant
- **Package**: `com.max.aiassistant`
- **Architecture**: Single-Activity with MVVM pattern
- **UI Framework**: 100% Jetpack Compose
- **Language**: Kotlin with StateFlow
- **Persistence**: DataStore for preferences and notes

See `CLAUDE.md` for detailed architecture notes and build commands.
