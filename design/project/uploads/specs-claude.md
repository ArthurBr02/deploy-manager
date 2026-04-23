# Deploy Manager — Cahier des charges détaillé

## 1. Objectif

Application web interne permettant de gérer et déployer des applications sur des hôtes distants via une interface conviviale. Les déploiements s'exécutent par commande bash locale sur le serveur.

---

## 2. Stack technique

| Couche | Technologie |
|---|---|
| Package Java | `fr.arthurbr02.deploymanager` |
| Backend | Java + Spring Boot |
| Frontend | Vue.js + Tailwind CSS |
| Base de données | PostgreSQL |
| Authentification | JWT (access token + refresh token) |
| Temps réel | SSE (Server-Sent Events) |
| Chiffrement mot de passe | Argon2 |

---

## 3. Modèle de données

### User
| Champ | Type | Notes |
|---|---|---|
| id | UUID | PK |
| email | String | unique |
| firstName | String | |
| lastName | String | |
| password | String | chiffré Argon2 |
| role | Enum | `ADMIN`, `USER` |
| avatar | TEXT | base64, nullable — icône générique si null |
| createdAt | DateTime | |
| updatedAt | DateTime | |
| deletedAt | DateTime | nullable, soft delete |

### Host
| Champ | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | String | nom de l'hôte (hostname) |
| ip | String | |
| domain | String | nullable en base, obligatoire en validation front+back (sauf import) |
| deploymentCommand | String | nullable — si null, utilise la commande par défaut globale |
| generateCommand | String | nullable — bouton "Générer" caché si null |
| deliverCommand | String | nullable — bouton "Livrer" caché si null |
| defaultTimeout | Integer | nullable — si null, utilise le timeout global. 0 = désactivé |
| createdAt | DateTime | |
| updatedAt | DateTime | |
| deletedAt | DateTime | nullable, soft delete |

Variables disponibles dans les commandes : `{host}`, `{ip}`, `{domain}`

### Deployment
| Champ | Type | Notes |
|---|---|---|
| id | UUID | PK |
| hostId | UUID | FK Host |
| userId | UUID | FK User |
| type | Enum | `DEPLOY`, `GENERATE`, `DELIVER` |
| status | Enum | `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILURE`, `CANCELLED` |
| logs | TEXT | logs persistés en base à la fin du déploiement |
| logFilePath | String | chemin vers le fichier de logs sur le serveur |
| timeout | Integer | timeout effectif au moment du lancement (0 = désactivé) |
| createdAt | DateTime | |

### UserHostPermission
| Champ | Type | Notes |
|---|---|---|
| userId | UUID | FK User |
| hostId | UUID | FK Host |
| canDeploy | Boolean | droit de lancer DEPLOY, GENERATE, DELIVER |
| canEdit | Boolean | droit de modifier les commandes de l'hôte |

### AppConfig (settings)
| Clé | Valeur par défaut | Description |
|---|---|---|
| `default_deploy_command` | `sh /root/{host}/liv.sh` | Commande de déploiement globale par défaut |
| `default_timeout` | `10` | Timeout en minutes (0 = désactivé) |
| `smtp_host` | — | Serveur SMTP |
| `smtp_port` | — | Port SMTP |
| `smtp_username` | — | Utilisateur SMTP |
| `smtp_password` | — | Mot de passe SMTP |
| `smtp_from` | — | Adresse expéditeur |

---

## 4. Authentification

- Connexion par email + mot de passe
- **Access token JWT** : stocké en mémoire (store Pinia), courte durée (ex: 15 min)
- **Refresh token JWT** : stocké en cookie httpOnly, longue durée (ex: 7 jours)
- Refresh transparent côté client
- **Réinitialisation de mot de passe** : par email avec token à durée limitée
- **Premier compte admin** : créé au premier démarrage via variables d'environnement (`ADMIN_EMAIL`, `ADMIN_PASSWORD`)
- **Contrainte** : impossible de supprimer le dernier utilisateur `ADMIN` non supprimé

---

## 5. Gestion des utilisateurs

Accessible uniquement aux admins (`/admin/users`).

Actions disponibles :
- Créer un utilisateur (email, prénom, nom, rôle, mot de passe temporaire généré automatiquement — affiché **une seule fois** à la création)
- Modifier un utilisateur (tous les champs sauf mot de passe — reset séparé)
- Supprimer (soft delete)
- Assigner les droits sur les hôtes (`canDeploy`, `canEdit`) depuis la fiche utilisateur

Les utilisateurs standard peuvent modifier leur propre profil (prénom, nom, avatar, mot de passe) depuis `/profile`.

---

## 6. Gestion des hôtes

- Création et suppression (soft delete) : **admins uniquement**
- Modification : admins + utilisateurs ayant `canEdit` sur cet hôte (ils peuvent modifier `deploymentCommand`, `generateCommand`, `deliverCommand`, `defaultTimeout`)
- Import depuis fichier Ansible `hosts-all` : admins uniquement

### Import hosts-all
- Champs extraits : `hostname` → `name`, `ansible_host` → `ip`, `domain_name` → `domain` (optionnel)
- Toutes les autres variables (`open_port`, etc.) et les groupes (`[acces_externe]`) sont ignorés
- Matching sur le **nom de l'hôte** uniquement
- Seuls les hôtes dont le nom correspond sont mis à jour
- Les hôtes absents du fichier ne sont **pas supprimés**

### Affichage liste des hôtes (`/hosts`)
Chaque carte affiche :
- Nom, IP, domaine
- Badge statut du dernier déploiement (coloré)
- Spinner si un déploiement est `IN_PROGRESS`
- Boutons **Déployer** / **Générer** / **Livrer** (selon droits `canDeploy` et commandes définies)
- Tooltip sur chaque bouton = commande résolue avec variables remplacées (ex: `sh /root/vpn/liv.sh`)

---

## 7. Déploiements

### Lancement
- Un seul déploiement actif à la fois par hôte (blocage au lancement si `IN_PROGRESS`)
- Au lancement, une modale affiche le timeout pré-rempli (valeur hôte > valeur globale) que l'utilisateur peut modifier
- Indication dans l'UI : "0 = timeout désactivé"
- Le champ `timeout` est enregistré dans le déploiement au moment du lancement

### Timeout
- Hiérarchie : timeout saisi au lancement (pré-rempli par valeur hôte > valeur globale)
- Un job planifié vérifie les déploiements `IN_PROGRESS` bloqués
- Si `timeout > 0` et durée dépassée : le processus shell est tué, statut → `FAILURE`
- Si `timeout = 0` : aucun timeout, le déploiement peut tourner indéfiniment

### Annulation
- L'utilisateur qui a lancé le déploiement **ou** un admin peut annuler un déploiement `IN_PROGRESS`
- Le processus shell est tué, statut → `CANCELLED`

### Logs en temps réel
- Stream via **SSE** (`SseEmitter` Spring Boot)
- Logs accumulés dans un **fichier sur le serveur** pendant l'exécution
- Logs **persistés en base** (champ `logs`) à la fin (succès, échec, annulation, timeout)
- **Reconnexion** : les logs accumulés sont chargés depuis le fichier, puis le stream SSE reprend en live

### Notifications
- Toast de succès ou d'échec à la fin du déploiement (aucun email)

---

## 8. Historique des déploiements

### Visibilité
- Utilisateurs standard : voient les déploiements des hôtes auxquels ils ont accès (`canDeploy` ou `canEdit`)
- Admins : voient tous les déploiements

### Après soft delete
- Les déploiements restent visibles dans l'historique même si l'hôte ou l'utilisateur est supprimé
- Le nom de l'hôte et le nom/email de l'utilisateur sont affichés (jointures incluant les enregistrements soft-deleted)

### Affichage
- Pagination : 20 résultats par page
- Filtres : hôte, statut, type (DEPLOY/GENERATE/DELIVER), période
- Accessible depuis :
  - `/deployments` — page globale (admins voient tout, users voient selon leurs accès)
  - `/hosts/:id` — onglet historique dans la fiche d'un hôte

---

## 9. Routes de l'application

| Route | Accès | Description |
|---|---|---|
| `/login` | Public | Connexion |
| `/hosts` | Tous | Liste des hôtes accessibles |
| `/hosts/:id` | Tous | Fiche hôte (déploiement, historique, détails) |
| `/deployments` | Tous | Historique global |
| `/admin/users` | Admin | Liste et gestion des utilisateurs |
| `/admin/users/:id` | Admin | Fiche utilisateur + assignation hôtes |
| `/admin/hosts/new` | Admin | Créer un hôte |
| `/admin/settings` | Admin | Paramètres globaux |
| `/profile` | Tous | Profil personnel (avatar, prénom, nom, mot de passe) |

---

## 10. Rôles et permissions — récapitulatif

| Action | ADMIN | USER (canDeploy) | USER (canEdit) | USER (aucun droit) |
|---|---|---|---|---|
| Voir les hôtes assignés | ✅ (tous) | ✅ | ✅ | ✅ |
| Lancer DEPLOY/GENERATE/DELIVER | ✅ | ✅ | ❌ | ❌ |
| Modifier commandes hôte | ✅ | ❌ | ✅ | ❌ |
| Créer/supprimer un hôte | ✅ | ❌ | ❌ | ❌ |
| Annuler un déploiement | ✅ | ✅ (le sien) | ✅ (le sien) | ❌ |
| Voir historique hôtes assignés | ✅ (tous) | ✅ | ✅ | ❌ |
| Gérer les utilisateurs | ✅ | ❌ | ❌ | ❌ |
| Configurer les paramètres | ✅ | ❌ | ❌ | ❌ |
| Importer hosts-all | ✅ | ❌ | ❌ | ❌ |

---

## 11. Variables d'environnement (backend)

| Variable | Description |
|---|---|
| `ADMIN_EMAIL` | Email du premier compte admin |
| `ADMIN_PASSWORD` | Mot de passe du premier compte admin |
| `JWT_ACCESS_SECRET` | Secret pour les access tokens |
| `JWT_REFRESH_SECRET` | Secret pour les refresh tokens |
| `JWT_ACCESS_EXPIRY` | Durée access token (ex: `15m`) |
| `JWT_REFRESH_EXPIRY` | Durée refresh token (ex: `7d`) |
| `DB_URL` | URL PostgreSQL |
| `DB_USERNAME` | Utilisateur base de données |
| `DB_PASSWORD` | Mot de passe base de données |
| `LOG_DIR` | Répertoire de stockage des fichiers de logs |

---

## 12. Version 2 — Serveur MCP (Model Context Protocol)

Exposer les fonctionnalités de Deploy Manager via un serveur MCP pour permettre à un agent IA (ex: Claude) de piloter les déploiements directement depuis une conversation.

### Outils MCP envisagés

| Outil | Description |
|---|---|
| `list_hosts` | Lister les hôtes disponibles (selon les droits du token) |
| `get_host` | Récupérer les détails d'un hôte |
| `deploy` | Lancer un déploiement DEPLOY / GENERATE / DELIVER sur un hôte |
| `get_deployment_status` | Consulter le statut et les logs d'un déploiement |
| `cancel_deployment` | Annuler un déploiement en cours |
| `list_deployments` | Consulter l'historique des déploiements |

### Authentification MCP
À définir en v2 : token API dédié (distinct du JWT web) associé à un utilisateur, avec les mêmes droits que cet utilisateur.
