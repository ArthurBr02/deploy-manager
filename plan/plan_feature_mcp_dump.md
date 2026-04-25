# Plan d'implémentation : Paramètres MCP et Gestion des Dumps SQL

## Objective
Ajouter la possibilité de désactiver le MCP depuis les paramètres administrateur et permettre la gestion des dumps SQL pour chaque hôte (téléchargement si disponible, ou demande par e-mail aux administrateurs).

## Key Files & Context
- **Base de données** : `back/src/main/resources/db/migration/V14__add_host_dump_folder.sql` (Attention à vérifier le numéro de version disponible).
- **Backend Entities / DTOs** : `Host.java`, `HostRequest.java`, `HostResponse.java`, `HostWithStatusResponse.java`
- **Backend Services** : `HostService.java`, `MailService.java` (existant)
- **Backend Controllers** : `HostController.java`, `McpController.java`
- **Backend Repositories** : `UserRepository.java`
- **Frontend Views** : `SettingsView.vue`, `HostDetailView.vue`, `HostEditForm.vue`
- **Frontend Services** : `hostsService.js`

## Implementation Steps

### Phase 0 : Mise à jour du plan et de la progression
1. Mettre à jour `doc/progress.md` pour refléter la nouvelle fonctionnalité en cours.

### Phase 1 : Configuration et Base de données
1. **Migration Flyway** : Créer le fichier de migration (ex: `V14__add_host_dump_folder.sql`) pour ajouter la colonne `dump_folder` (VARCHAR) à la table `hosts`.
2. **Entité Host** : Ajouter la propriété `dumpFolder` dans l'entité `Host`.
3. **DTOs** : Ajouter `dumpFolder` dans `HostRequest` et `HostResponse`. Ajouter un booléen `isDumpAvailable` dans les réponses d'hôtes (`HostResponse`, `HostWithStatusResponse`).

### Phase 2 : Backend - Logique Métier
1. **Repository (UserRepository)** :
   - Ajouter la méthode `List<User> findAllByRoleAndDeletedAtIsNull(Role role)` pour récupérer les administrateurs.
2. **Contrôleur MCP (McpController)** :
   - Modifier les méthodes du `McpController` (notamment `establishSse` et `handleMessage`) pour vérifier si `AppConfigService.get("mcp_enabled", "true")` vaut `"true"`.
   - Si `false`, lever une `ForbiddenException("MCP est désactivé")` ou renvoyer un statut 403.
3. **Service Hôte (HostService)** :
   - Logique pour `isDumpAvailable` : Vérifier si le fichier `{host.name}.sql` existe localement dans le répertoire défini par `host.getDumpFolder()` ou par le paramètre global `default_dump_folder` si non défini sur l'hôte.
4. **Contrôleur Hôte (HostController)** :
   - `GET /api/hosts/{id}/dump` : Vérifie l'existence locale du fichier, le lit et le retourne en tant que ressource téléchargeable (application/sql ou application/octet-stream).
   - `POST /api/hosts/{id}/dump-request` : Récupère tous les utilisateurs ayant le rôle `ADMIN` depuis `UserRepository`, et utilise le `MailService` existant pour leur envoyer un e-mail indiquant que l'utilisateur actuel demande un dump pour cet hôte.

### Phase 3 : Frontend
1. **Paramètres Admin (`SettingsView.vue`)** :
   - Ajouter un Toggle (Checkbox) pour `mcp_enabled` (par défaut `true`).
   - Ajouter un champ texte pour `default_dump_folder` (Chemin du dossier contenant les dumps SQL sur le serveur backend).
2. **Formulaire d'Édition d'Hôte (`HostEditForm.vue`)** :
   - Ajouter un champ texte optionnel pour `dump_folder` (Dossier de dump spécifique pour cet hôte).
3. **Détail de l'Hôte (`HostDetailView.vue`)** :
   - Exploiter le nouveau champ `isDumpAvailable` de la réponse API.
   - Si `true` : Afficher un bouton "Télécharger le dump" qui appelle l'endpoint GET et déclenche le téléchargement du fichier `{host.name}.sql`.
   - Si `false` : Afficher un bouton "Demander un dump" qui appelle l'endpoint POST (afficher un Toast de succès).

## Verification & Testing
- Lancer les migrations Flyway avec succès.
- Sauvegarder les nouveaux paramètres depuis l'interface administrateur (`mcp_enabled`, `default_dump_folder`) et vérifier leur persistance.
- Tester que l'API MCP renvoie bien une erreur 403 si désactivée.
- Créer un fichier `{host.name}.sql` manuellement dans le dossier configuré sur le backend pour vérifier que `isDumpAvailable` passe à `true` et que le téléchargement fonctionne.
- Retirer le fichier pour vérifier que le bouton "Demander un dump" s'affiche et que l'envoi d'e-mail s'effectue correctement.
