# Plan d'implémentation : Logs Applicatifs en temps réel (tlog)

## Objectif
Permettre de visualiser en temps réel les logs de l'application (ex: via la commande distante `tlog`) pour un hôte spécifique. L'interface utilisera un affichage divisé verticalement (split screen) sur la vue détaillée de l'hôte, et le flux sera géré via SSE indépendamment du cycle de vie des déploiements.

## 0. Préparation (Workspace)
- **Sauvegarde** : Créer une sauvegarde de `doc/progress.md` (par exemple `doc/progress_backup.md`).
- **Mise à jour du suivi** : Mettre à jour `doc/progress.md` pour inclure ce nouveau plan et suivre son avancement.
- **Stockage du plan** : Copier ce fichier de plan dans le dossier `plan/` du projet (ex: `plan/plan_tlog_feature.md`).

## 1. Backend : Base de données
Fichier : `back/src/main/resources/db/migration/V9__add_tlog_command.sql` (nouveau fichier)
- **Objectif** :
  - Insérer la configuration par défaut : `INSERT INTO app_config (key, value) VALUES ('default_tlog_command', 'ssh root@{domain} tlog');`
  - Ajouter la colonne personnalisable pour l'hôte : `ALTER TABLE hosts ADD COLUMN tlog_command TEXT;`

## 2. Backend : Entités et DTOs
- **`Host.java`** : Ajouter l'attribut `String tlogCommand`.
- **`HostRequest.java`** & **`HostResponse.java`** : Ajouter le champ `tlogCommand`.
- **`HostWithStatusResponse.java`** : Ajouter le champ `tlogCommand`.

## 3. Backend : Service et Streaming
- **`HostService.java`** (ou création d'un service dédié) :
  - Ajouter une méthode `streamTlog(UUID hostId, User user)` qui renvoie un `SseEmitter`.
  - La méthode vérifiera les droits de l'utilisateur sur l'hôte.
  - Elle récupèrera la commande (priorité: `host.getTlogCommand()`, fallback: `appConfig.get("default_tlog_command")`).
  - Elle remplacera les variables (`{host}`, `{ip}`, `{domain}`).
  - Elle lancera le processus (`ProcessBuilder` via shell) et lira la sortie (stdout/stderr).
  - Elle enverra les lignes via `SseEmitter` et s'assurera de tuer le processus (`process.destroyForcibly()`) lorsque le client se déconnecte, qu'une erreur survient ou que l'emitter atteint son timeout.

## 4. Backend : Contrôleur
Fichier : `back/src/main/java/fr/arthurbr02/deploymanager/controller/HostController.java`
- **Ajout** : Endpoint `GET /hosts/{id}/tlog?token={token}` (`produces = MediaType.TEXT_EVENT_STREAM_VALUE`).
- **Validation** : Vérifier la validité du token (comme fait pour les déploiements) avant d'appeler le service.

## 5. Frontend : API et Services
- **`hostsService.js`** : Ajouter une méthode `getTlogStreamUrl(hostId)` ou gérer la connexion EventSource (en récupérant d'abord un token SSE temporaire via `deploymentsService.getSseToken()` ou un nouveau service similaire).
- **`adminSettingsService.js` / Vue de Paramètres** : S'assurer que `default_tlog_command` peut être éditée dans le composant `SettingsView.vue`.

## 6. Frontend : Édition des Hôtes
- **`AdminHostCreateView.vue`** & **`HostEditView.vue`** : Ajouter un champ `<textarea>` pour la commande `tlogCommand` personnalisée (avec une aide indiquant les variables `{host}`, `{ip}`, `{domain}`).

## 7. Frontend : Interface Utilisateur (HostDetailView.vue)
- **Modifications visuelles** :
  - Remplacer l'affichage actuel par un layout en "split screen" vertical (deux colonnes).
  - **Colonne de gauche** : Informations de l'hôte, actions (Bouton Déployer), et Logs de déploiement (le composant actuel `DeploymentLogsModal` ou l'affichage en ligne).
  - **Colonne de droite** : Panneau dédié aux "Logs de l'application" (tlog).
- **Logique SSE** :
  - Initialiser une nouvelle source EventSource pour l'URL `/hosts/{id}/tlog`.
  - Gérer l'état de connexion (Démarrer/Arrêter le flux, auto-reconnexion éventuelle, état "En attente").
  - L'EventSource devra être proprement fermé (`close()`) dans le hook `beforeUnmount` pour éviter de laisser des processus distants tourner inutilement.

## 8. Vérification
- S'assurer que l'exécution de la commande distante est correctement tuée sur le serveur quand le client ferme l'onglet ou navigue ailleurs.
- Vérifier que les variables d'environnement (`{domain}`, etc.) sont bien substituées.
- Vérifier que l'interface en split screen reste lisible et responsive.