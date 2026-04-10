# Specification fonctionnelle consolidee - Nouvelle application Max

## 1. Objet du document

Ce document decrit la specification fonctionnelle consolidee de la nouvelle application Android "Max".

Il sert de reference pour :

- cadrer le perimetre produit cible
- organiser le travail en epics, user stories et lots d'implementation
- integrer les impacts UX et les contraintes techniques visibles a partir du POC actuel
- preparer les prochaines etapes de conception, developpement et validation

## 2. Contexte et ambition

Le POC actuel couvre deja un assistant personnel mobile avec :

- chat IA
- vocal
- taches
- planning
- meteo et allergies
- notes
- actualites

La nouvelle application doit conserver ce coeur produit tout en supprimant la dependance historique a `n8n` pour l'orchestration centrale.

L'objectif cible est une application Android autonome, avec :

- une inference locale basee sur Gemma 4
- une orchestration applicative locale
- une architecture plus robuste, maintenable et observable
- une UX clarifiee pour les parcours clefs

## 3. Sources consolidees

- Documentation POC : `C:\Users\cyril\projet\max_mobilev2\documentation\documentation-POC.md`
- Modele local fourni : `C:\Users\cyril\projet\max_mobilev2\model\gemma-4-E4B-it.litertlm`
- Modele web fourni : `C:\Users\cyril\projet\max_mobilev2\model\gemma-4-E4B-it-web.task`
- Note de packaging du modele : `C:\Users\cyril\projet\max_mobilev2\model\README.md`
- Contribution UX : [CYRAA-15](/CYRAA/issues/CYRAA-15)
- Contribution architecture Android locale : [CYRAA-16](/CYRAA/issues/CYRAA-16)
- Contribution QA : [CYRAA-17](/CYRAA/issues/CYRAA-17)
- Complement UX/UI de specification : [CYRAA-21](/CYRAA/issues/CYRAA-21)

## 4. Principes directeurs

- L'application doit pouvoir fonctionner sans backend `n8n`.
- Le coeur assistant doit privilegier le local-first.
- Les modules a forte valeur utilitaire doivent rester rapides et comprenables.
- Les parcours doivent etre explicites, coherents et robustes sur mobile.
- Les contraintes device, stockage, memoire et latence doivent etre traitees comme des exigences produit, pas comme des details techniques.

## 5. Points d'arbitrage identifies

### 5.1 Format du modele et runtime

Le fichier fourni est en format `.litertlm`, documente pour LiteRT-LM, alors que le brief mentionne une integration via `llama-cpp`.

Implication :

- un arbitrage technique est necessaire tres tot
- soit le runtime cible devient LiteRT-LM / AI Edge compatible avec ce format
- soit un modele ou un artefact compatible `llama-cpp` doit etre fourni a la place

Tant que cet arbitrage n'est pas tranche, la specification ci-dessous doit etre lue comme un cadrage fonctionnel avec contrainte technique ouverte sur le runtime exact.

### 5.2 Niveau d'offline reel

Le coeur conversationnel local peut devenir offline, mais certains modules resteront potentiellement dependants du reseau :

- meteo
- actualites
- radar meteo
- eventuelles synchronisations externes

La cible produit doit donc etre "local-first" plutot que "100% offline sur tout le scope".

### 5.3 Portee du vocal local

Le POC s'appuyait sur OpenAI Realtime pour le vocal. Le maintien d'un mode voice-to-voice fluide en local impose de clarifier :

- la brique STT
- la brique TTS
- la latence acceptable
- la compatibilite device

Le vocal doit rester dans le perimetre cible, mais sa priorisation doit dependre de la faisabilite device.

## 6. Vision produit cible

### 6.1 Promesse produit

Max doit devenir un assistant personnel Android qui aide l'utilisateur a :

- converser avec une IA locale
- organiser ses taches et son temps
- consulter ses informations contextuelles utiles
- capturer rapidement ses pensees et ses actions

### 6.2 Resultat attendu cote utilisateur

L'utilisateur doit percevoir :

- une application plus autonome
- une meilleure maitrise de ses donnees
- moins de dependances invisibles
- des parcours plus clairs et plus stables

## 7. Utilisateurs et besoins

### 7.1 Utilisateur principal

Personne qui utilise son telephone comme centre d'organisation personnelle et souhaite un assistant contextuel disponible rapidement.

### 7.2 Besoins prioritaires

- poser une question ou demander une aide sans passer par un service externe opaque
- retrouver ses taches et son planning rapidement
- acceder a un resume utile depuis l'accueil
- obtenir des retours explicites quand l'IA locale charge, reflechit ou echoue
- capturer une note, une idee, une image ou une demande sans friction

## 8. Exigences UX consolidees

Les exigences UX ci-dessous sont a integrer dans toute la conception de la nouvelle application.

### 8.1 Navigation

- la navigation principale ne doit pas reposer uniquement sur un geste cache
- l'acces aux modules doit rester visible depuis l'accueil et/ou une navigation persistante
- le comportement de navigation vers le module Vocal doit etre coherent depuis tous les ecrans
- le radar meteo peut rester un sous-parcours de la meteo

### 8.2 Lisibilite et feedback

- chaque module doit exposer des etats vides, chargement, erreur et succes
- l'inference locale doit afficher un feedback clair quand le modele charge, warm-up, genere ou est indisponible
- les actions longues doivent etre accompagnees d'un statut utilisateur comprenable

### 8.3 Coherence inter-modules

- les conventions de composants et d'actions doivent etre homogenes
- l'utilisateur doit comprendre la difference entre Taches et Planning
- les actions semblables doivent produire les memes comportements quel que soit l'ecran

### 8.4 Permissions et frictions

- les permissions micro, camera et media doivent etre demandees au bon moment
- l'utilisateur doit comprendre pourquoi une permission est necessaire
- un refus de permission ne doit pas bloquer toute l'application

### 8.5 Exigences UX explicitement integrees depuis [CYRAA-15](/CYRAA/issues/CYRAA-15)

- prevoir un ecran d'initialisation qui expose la disponibilite du moteur local et l'etat des services encore distants
- garder l'accueil comme hub prioritaire avec synthese du jour et raccourcis stables
- standardiser un shell de navigation visible plutot qu'une navigation cachee par swipe
- preserver le partage Android entrant vers le chat comme parcours a forte valeur
- traiter la latence locale comme un probleme UX de premier plan avec warm-up, attente, annulation et reprise

### 8.6 Architecture d'interface Android-first

- retenir un shell compose d'une top app bar par ecran et d'une navigation persistante pour les destinations coeur
- limiter la navigation primaire a 3 a 5 destinations maximum ; les modules secondaires restent accessibles depuis l'accueil et des entrees explicites
- reserver les gestes caches a des accelerations, jamais a l'unique moyen d'acces a un module ou a une action critique
- exposer un point d'entree vocal visible et constant, sans casser le contexte de l'ecran courant
- standardiser les comportements systeme Android : bouton retour, up navigation, deep links, partage entrant et reprise apres interruption

### 8.7 Inventaire minimal des ecrans et contrats UX

- Initialisation moteur local : verifier compatibilite device, stockage, presence ou corruption du modele, proposer telechargement, reprise, reparation et mode degrade
- Accueil : afficher la synthese du jour, l'etat du moteur local, les raccourcis stables vers les modules coeur et les raccourcis contextuels prioritaires
- Chat : afficher historique, saisie, pieces jointes, statut du modele, annulation, retry, partage entrant pre-rempli et erreurs explicites
- Taches : distinguer clairement liste, detail, creation/edition, filtres, tri, priorite, sous-taches et etats vides actionnables
- Planning : distinguer vue semaine, vue mois et detail evenement avec une explication visible de la difference entre evenement calendrier et tache personnelle
- Notes : couvrir liste, creation, edition, checklist, suppression et restauration ou confirmation de suppression selon le pattern retenu
- Meteo et radar : garder le radar comme detail subordonne a la meteo, avec CTA clairs pour changer de ville, activer allergies et comprendre l'absence de donnees
- Actualites : separer clairement flux principal, recherche ou synthese, source externe, etat hors ligne et erreurs distantes
- Parametres et permissions : centraliser l'etat du modele local, les permissions accorde/refusees, le stockage utilise, la purge et les reglages reseau utiles

### 8.8 Composants, etats et variantes a documenter

- chaque composant critique doit documenter son anatomie, ses variantes, ses etats, ses regles de contenu et ses actions associees
- les composants minimum a specifier sont : app bars, navigation bar, cartes d'accueil, champs de saisie, listes, cellules de tache, cartes meteo, lecteur d'etat du modele, banners d'erreur et dialogues de permission
- la matrice d'etats transverses doit au minimum couvrir : default, loading, disabled, empty, success, error, offline, permission_blocked et incompatible_device
- chaque etat doit definir un message principal, une action primaire, une action secondaire eventuelle et le comportement de retour a l'etat nominal
- les CTA primaires doivent rester stables par ecran ; les actions destructives ou irreversibles doivent etre distinguees visuellement et textuellement

### 8.9 Accessibilite et ergonomie

- respecter les cibles tactiles minimales Android et eviter les zones actives trop proches sur les ecrans denses
- supporter l'augmentation de taille de police sans perte de lisibilite ni troncature critique sur les ecrans coeur
- fournir labels, roles, ordre de lecture et annonces d'etat compatibles avec TalkBack
- ne jamais encoder une information critique par la couleur seule ; chaque statut doit combiner texte, contraste et, si utile, icone
- rendre les etats de generation locale, telechargement du modele et permission refusee perceptibles par le texte et l'accessibilite systeme
- limiter les animations longues ou purement decoratives ; les transitions doivent surtout clarifier le changement de contexte

### 8.10 Formulaires, validation et microcopy

- utiliser un schema constant label, aide, erreur, succes pour les formulaires de taches, notes, recherche et reglages
- privilegier la validation inline des que le cout d'erreur utilisateur est faible ; reserver les dialogues de blocage aux cas irreversibles
- faire apparaitre les messages d'erreur dans le contexte du champ ou du module concerne, avec un libelle actionnable plutot qu'un code technique
- expliciter en langage simple la difference entre panne locale, panne reseau, permission manquante et appareil non compatible
- aligner la microcopy avec la promesse locale-first : expliquer ce qui reste sur l'appareil, ce qui requiert le reseau et ce qui est temporairement indisponible

### 8.11 Handoff design-dev-QA attendu

- pour chaque ecran prioritaire, produire un contrat de conception avec objectif, zones de contenu, actions primaires et secondaires, etats et cas limites
- pour chaque composant partage, documenter les variantes, tokens ou styles, comportement interactif, accessibilite et dependances techniques
- pour chaque parcours sensible, lister explicitement les permissions, prerequis device, points de telemetrie et regressions critiques a couvrir
- les handoffs doivent etre exploitables sans interpretation implicite par Android, QA et produit

## 9. Contraintes techniques consolidees

### 9.1 Contraintes IA locale

- taille modele fournie : environ 3,65 Go
- consommation memoire potentiellement elevee selon le backend choisi
- temps de premier token significatif sur CPU
- besoin probable d'acceleration materielle sur certains devices

### 9.2 Contraintes Android

- verifier stockage disponible avant activation du modele
- maitriser les temps de demarrage et de warm-up
- gerer les retours en arriere, rotation, reprise d'activite et interruption OS
- journaliser les echecs d'inference, latences et erreurs media

### 9.3 Contraintes produit

- les donnees utilisateur sensibles ne doivent pas quitter l'appareil sans intention explicite
- les modules historiquement branches sur `n8n` doivent etre redefinis avec une source locale ou une source explicite
- les modules dependants du reseau doivent degrader proprement en cas d'absence de connexion

### 9.4 Contraintes explicitement integrees depuis [CYRAA-16](/CYRAA/issues/CYRAA-16)

- encapsuler le runtime IA derriere une abstraction `AiInferenceEngine`
- separer un `LocalAiService` pour les sessions longues et le vocal
- migrer les donnees metier persistantes vers `Room` et reserver `DataStore` aux preferences simples
- ne pas embarquer le modele de 3,65 Go dans l'APK/AAB ; prevoir telechargement, checksum, reprise et purge
- traiter le choix runtime/modele comme prerequis du lot d'inference locale

## 10. Perimetre cible

### 10.1 Inclus

- accueil et synthese personnelle
- assistant conversationnel local texte
- gestion de taches
- planning
- notes
- meteo et allergies
- radar meteo
- actualites
- partage Android vers le chat
- piece jointe image dans le chat
- vocal, sous reserve de faisabilite et d'un lot dedie

### 10.2 Hors perimetre immediate

- compte utilisateur complexe
- synchronisation cloud proprietaire generique
- administration backend `n8n`
- duplication de tous les comportements historiques sans reevaluation produit

## 11. Macro-decoupage en releases

### Release 1 - Socle local et modules coeur

- architecture applicative cible
- socle IA locale texte
- ecran d'initialisation du moteur local
- accueil
- chat texte
- taches
- planning
- notes
- etats UX transverses

### Release 2 - Modules contextuels et robustesse

- meteo
- allergies
- radar
- actualites
- cache et gestion degradee reseau
- instrumentation et observabilite

### Release 3 - Multimodal et optimisation device

- image dans le chat
- partage Android enrichi
- vocal local ou hybride valide
- optimisation performance et compatibilite terminaux

## 12. Epics, user stories et criteres d'acceptation

## Epic 1 - Socle produit et architecture locale

### Objectif

Fournir la base Android de la nouvelle application autonome.

### User stories

- En tant qu'utilisateur, je veux que l'application demarre sans dependre d'un orchestrateur `n8n`.
- En tant qu'utilisateur, je veux savoir si mon appareil est compatible avec l'IA locale avant d'utiliser le module assistant.
- En tant qu'equipe produit, nous voulons une architecture modulaire qui separe UI, logique metier, donnees locales et inference.

### Criteres d'acceptation

- l'application demarre sans appel obligatoire a `n8n`
- un ecran ou mecanisme de pre-verification indique compatibilite, stockage disponible et etat du modele
- les services applicatifs sont decoupes par domaine fonctionnel
- la specification technique tranche ou documente explicitement l'arbitrage LiteRT-LM versus `llama-cpp`

### Taches d'implementation

- definir l'architecture cible Android
- definir le runtime d'inference retenu
- introduire l'abstraction `AiInferenceEngine`
- definir un `LocalAiService` pour les usages longs et vocaux
- definir le packaging du modele
- definir la politique de warm-up et de release memoire
- definir la journalisation des erreurs et performances

## Epic 2 - Assistant conversationnel local texte

### Objectif

Permettre a l'utilisateur d'echanger en texte avec Max via une inference locale.

### User stories

- En tant qu'utilisateur, je veux poser une question en texte et obtenir une reponse locale.
- En tant qu'utilisateur, je veux voir quand le modele charge ou genere une reponse.
- En tant qu'utilisateur, je veux comprendre quand la reponse est indisponible ou trop lente.

### Criteres d'acceptation

- le chat texte fonctionne sans `n8n`
- l'utilisateur voit les etats `pret`, `chargement`, `generation`, `erreur`
- le message utilisateur est conserve dans l'historique local
- l'application supporte une sortie partielle ou progressive si le runtime le permet
- un timeout et un message de degradation existent en cas d'echec

### Taches d'implementation

- definir le format de prompt systeme et utilisateur
- definir la gestion de contexte local
- definir la persistance de l'historique
- definir les mecanismes de retry et d'annulation
- definir les seuils UX de latence
- supporter le streaming token par token si le runtime choisi le permet

## Epic 3 - Accueil et synthese personnelle

### Objectif

Faire de l'accueil un hub utile, stable et actionnable.

### User stories

- En tant qu'utilisateur, je veux voir mes informations prioritaires du jour en arrivant.
- En tant qu'utilisateur, je veux acceder rapidement aux modules principaux.
- En tant qu'utilisateur, je veux que chaque carte de l'accueil ait une action explicite et coherente.

### Criteres d'acceptation

- l'accueil affiche au minimum resume du jour, meteo et raccourcis modules
- les raccourcis ouvrent toujours le bon module
- la carte meteo de l'accueil expose un comportement explicite et coherent avec l'ecran Meteo
- les etats vides ou indisponibles sont visibles si aucune donnee n'existe

### Taches d'implementation

- redefinir la structure du dashboard
- formaliser les KPI et donnees minimales du resume du jour
- aligner les CTA d'accueil avec les modules cibles
- integrer l'etat de disponibilite du moteur local dans l'experience d'accueil ou d'initialisation

## Epic 4 - Navigation, shell et UX transverse

### Objectif

Rendre la navigation visible, coherente et robuste.

### User stories

- En tant qu'utilisateur, je veux comprendre comment acceder aux modules sans geste cache obligatoire.
- En tant qu'utilisateur, je veux une navigation coherente quel que soit l'ecran d'origine.
- En tant qu'utilisateur, je veux des retours clairs quand un parcours echoue.

### Criteres d'acceptation

- une navigation principale visible existe
- les routes vers Chat, Vocal, Taches, Planning, Notes, Meteo et Actualites sont consistantes
- les ecrans principaux ont des comportements de retour standardises
- chaque module possede des etats vide, chargement et erreur

### Taches d'implementation

- choisir le shell de navigation cible
- documenter les routes et sous-routes
- definir les regles de retour, fermeture et reprise
- definir les composants d'etat transverses
- expliciter le parcours d'entree systeme via partage Android vers le chat

## Epic 5 - Taches

### Objectif

Conserver et fiabiliser la gestion personnelle des taches.

### User stories

- En tant qu'utilisateur, je veux creer, modifier, filtrer et trier mes taches.
- En tant qu'utilisateur, je veux suivre des sous-taches et l'avancement.
- En tant qu'utilisateur, je veux comprendre clairement le statut et l'urgence de mes taches.

### Criteres d'acceptation

- creation, edition, suppression et consultation fonctionnent
- filtres et tris principaux sont disponibles
- les priorites `P1` a `P5` sont conservees ou remappees explicitement
- les sous-taches sont gerables
- la distinction entre description et note est tranchee dans le modele final

### Taches d'implementation

- definir la source de verite des taches
- definir le modele de donnees local
- clarifier le devenir du champ `note`
- definir la synchronisation future eventuelle sans la rendre bloquante
- prevoir des scenarios QA de creation, edition, completion et suppression sans corruption d'etat

## Epic 6 - Planning

### Objectif

Permettre la consultation claire du planning personnel.

### User stories

- En tant qu'utilisateur, je veux consulter mes evenements en semaine et en mois.
- En tant qu'utilisateur, je veux comprendre la difference entre mes taches et mon planning.
- En tant qu'utilisateur, je veux voir le detail utile d'un evenement.

### Criteres d'acceptation

- les vues semaine et mois existent
- un detail evenement affiche au minimum titre, horaires, lieu et description si disponible
- la relation fonctionnelle entre Taches et Planning est explicitee dans l'UX
- la source calendrier choisie est documentee

### Taches d'implementation

- definir la source calendrier locale ou externe
- definir les permissions et connecteurs necessaires
- definir les regles de mapping evenement vers UI
- prevoir la gestion d'absence d'evenements et de donnees invalides

## Epic 7 - Notes

### Objectif

Permettre une capture simple et rapide d'information personnelle.

### User stories

- En tant qu'utilisateur, je veux creer une note texte ou checklist.
- En tant qu'utilisateur, je veux modifier ou supprimer une note facilement.
- En tant qu'utilisateur, je veux retrouver mes notes meme hors connexion.

### Criteres d'acceptation

- les notes texte et checklist sont supportees
- la persistance locale est fiable
- les operations de base sont disponibles
- les etats vides et confirmation de suppression sont geres

### Taches d'implementation

- choisir la persistence cible
- definir la structure de note et checklist
- definir la preparation d'une synchronisation future sans l'imposer au MVP
- garantir la persistence apres fermeture et redemarrage

## Epic 8 - Meteo, allergies et radar

### Objectif

Fournir un module contextuel utile avec parcours simple.

### User stories

- En tant qu'utilisateur, je veux voir la meteo de ma ville courante.
- En tant qu'utilisateur, je veux changer de ville et activer ou non le bloc allergies.
- En tant qu'utilisateur, je veux ouvrir le radar comme detail de la meteo.

### Criteres d'acceptation

- la meteo actuelle et les vues heure par heure / jour par jour sont disponibles
- les allergies sont affichables si l'utilisateur les active
- le radar reste accessible comme detail rattache a la meteo
- l'absence de reseau est geree proprement avec message explicite

### Taches d'implementation

- definir les API reseau retenues
- definir le cache minimal et la frequence de refresh
- definir l'UX de degradation hors ligne
- integrer les cas ville inconnue, perte reseau et erreur radar

## Epic 9 - Actualites

### Objectif

Conserver un module de veille tout en le rendant explicite sur sa dependance reseau.

### User stories

- En tant qu'utilisateur, je veux consulter l'actualite du jour.
- En tant qu'utilisateur, je veux lancer une recherche IA ou thematique.
- En tant qu'utilisateur, je veux savoir si les contenus necessitent une connexion.

### Criteres d'acceptation

- les deux vues principales sont conservees ou remplacees explicitement
- le lien vers la source externe est visible
- les etats hors ligne ou erreur reseau sont comprehensibles
- le mode de generation ou selection des contenus est documente

### Taches d'implementation

- definir les fournisseurs ou flux de contenus
- definir ce qui est genere localement versus recupere via API
- definir la politique de cache et freshness
- definir les etats succes, vide et erreur distante pour chaque vue

## Epic 10 - Multimodal image et partage Android

### Objectif

Conserver les parcours d'entree riches du POC.

### User stories

- En tant qu'utilisateur, je veux partager du texte depuis une autre application vers Max.
- En tant qu'utilisateur, je veux joindre une image depuis la galerie ou la camera dans une conversation.
- En tant qu'utilisateur, je veux comprendre pourquoi l'application demande l'acces camera ou media.

### Criteres d'acceptation

- le partage `text/plain` ouvre le bon parcours de chat
- la jointure image est disponible depuis galerie et camera si permission accordee
- un refus de permission produit un fallback comprehensible
- les pieces jointes n'entrainent pas de blocage silencieux

### Taches d'implementation

- definir le support exact image dans le pipeline IA local
- definir le stockage temporaire et la purge
- definir les contrats Android d'intent et `FileProvider`
- couvrir les cas permission refusee, URI invalide et reprise utilisateur

## Epic 11 - Vocal

### Objectif

Reintroduire une experience vocale seulement si la faisabilite produit et technique est validee.

### User stories

- En tant qu'utilisateur, je veux parler a Max avec un feedback clair.
- En tant qu'utilisateur, je veux savoir si le vocal est indisponible sur mon appareil.
- En tant qu'equipe produit, nous voulons eviter une experience vocale degradee ou trompeuse.

### Criteres d'acceptation

- le produit documente clairement si le vocal est inclus au MVP ou en phase suivante
- si le vocal est livre, la permission micro est geree proprement
- l'utilisateur voit les etats d'ecoute, traitement, reponse et erreur
- la latence cible et les preconditions device sont formalisees

### Taches d'implementation

- choisir STT, TTS et orchestration
- mesurer latence device cible
- definir le mode fallback si le device n'est pas compatible
- prevoir les cas interruption systeme, casque et reprise de session

## Epic 12 - Donnees locales, confidentialite et observabilite

### Objectif

Garantir un comportement fiable, verifiable et compatible avec une promesse locale.

### User stories

- En tant qu'utilisateur, je veux savoir quelles donnees restent locales.
- En tant qu'equipe produit, nous voulons pouvoir diagnostiquer les echecs terrain.
- En tant qu'equipe produit, nous voulons une politique claire de stockage local, purge et reprise.

### Criteres d'acceptation

- les donnees locales et donnees reseau sont documentees par module
- les erreurs critiques sont journalisees
- les temps de chargement et echecs d'inference sont mesurables
- la politique de retention locale est documentee

### Taches d'implementation

- definir telemetrie locale et remontable
- definir politique de logs
- definir retention des historiques et caches

## 13. Lots techniques recommandes

### Lot A - Faisabilite IA locale

- arbitrer LiteRT-LM versus `llama-cpp`
- mesurer compatibilite devices cibles
- valider strategie de chargement du modele
- definir le budget stockage et memoire

### Lot B - Socle Android cible

- navigation
- architecture modules
- persistence locale
- instrumentation

### Lot C - Coeur assistant et organisation personnelle

- chat texte
- accueil
- taches
- planning
- notes

### Lot D - Modules contextuels

- meteo
- allergies
- radar
- actualites

### Lot E - Multimodal

- partage Android
- image
- vocal

## 14. Criteres d'acceptation transverses du produit

- aucun parcours coeur ne depend d'un orchestrateur `n8n`
- l'utilisateur comprend quand l'IA locale est disponible, indisponible ou lente
- les modules principaux sont accessibles via une navigation explicite
- le shell de navigation et les comportements de retour sont standardises sur les ecrans coeur
- chaque module gere les etats vide, chargement et erreur
- chaque ecran prioritaire documente au minimum ses CTA, ses variantes et sa matrice d'etats
- les permissions Android sont demandees juste a temps
- les modules reseau se degradent proprement hors connexion
- les exigences d'accessibilite mobile sont prises en compte des le MVP sur les parcours coeur
- les choix techniques structurants encore ouverts sont identifies noir sur blanc

## 15. Validation QA transverse integree depuis [CYRAA-17](/CYRAA/issues/CYRAA-17)

### Axes de validation obligatoires

- bootstrap local IA : presence modele, chargement initial, stockage insuffisant, corruption, reprise
- permissions Android : micro, camera, media, refus initial, refus permanent, retour via reglages
- parcours coeur : Accueil, Chat, Vocal, Taches, Planning, Meteo, Radar, Notes, Actualites
- mode degrade : indisponibilite IA locale, lenteur, erreur reseau, retry et absence de blocage global
- persistence locale : preferences, notes, historiques utiles, pieces jointes temporaires
- performance et stabilite : fluidite, memoire, retour premier plan, longue session
- accessibilite : lecture TalkBack, contraste, focus, taille de police augmentee, cibles tactiles et feedback d'etat
- coherence de composants : CTA, messages d'erreur, navigation, comportements de retour et etats transverses

### Scenarios critiques a exiger

- premier lancement sans permissions accordees
- premier lancement avec modele absent, corrompu ou inaccessible
- premier lancement sur appareil incompatible ou stockage insuffisant
- chat local avec reponse longue puis retry
- vocal avec permission micro refusee puis acceptee
- partage Android textuel avec pre-remplissage correct
- ajout d'image depuis galerie et camera
- gestion complete des taches et sous-taches
- planning vide ou avec donnees invalides
- meteo avec ville inconnue et perte reseau
- notes persistantes apres redemarrage
- actualites en succes, vide et erreur distante
- verification des parcours coeur avec police systeme agrandie et lecteur d'ecran actif

## 16. Risques majeurs

- incompatibilite entre le format de modele fourni et le runtime mentionne
- empreinte stockage et memoire trop elevee pour une partie du parc Android
- vocal local trop couteux en latence ou en consommation
- ambiguite persistante entre perimetre local et perimetre reseau
- experience degradee si la navigation cachee du POC est reproduite sans correction

## 17. Decisions a prendre rapidement

- choisir le runtime reel d'inference
- choisir les appareils minimum supportes
- confirmer si le vocal est MVP ou phase 2
- definir la source de verite des taches et du planning
- definir la politique reseau de la meteo, des actualites et du radar

## 18. Suite attendue

- la specification presente integre deja les apports de [CYRAA-15](/CYRAA/issues/CYRAA-15), [CYRAA-16](/CYRAA/issues/CYRAA-16), [CYRAA-17](/CYRAA/issues/CYRAA-17) et [CYRAA-21](/CYRAA/issues/CYRAA-21)
- arbitrer les decisions ouvertes avant decoupage fin en tickets de build
