# Cadrage UI/UX exploitable - MAX Android

## 1. Objet

Ce document transforme la specification fonctionnelle consolidee en cadrage UI/UX directement exploitable pour Android.

Il sert de handoff pour :

- l'implementation Android liee a [CYRAA-23](/CYRAA/issues/CYRAA-23)
- la validation UX et accessibilite
- la couverture QA des etats critiques

Sources utilisees :

- `C:\Users\cyril\projet\max_mobilev2\documentation\specification-nouvelle-application.md`
- `C:\Users\cyril\projet\max_mobilev2\documentation\documentation-POC.md`
- `C:\Users\cyril\projet\max_mobilev2\Max\app\src\main\java\com\max\aiassistant\MainActivity.kt`
- `C:\Users\cyril\.paperclip\instances\default\companies\12465807-5e69-4172-ad36-bf3bbe33b815\codex-home\references\awesome-design-systems\CYRAA-mobile-guidance.md`

## 2. Cadre de reference

Principes retenus :

- Android-first via `Material Design` pour shell, navigation, comportements systeme, accessibilite et permissions
- `Apple Developer Design Guidelines` comme contrepoint qualite sur la clarte des parcours et la reduction de friction
- `Carbon` et `Primer` pour la structure des composants, des etats et du handoff design-dev-QA

Decision de posture :

- l'application cible est mobile phone-first
- le shell doit etre natif, visible et explicite
- la latence locale doit etre traitee comme une partie de l'interface, pas comme un detail technique
- les modules reseau doivent annoncer clairement leur dependance a la connexion

## 3. Decisions UX structurantes

### 3.1 Shell de navigation cible

Le POC actuel repose sur un pager interne et une sidebar laterale cachee. Cette approche ne doit pas etre reconduite pour la cible Android.

Decision :

- utiliser une `Top App Bar` par destination
- utiliser une `Navigation Bar` persistante a 4 destinations coeur
- reserver les modules secondaires a l'accueil, aux CTA contextuels et a des routes explicites
- conserver le comportement Android standard de retour et de reprise

Destinations coeur retenues :

1. `Accueil`
2. `Chat`
3. `Taches`
4. `Planning`

Modules secondaires :

- `Notes`
- `Meteo`
- `Actualites`
- `Parametres`

Regle :

- `Vocal` reste un point d'entree visible global, mais n'entre pas dans les 4 destinations primaires
- sur telephone, l'entree vocale est portee par un bouton persistant visible depuis le shell
- sur grand ecran Android, une `Navigation Rail` pourra remplacer la bottom bar plus tard, sans changer l'architecture des destinations

### 3.2 Point d'entree vocal

Decision :

- le vocal s'ouvre comme destination plein ecran preservee dans le back stack
- l'utilisateur revient a l'ecran d'origine via retour systeme ou CTA de fermeture
- l'etat du vocal peut etre `pret`, `indisponible sur cet appareil`, `permission micro requise`, `reseau requis`, `en session`

Raison :

- cela garde un acces visible et constant sans casser le contexte courant
- cela permet de feature-gater le vocal sans casser le shell si la faisabilite locale n'est pas tranchee

### 3.3 Priorisation release UI

`P0` interface :

- shell de navigation
- initialisation moteur local
- accueil
- chat texte
- taches
- planning
- notes
- parametres / permissions / stockage

`P1` interface :

- meteo
- radar
- actualites
- partage Android vers le chat
- piece jointe image dans le chat

`P2` interface :

- vocal complet
- optimisations device et etats de compatibilite fines

## 4. Architecture des parcours

### 4.1 Arborescence cible

```text
Init moteur local
Accueil
Chat
Taches
Planning
Notes
Meteo
  Radar
Actualites
Parametres
Vocal
```

### 4.2 Regles de navigation

- `Accueil`, `Chat`, `Taches`, `Planning` sont accessibles en un tap depuis la navigation persistante
- `Notes`, `Meteo`, `Actualites` sont accessibles depuis l'accueil et par CTA explicites internes
- `Radar` n'est jamais expose comme destination primaire
- `Parametres` est accessible depuis l'accueil, l'app bar ou l'etat moteur local
- un partage Android entrant ouvre directement `Chat` avec contenu pre-rempli
- le bouton retour Android revient a l'etape precedente reelle, jamais a un ecran arbitraire du pager

### 4.3 Regles de shell

- chaque ecran expose un titre clair
- chaque ecran expose au plus une action primaire persistante
- les actions destructives ou irreversibles passent par confirmation ou undo selon le cout de l'erreur
- les badges et statuts critiques ne reposent pas sur la couleur seule

## 5. Contrats d'ecrans

## 5.1 Initialisation moteur local

Objectif :
Verifier que l'appareil peut utiliser le moteur local et rendre le statut comprehensible avant l'entree dans les experiences IA.

Zones de contenu :

- resume compatibilite appareil
- stockage disponible et taille attendue
- etat du modele : absent, telechargement, verification, pret, corrompu
- etat des services encore distants
- actions de reprise, reparation, purge

Actions primaires :

- `Telecharger le modele`
- `Reprendre`
- `Reparer`
- `Continuer en mode degrade`

Etats obligatoires :

- `loading`
- `incompatible_device`
- `offline`
- `error`
- `success`

Cas limites :

- stockage insuffisant
- checksum invalide
- reprise apres interruption OS
- modele present mais runtime incompatible

Decision UI :

- l'ecran d'initialisation est obligatoire au premier lancement et re-accessible depuis `Parametres`
- si le modele n'est pas pret, `Chat` et `Vocal` affichent un etat bloque mais non cassant avec CTA vers cet ecran

## 5.2 Accueil

Objectif :
Servir de hub stable avec synthese du jour, etat moteur local et raccourcis forts.

Zones de contenu :

- carte d'etat du moteur local
- synthese du jour
- cartes de raccourcis vers modules
- resume meteo compact
- rappels contextuels prioritaires

Action primaire :

- ouvrir le module le plus probable selon le contexte utilisateur

Actions secondaires :

- ouvrir `Meteo`
- ouvrir `Notes`
- ouvrir `Actualites`
- ouvrir `Parametres`

Etats obligatoires :

- `default`
- `loading`
- `empty`
- `offline`
- `error`

Decision UI :

- l'accueil ne surcharge pas l'ecran avec 8 destinations equivalentes
- les cartes sont ordonnees par utilite du jour, pas par historique technique du POC
- l'etat moteur local reste visible en haut de l'accueil

## 5.3 Chat

Objectif :
Permettre une conversation texte locale, comprehensible et tolérante a la latence.

Zones de contenu :

- top app bar avec titre, statut du moteur et acces parametres
- historique des messages
- zone de statut de generation
- composer texte
- piece jointe image
- CTA d'envoi

Action primaire :

- `Envoyer`

Actions secondaires :

- `Joindre une image`
- `Annuler la generation`
- `Reessayer`
- `Ouvrir le vocal`

Etats obligatoires :

- `empty`
- `loading`
- `warming_up`
- `generating`
- `error`
- `offline`
- `permission_blocked` pour camera / media si piece jointe
- `incompatible_device`

Cas limites :

- texte partage entrant pre-rempli
- image capturee puis permission refusee
- generation longue avec annulation
- retour d'erreur runtime local
- historique vide mais modele pret

Decision UI :

- le statut moteur n'est jamais cache dans un coin silencieux
- le composer reste utilisable meme si la reponse precedente a echoue
- l'UI doit supporter des reponses progressives si le runtime le permet

## 5.4 Vocal

Objectif :
Proposer une entree vocale visible sans devenir un point de blocage structurel de l'application.

Zones de contenu :

- statut de disponibilite
- visualisation de session
- transcript en cours
- CTA principal micro / arret
- aide sur permission ou prerequis

Action primaire :

- `Demarrer la session`

Actions secondaires :

- `Accorder le micro`
- `Basculer vers le chat`
- `Fermer`

Etats obligatoires :

- `permission_blocked`
- `loading`
- `success`
- `error`
- `incompatible_device`

Decision UI :

- si le vocal n'est pas livrable en local sur un device donne, l'entree reste visible mais annonce clairement l'indisponibilite
- le transcript doit rester lisible et copiable

## 5.5 Taches

Objectif :
Clarifier la gestion personnelle des taches et la differencier du planning.

Sous-ecrans :

- liste
- detail
- creation / edition
- filtres et tri

Zones de contenu liste :

- recherche ou filtrage
- resume de filtre actif
- liste de taches
- CTA de creation

Actions primaires :

- `Creer une tache`
- `Enregistrer`

Actions secondaires :

- `Filtrer`
- `Trier`
- `Completer`
- `Dupliquer`
- `Supprimer`

Etats obligatoires :

- `empty`
- `loading`
- `error`
- `offline`
- `success`

Regle de comprehension :

- une tache est une action personnelle a faire
- une tache peut avoir une priorite, une echeance et des sous-taches
- la tache reste distincte d'un evenement de calendrier

Decision UI :

- la creation / edition passe par un ecran ou une sheet pleine hauteur, pas par une fenetre dense difficile a lire
- les sous-taches ont une edition inline simple et des cibles tactiles larges

## 5.6 Planning

Objectif :
Rendre la lecture des evenements rapide et differencier clairement calendrier et taches.

Sous-ecrans :

- vue semaine
- vue mois
- detail evenement

Actions primaires :

- `Changer de vue`
- `Ouvrir un evenement`

Actions secondaires :

- `Aller a aujourd'hui`
- `Semaine precedente / suivante`
- `Mois precedent / suivant`

Etats obligatoires :

- `loading`
- `empty`
- `error`
- `offline`

Decision UI :

- l'ecran doit rappeler explicitement qu'un evenement est un element de calendrier date et horaire
- les taches a echeance proche peuvent etre suggerees, mais jamais melangees comme s'il s'agissait du meme objet

## 5.7 Notes

Objectif :
Capturer rapidement des informations sans friction et les retrouver ensuite.

Sous-ecrans :

- liste
- editeur

Actions primaires :

- `Nouvelle note`
- `Enregistrer`

Actions secondaires :

- `Transformer en checklist`
- `Supprimer`
- `Restaurer`

Etats obligatoires :

- `empty`
- `loading`
- `error`
- `success`

Decision UI :

- suppression via undo snack bar si action reversible
- confirmation explicite si destruction definitive

## 5.8 Meteo et radar

Objectif :
Afficher une information contextuelle utile sans laisser croire a un module offline.

Sous-ecrans :

- meteo principale
- radar

Actions primaires :

- `Changer de ville`
- `Voir le radar`

Actions secondaires :

- `Activer les allergies`
- `Actualiser`

Etats obligatoires :

- `loading`
- `empty`
- `error`
- `offline`
- `permission_blocked` si la localisation est introduite plus tard

Decision UI :

- le radar reste une profondeur du module meteo
- l'absence de donnees reseau doit etre decrite comme une panne de service distante, pas comme une panne generale de Max

## 5.9 Actualites

Objectif :
Presenter un flux utile et comprehensible comme service reseau secondaire.

Zones de contenu :

- flux principal
- recherche ou synthese
- etat de fraicheur des donnees
- source ou provenance externe

Actions primaires :

- `Ouvrir l'article`
- `Actualiser`

Actions secondaires :

- `Rechercher`
- `Voir la synthese`

Etats obligatoires :

- `loading`
- `empty`
- `error`
- `offline`

Decision UI :

- toujours separer clairement contenu local et contenu distant
- afficher un libelle de derniere mise a jour si le cache est conserve

## 5.10 Parametres et permissions

Objectif :
Donner un point central pour comprendre l'etat technique visible du produit.

Zones de contenu :

- etat du moteur local
- permissions accordees / refusees
- stockage utilise
- actions de purge / reparation
- preferences reseau ou contenu

Actions primaires :

- `Gerer le modele`
- `Ouvrir les reglages systeme`
- `Purger`

Etats obligatoires :

- `default`
- `loading`
- `error`
- `permission_blocked`

Decision UI :

- cet ecran doit permettre a QA et support de verifier rapidement l'etat du produit sans lire les logs

## 6. Composants a specifier

## 6.1 Top App Bar

Anatomie :

- titre
- navigation icon si sous-route
- action contextuelle
- acces parametres ou statut

Variantes :

- destination primaire
- sous-route detail
- destination avec recherche

Etats :

- default
- scrolled
- loading

## 6.2 Navigation Bar

Items :

- accueil
- chat
- taches
- planning

Regles :

- label toujours visible
- icone + texte
- badge optionnel seulement si vrai gain informationnel

## 6.3 Carte d'etat moteur local

Anatomie :

- titre d'etat
- sous-texte explicatif
- icone de statut
- CTA primaire
- CTA secondaire optionnel

Variantes :

- pret
- telechargement
- warm-up
- indisponible
- incompatible

## 6.4 Composer de chat

Anatomie :

- champ multiline
- piece jointe visible
- CTA d'envoi
- CTA vocal optionnel

Etats :

- vide
- texte saisi
- image jointe
- generation en cours
- erreur precedente

## 6.5 Cellule de tache

Anatomie :

- statut
- titre
- meta ligne secondaire
- priorite
- progression sous-taches
- affordance d'action

Etats :

- default
- completed
- overdue
- selected
- disabled

## 6.6 Bloc d'etat transverse

Usage :

- empty
- error
- offline
- permission_blocked
- incompatible_device

Contenu minimum :

- titre
- explication courte
- action primaire
- action secondaire optionnelle

## 6.7 Dialogue / carte de permission

Regles :

- expliquer le pourquoi avant la demande systeme
- proposer une issue claire apres refus
- ne pas bloquer tout le produit pour une permission non critique

## 7. Matrice d'etats transverse

| Etat | Quand l'utiliser | Message attendu | Action primaire | Sortie attendue |
| --- | --- | --- | --- | --- |
| `default` | Donnees disponibles, parcours nominal | Etat courant clair | Action metier | Continuer le flux |
| `loading` | Chargement court ou initial | Ce qui charge et pourquoi | Patienter ou annuler si long | Arrivee donnees ou erreur |
| `disabled` | Action temporairement impossible | Cause immediate | Aucune ou action alternative | Re-evaluation de l'etat |
| `empty` | Aucune donnee utilisateur | Explication + prochain pas | Creer / ajouter | Premier contenu cree |
| `success` | Action terminee | Confirmation concise | Continuer | Retour a l'usage normal |
| `error` | Echec recuperable | Cause comprehensible | Reessayer | Reprise ou support |
| `offline` | Reseau requis absent | Fonction impactee | Reessayer / mode degrade | Retour connexion ou sortie |
| `permission_blocked` | Permission refusee / absente | Pourquoi elle est utile | Autoriser | Autorisation accordee ou degrader |
| `incompatible_device` | Appareil ou runtime non supporte | Limite device explicite | Continuer sans IA / support | Mode degrade ou sortie |

Regle :

- cette matrice s'applique a tous les modules
- chaque ecran doit exposer au minimum les etats qui lui sont pertinents
- QA doit couvrir ces etats comme scenarios de regression obligatoires

## 8. Permissions et frictions

Permissions a gerer en just-in-time :

- `Microphone` au premier acces vocal
- `Camera` lors de la capture d'image
- `Photos / media` lors de la selection d'image

Permissions a ne pas rendre bloquantes globalement :

- refus micro ne bloque pas chat texte
- refus camera ne bloque pas galerie ni texte
- indisponibilite reseau ne bloque pas taches, notes ou shell

Microcopy attendue :

- expliquer la fonction
- dire si la donnee reste sur l'appareil
- donner une alternative si l'utilisateur refuse

## 9. Accessibilite et ergonomie

Exigences minimales :

- cibles tactiles Android dimensionnees de maniere confortable
- support police agrandie sans collision critique des CTA
- ordre de lecture compatible TalkBack
- annonces vocales des changements d'etat critiques
- contraste suffisant sur les cartes et statuts
- aucune information critique uniquement par couleur

Motion :

- animations utiles pour clarifier les transitions
- pas d'animation longue decorative sur les etats de chargement
- sur inference locale, preferer des indicateurs d'avancement ou de phase a une simple boucle infinie abstraite

## 10. Regles de contenu et microcopy

La microcopy doit distinguer quatre causes :

1. `Le moteur local n'est pas pret`
2. `La connexion internet manque pour ce module`
3. `Une permission est necessaire`
4. `Cet appareil ne supporte pas cette fonction`

Regles :

- jamais de message purement technique si une cause orientee utilisateur peut etre formulee
- expliciter ce qui reste local et ce qui depend du reseau
- employer des CTA verbaux et precis

Exemples attendus :

- `Modele local non disponible. Telechargez-le pour discuter avec Max hors ligne.`
- `Actualites indisponibles hors connexion. Reessayez une fois le reseau retabli.`
- `Le micro est desactive. Autorisez-le pour parler a Max.`
- `Cet appareil ne prend pas en charge le moteur local complet. Continuez en mode degrade.`

## 11. Handoff design-dev-QA

Livrables attendus cote Android :

- routes et destinations alignees sur ce document
- composants partages centralises
- etats transverses reutilisables
- differentiations explicites entre local et reseau

Checklist Android :

- supprimer la logique de navigation cachee du POC au profit d'un shell explicite
- maintenir le partage Android entrant vers le chat
- garder le radar comme sous-route meteo
- exposer l'etat du moteur local dans l'accueil, le chat et les parametres

Checklist QA :

- verifier la matrice d'etats sur chaque module critique
- verifier les permissions refusees puis autorisees
- verifier retour systeme, reprise d'activite, rotation et reprise apres interruption
- verifier lisibilite en police agrandie et avec TalkBack

## 12. Ecarts cles entre POC et cible

- remplacer le `HorizontalPager` et la sidebar cachee par un shell Material explicite
- reduire la navigation primaire a 4 destinations
- sortir `Radar` du niveau primaire
- rendre visible l'etat du moteur local dans les experiences IA
- traiter `Vocal` comme capacite visible mais feature-gatee
- durcir la difference UX entre `Taches` et `Planning`

## 13. Arbitrages a trancher rapidement

Ces points n'empechent pas la production du cadrage UX, mais ils peuvent ralentir l'implementation si aucun proprietaire ne tranche vite.

### 13.1 Runtime IA local

Question :

- runtime cible `LiteRT-LM / AI Edge` ou autre artefact compatible `llama-cpp`

Impact UX :

- textes d'initialisation
- compatibilite device
- temps de warm-up
- capacites de streaming

### 13.2 Vocal local

Question :

- maintien d'un vocal local, hybride ou reporte

Impact UX :

- statut du point d'entree vocal
- permissions
- attentes de latence
- fallback vers chat

### 13.3 Frontiere local / reseau par module

Question :

- quel niveau de cache et de degrade pour `Meteo`, `Actualites`, `Radar`, `Allergies`

Impact UX :

- messages offline
- fraicheur des donnees
- priorite des CTA de reessai

### 13.4 Compatibilite device minimum

Question :

- quels seuils minimums de stockage, RAM et performance doivent etre exposes a l'utilisateur

Impact UX :

- libelles de compatibilite
- mode degrade
- support et diagnostics

## 14. Recommandation de lancement implementation

Ordre recommande :

1. shell + parametres + etat moteur local
2. chat texte + ecran d'initialisation
3. taches + planning + notes
4. accueil final branche sur les composants precedents
5. meteo / actualites / radar
6. vocal selon arbitrage technique

Ce sequence permet d'ancrer l'UX sur les parcours coeur tout en laissant les modules les plus incertains dans des entrees explicites et non bloquantes.
