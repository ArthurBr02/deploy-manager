# Plan : Audit détaillé + Responsive vue Audit

## Contexte
Le système de logs d'audit existe mais est incomplet : seuls les hôtes et la configuration sont tracés, avec des valeurs simplifiées (juste le nom de l'entité). L'objectif est d'étendre la couverture (Users, Permissions, Terminal), de stocker des snapshots JSON complets pour permettre un diff visuel, et d'améliorer le responsive de la vue Audit.

## Décisions clés
- **Terminal** : Logs connexion + commandes tapées, avec toggle `audit_terminal_commands` dans les paramètres admin (défaut : désactivé)
- **Diff** : Snapshots JSON complets dans `old_value` / `new_value` — modal au clic pour afficher les différences champ par champ
- **Entités** : Nom de l'entité devient un lien cliquable vers `/hosts/:id` ou `/users/:id` dans la vue Audit
- **Scope audit** : Users CRUD + UserHostPermission + Terminal (en plus de Host et AppConfig déjà couverts)
- **Responsive** : Uniquement la vue Audit (AuditView.vue)

---

## Backend

### 1. AuditService — Sérialisation JSON complète
**Fichier** : `back/src/main/java/fr/arthurbr02/deploymanager/service/AuditService.java`
- Injecter `ObjectMapper`
- Ajouter surcharge `log(String entityName, UUID entityId, String action, Object oldEntity, Object newEntity)` qui sérialise les objets en JSON via ObjectMapper
- Conserver la méthode `log(String, UUID, String, String, String)` existante pour compatibilité

### 2. UserService — Audit CRUD
**Fichier** : `back/src/main/java/fr/arthurbr02/deploymanager/service/UserService.java`
- Créer `UserAuditSnapshot` record (sans password : id, email, firstName, lastName, role) — classe interne ou DTO dédié
- `create()` : `auditService.log("User", id, "CREATE", null, snapshot)`
- `update()` : snapshot avant + après, `auditService.log("User", id, "UPDATE", oldSnapshot, newSnapshot)`
- `delete()` (soft delete) : `auditService.log("User", id, "DELETE", snapshot, null)`

### 3. HostService — Snapshots complets + Permissions
**Fichier** : `back/src/main/java/fr/arthurbr02/deploymanager/service/HostService.java`
- Créer `HostAuditSnapshot` record avec tous les champs Host (name, ip, domain, sshUser, sshPort, deploymentCommand, generateCommand, deliverCommand, rollbackCommand, healthcheckUrl, etc.)
- `create()` : remplacer `host.getName()` par `auditService.log("Host", id, "CREATE", null, snapshot)`
- `update()` : capturer snapshot avant modification, puis `auditService.log("Host", id, "UPDATE", oldSnapshot, newSnapshot)`
- `delete()` : `auditService.log("Host", id, "DELETE", snapshot, null)`
- `setPermission()` : ajouter `auditService.log("UserHostPermission", hostId, "UPDATE", oldPerms, newPerms)` avec snapshot `{userId, hostId, canDeploy, canEdit, canExecute}`

### 4. TerminalHandler — Logging connexion + commandes
**Fichier** : `back/src/main/java/fr/arthurbr02/deploymanager/service/TerminalHandler.java`
- Injecter `AuditService` et `AppConfigService`
- Ajouter `Map<String, StringBuilder> commandBuffers` et `Map<String, Long> sessionStartTimes` pour état par session
- `afterConnectionEstablished()` : `auditService.log("Terminal", hostId, "TERMINAL_CONNECT", null, {hostName, userId})`
- `afterConnectionClosed()` : `auditService.log("Terminal", hostId, "TERMINAL_DISCONNECT", null, {durationSeconds})`
- `handleTextMessage()` :
  - Accumuler les caractères dans `commandBuffers.get(sessionId)`
  - Gérer backspace (`\x7f`, `\x08`) : retirer dernier caractère du buffer
  - Filtrer les séquences ANSI (regex `\x1b\[[0-9;]*[A-Za-z]`)
  - Sur `\r` : si `audit_terminal_commands == "true"`, logger `("Terminal", hostId, "TERMINAL_COMMAND", null, {command})`; vider le buffer
- Injecter le `userId` résolu à la connexion dans un `Map<String, UUID> sessionUsers`

### 5. AppConfig — Nouveau paramètre
**Fichier** : `back/src/main/java/fr/arthurbr02/deploymanager/service/AppConfigService.java`
- Ajouter `audit_terminal_commands` = `"false"` dans les valeurs par défaut (méthode `getAll()` ou équivalente)

### 6. AuditController — Endpoint détail
**Fichier** : `back/src/main/java/fr/arthurbr02/deploymanager/controller/AuditController.java`
- Ajouter `GET /admin/audit/{id}` → retourne `AuditLogResponse` complet (pour charger le diff dans la modal)
- Ajouter dans `AuditService` : `findById(UUID id)`

---

## Frontend

### 7. AuditDetailModal.vue — Nouveau composant
**Fichier** : `front/src/components/AuditDetailModal.vue`
- Props : `log` (AuditLogResponse | null), `show` (boolean), emit `close`
- Logique :
  - Tenter de parser `log.oldValue` et `log.newValue` en JSON
  - Si les deux sont JSON : afficher tableau à 3 colonnes (Champ | Avant | Après), lignes surlignées si valeur différente (rouge pour supprimé, vert pour ajouté, jaune pour modifié)
  - Si action `TERMINAL_CONNECT` / `TERMINAL_DISCONNECT` / `TERMINAL_COMMAND` : afficher les métadonnées structurées (pas de diff)
  - Sinon (texte brut hérité) : afficher le texte brut

### 8. AuditView.vue — Refonte
**Fichier** : `front/src/views/admin/AuditView.vue`
- **Colonne "Voir"** : bouton loupe → ouvre `AuditDetailModal` avec le log sélectionné
- **Liens entités** : mapper `entityName` vers route :
  ```
  Host / Terminal → /hosts/:entityId
  User            → /users/:entityId
  AppConfig / UserHostPermission → texte brut (pas de page dédiée)
  ```
- **Responsive mobile** (priorité) :
  - Envelopper la table dans `overflow-x-auto`
  - Sur `< sm` : masquer colonnes "Ancienne valeur" et "Nouvelle valeur" (`hidden sm:table-cell`) — le modal compense
  - Réduire padding cellules sur mobile (`px-3 py-2` vs `px-4 py-3`)
  - Truncate entity ID sur mobile (afficher les 8 premiers caractères)
- **Nouveaux badges action** : ajouter `TERMINAL_CONNECT` (violet), `TERMINAL_COMMAND` (orange), `TERMINAL_DISCONNECT` (gris)

### 9. adminAuditService.js
**Fichier** : `front/src/services/adminAuditService.js`
- Ajouter `getById(id)` → `GET /admin/audit/:id`

### 10. SettingsView.vue — Toggle audit terminal
**Fichier** : `front/src/views/admin/SettingsView.vue`
- Ajouter section "Audit" avec toggle booléen pour `audit_terminal_commands`
- Label : "Logger les commandes Terminal SSH"
- Avertissement visible : "Les commandes tapées dans le terminal seront enregistrées. Les mots de passe peuvent être capturés."

---

## Ordre d'implémentation

1. `AuditService` — sérialisation JSON + `findById`
2. `AuditController` — endpoint `GET /admin/audit/{id}`
3. `UserService` — audit CRUD
4. `HostService` — snapshots complets + permissions
5. `AppConfigService` — paramètre `audit_terminal_commands`
6. `TerminalHandler` — connexion + commandes
7. Frontend : `AuditDetailModal.vue`
8. Frontend : `AuditView.vue` (modal + liens + responsive)
9. Frontend : `adminAuditService.js` + `SettingsView.vue`

---

## Fichiers critiques

| Fichier | Modification |
|---|---|
| `service/AuditService.java` | Surcharge JSON, findById |
| `service/UserService.java` | Ajout appels audit |
| `service/HostService.java` | Snapshots complets + permissions |
| `service/TerminalHandler.java` | Connexion + commandes |
| `service/AppConfigService.java` | Nouveau paramètre |
| `controller/AuditController.java` | Endpoint détail |
| `views/admin/AuditView.vue` | Modal + liens + responsive |
| `components/AuditDetailModal.vue` | Nouveau |
| `services/adminAuditService.js` | getById |
| `views/admin/SettingsView.vue` | Toggle audit_terminal_commands |

---

## Vérification

1. Créer/modifier/supprimer un hôte → `old_value` et `new_value` contiennent le JSON complet de l'entité
2. Créer/modifier/supprimer un utilisateur → logs présents avec snapshots
3. Modifier des permissions canDeploy/canEdit/canExecute → log `UserHostPermission UPDATE`
4. Se connecter au terminal d'un hôte → log `TERMINAL_CONNECT`
5. Activer `audit_terminal_commands` dans Paramètres → taper des commandes → logs `TERMINAL_COMMAND`
6. Dans AuditView : cliquer sur 🔍 → modal s'ouvre avec diff champ par champ (surligné)
7. Cliquer sur un nom d'hôte dans les logs → navigation vers `/hosts/:id`
8. Tester sur mobile (< 640px) : colonnes old/new masquées, table lisible avec scroll horizontal
