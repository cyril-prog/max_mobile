# Workflow agentique Max Mobile

## Objectif

Ce document detaille le flux standard de production pilote par `@nexus` pour les evolutions, corrections, hooks, skills et livraisons du projet Max Mobile.

La version courte et normative se trouve dans `AGENTS.md`. En cas de divergence, `AGENTS.md` prime.

## Flux standard

`Utilisateur -> Agent parent -> @nexus -> Product/BA -> UI/UX si besoin -> Agent specialise -> QA -> Reviewer si besoin -> @nexus -> merge`

## Gates obligatoires

1. Cadrage Product/BA avant developpement pour toute evolution ou correction.
2. Passage UI/UX avant developpement si l'IHM, un parcours ou un wording utilisateur est impacte.
3. Branche dediee depuis `main` avant modification.
4. Compilation validee par le developpeur avant passage QA.
5. QA avant merge, avec verdict explicite.
6. Merge vers `main` uniquement si les validations sont OK et si QA donne `OK merge`.

## Mapping des demandes vers agents

| Type de demande | Agents obligatoires | Agents conditionnels | Validation attendue |
| --- | --- | --- | --- |
| Evolution fonctionnelle | `@nexus`, `@product`, dev specialise, `@qa` | `@pixelou`, `@architecte`, `@reviewer` | Compile dev + tests QA |
| Correction bug | `@nexus`, `@product`, dev specialise, `@qa` | `@pixelou` si UI, `@reviewer` si risque | Reproduction couverte + non-regression |
| Modification UI/IHM | `@nexus`, `@product`, `@pixelou`, `@fronty`, `@qa` | `@reviewer` | Etats visuels et parcours valides |
| Backend/API/donnees | `@nexus`, `@product`, `@backy`, `@qa` | `@architecte`, `@securite` | Contrats, erreurs et regressions valides |
| Build, CI/CD, hooks | `@nexus`, `@devops`, `@securite`, `@qa` | `@reviewer` | Scripts lisibles, non destructifs, validations OK |
| Skill externe | `@nexus`, `@securite`, `@devops` | `@docs`, specialiste metier | Revue contenu avant installation |
| Documentation versionnee | `@nexus`, `@docs`, `@qa` | `@reviewer` | Coherence avec `AGENTS.md` et projet |

## Product/BA

Le Product/BA transforme la demande utilisateur en besoin testable. Il clarifie le perimetre, les impacts, les cas limites et les criteres d'acceptation.

Le developpement ne commence pas si le BA signale une ambiguite bloquante.

## UI/UX

`@pixelou` intervient quand la demande touche :

- un ecran Compose, composant, navigation ou transition ;
- un wording visible, une hierarchie d'information ou un parcours ;
- un etat de chargement, vide, erreur, succes ou permission ;
- une adaptation mobile, tablette, clavier, orientation ou accessibilite.

Son livrable doit etre exploitable par le developpeur et par QA.

## Developpeur

Le developpeur implemente le perimetre cadre, respecte les contraintes du projet, puis verifie la compilation.

Il transmet a QA :

- le perimetre modifie ;
- les impacts attendus ;
- les risques connus ;
- les prerequis ;
- la commande de compilation executee et son resultat.

Le developpeur ne porte pas le verdict fonctionnel final.

## QA

QA verifie que le besoin est couvert de bout en bout, complete les tests si necessaire, puis donne un verdict :

- `OK merge` : merge autorise ;
- `OK avec reserve` : merge bloque jusqu'a arbitrage explicite de `@nexus`, correction ou transformation en `OK merge` ;
- `KO` : merge interdit.

Le rapport QA liste le perimetre teste, les scenarios executes, les tests ajoutes ou lances, les bugs, les risques residuels et le verdict.

## Hooks

Les hooks projet sont versionnes dans `tools/git-hooks/`. Ils doivent etre installes pour le worktree courant uniquement.

Ils s'installent avec :

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\install-git-hooks.ps1
```

Ils ne remplacent pas la responsabilite de l'agent : staging explicite, revue des diffs et validation avant merge restent obligatoires.
Le hook fourni est un `pre-commit` de garde-fous ; il ne lance pas de build automatique au push.

## Skills externes

Les skills externes, y compris ceux trouves sur `https://skills.sh/`, ne doivent pas etre installes sans revue `@securite`.

La revue doit verifier l'origine, les fichiers installes, les scripts executes, les acces reseau/fichiers et les risques de secret ou d'exfiltration.
