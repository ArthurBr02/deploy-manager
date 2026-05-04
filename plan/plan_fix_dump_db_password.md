# Plan : Fix dump "Dump indisponible" + placeholder {db_password}

## Diagnostic

**Erreur "Dump indisponible"** (HostService.java:428) : La commande `ssh root@{domain} "mysqldump -u root -p hbbh2 > {host}.sql"` place `>` à l'intérieur des guillemets SSH → le fichier est créé sur le **serveur distant**. `getDump()` cherche ensuite le fichier sur le **filesystem local** → absent → RuntimeException.

**Commande correcte** :
```
ssh root@{domain} "mysqldump -u root -p{db_password} nom_base" > {dump_name}
```
- `>` hors des guillemets → sortie redirigée localement
- `{dump_name}` → chemin local calculé par `generateDump()`
- `{db_password}` → nouveau placeholder

## Fichiers à modifier

| Fichier | Modification |
|---|---|
| `back/.../entity/Host.java` | Ajouter champ `dbPassword` |
| `back/.../dto/host/HostRequest.java` | Ajouter `dbPassword` |
| `back/.../dto/host/HostResponse.java` | Ne pas ajouter le password |
| `back/.../dto/host/HostWithStatusResponse.java` | Ne pas ajouter le password |
| `back/.../dto/host/HostAdminDetailResponse.java` | **Nouveau** DTO avec `dbPassword` |
| `back/.../util/ShellUtil.java` | Ajouter `dbPassword` à `replaceVariables()` |
| `back/.../service/HostService.java` | `create`, `update`, `generateDump`, `streamTlog`, `HostAuditSnapshot`, nouveau `getHostForEdit()` |
| `back/.../service/DeploymentService.java` | Mettre à jour appels `replaceVariables()` |
| `back/.../controller/HostController.java` | Ajouter `GET /hosts/{id}/edit` |
| `back/resources/db/migration/V19__add_host_db_password.sql` | **Nouveau** |
| `front/src/services/hostsService.js` | Ajouter `getForEdit(id)` |
| `front/src/views/HostEditView.vue` | Utiliser `getForEdit`, ajouter champ password |
| `front/src/views/admin/AdminHostCreateView.vue` | Ajouter champ `dbPassword` |

## Règles de sécurité

- `dbPassword` jamais retourné par `GET /hosts` ni `GET /hosts/{id}` (HostWithStatusResponse)
- `dbPassword` retourné uniquement par `GET /hosts/{id}/edit` (HostAdminDetailResponse), accessible admin ou canEdit
- Dans l'audit log : masqué en `***`
- En update : si `dbPassword` null/vide → conserver l'existant

## Substitution shell

`{db_password}` est échappé avec `escapeShell()` mais **sans** quotes supplémentaires (utilisé en `-p{db_password}` directement dans une string SSH).
