# Deploy Manager

Application web interne de gestion et de déploiement d'applications sur des hôtes distants via des commandes bash.

## Stack

| Couche | Technologie |
|---|---|
| Backend | Java 21 · Spring Boot 3.3 · Maven |
| Frontend | Vue 3 · Vite · Tailwind CSS · Pinia |
| Base de données | PostgreSQL 16 · Flyway |
| Auth | JWT (access + refresh) · Argon2 · Personal Access Tokens (PAT) |
| Temps réel | SSE (Server-Sent Events) · WebSocket |
| Terminal web | Spring WebSocket · xterm.js |
| Intégration AI | Serveur MCP (Model Context Protocol) via SSE |

---

## Fonctionnalités

### Déploiement
- **Types de déploiement** : DEPLOY, GENERATE, DELIVER, ROLLBACK — chaque hôte peut avoir une commande distincte par type
- **Exécution shell** : remplacement automatique des variables `{host}`, `{ip}`, `{domain}` avec échappement anti-injection
- **Logs en temps réel** : streaming SSE des sorties stdout/stderr pendant l'exécution
- **Timeout & annulation** : job planifié de détection des timeouts et annulation manuelle avec kill forcé du processus
- **Blocage concurrent** : un seul déploiement actif par hôte
- **Healthcheck post-déploiement** : requête HTTP automatique après un déploiement réussi (URL configurable par hôte)
- **Nettoyage au démarrage** : les déploiements `IN_PROGRESS` au redémarrage sont automatiquement marqués `FAILURE`

### Logs applicatifs (tlog)
- Streaming SSE en temps réel via une commande distante configurable (ex : `ssh root@{domain} tlog`)
- Commande globale par défaut (`default_tlog_command`) surchargeable par hôte
- Affichage en split-screen vertical sur la vue détaillée de l'hôte
- Fermeture propre du processus distant à la déconnexion du client

### Terminal SSH Web
- Interface terminal interactive (xterm.js) directement dans le navigateur
- Connexion WebSocket (`/ws/terminal`) avec authentification JWT
- Permission dédiée `can_execute` par hôte et par utilisateur

### Sécurité
- Authentification JWT (access 15 min + refresh 7 jours, rotation automatique)
- Personal Access Tokens (PAT) hachés en Argon2, affichés une seule fois à la création
- SSE protégés par tokens à usage unique
- Injection shell bloquée : les variables sont systématiquement entre quotes avec échappement
- Soft delete sur toutes les entités principales (User, Host)

### Administration
- CRUD utilisateurs (génération de mot de passe, gestion des rôles, avatar)
- Page de détail utilisateur complète avec son historique de déploiements et son journal d'audit filtré
- CRUD hôtes avec assignation fine des permissions par utilisateur (`can_deploy`, `can_execute`)
- Import de fichier Ansible `hosts-all` (parsing et mise à jour conditionnelle)
- Paramètres globaux (commande tlog par défaut, notifications, shell, OS)
- **Audit log** : historique paginé des modifications de configuration (Host, AppConfig, User) avec enrichissement automatique des informations utilisateur (nom complet, email)
- **Notifications externes** : webhook Discord/Slack déclenché automatiquement sur les déploiements en échec

### Historique & Monitoring
- Historique des déploiements paginé avec filtres (hôte, statut, type)
- Statistiques des déploiements sur une période glissante
- Export CSV de l'historique
- Notifications toast en temps réel (SSE events) à la fin de chaque déploiement

---

## Démarrage rapide — Docker

**Prérequis** : Docker Desktop

```bash
cp .env.example .env
# Éditez .env pour définir vos secrets JWT et vos identifiants admin
docker-compose up --build
```

| Service | URL |
|---|---|
| Application | http://localhost:3000 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| MCP SSE | http://localhost:8080/api/mcp/sse |
| Terminal WS | ws://localhost:8080/api/ws/terminal |

Le port du frontend est configurable via `FRONTEND_PORT` dans `.env` (défaut : `3000`).

---

## Démarrage en développement local

### Prérequis

- Java 21+
- Maven 3.9+
- Node.js 20+
- PostgreSQL 16 (instance locale ou Docker)

### 1. Base de données

```bash
# Option rapide avec Docker
docker run -d \
  --name deploy-manager-db \
  -e POSTGRES_DB=deploymanager \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Backend

```bash
cd back

# Avec les valeurs par défaut (postgres/postgres, admin admin@example.com / Admin1234!)
mvn spring-boot:run

# Ou avec des variables personnalisées
ADMIN_EMAIL=moi@example.com ADMIN_PASSWORD=MonMotDePasse mvn spring-boot:run
```

Le backend démarre sur **http://localhost:8080/api**

> Les migrations Flyway s'exécutent automatiquement au démarrage.  
> Le compte admin est créé automatiquement s'il n'existe aucun administrateur.

### 3. Frontend

```bash
cd front
npm install
npm run dev
```

Le frontend démarre sur **http://localhost:5173**

---

## Variables d'environnement

Copiez `.env.example` en `.env` et adaptez les valeurs.

| Variable | Défaut | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/deploymanager` | URL PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur BDD |
| `DB_PASSWORD` | `postgres` | Mot de passe BDD |
| `JWT_ACCESS_SECRET` | *(valeur de dev)* | Secret access token — **changer en prod** |
| `JWT_REFRESH_SECRET` | *(valeur de dev)* | Secret refresh token — **changer en prod** |
| `JWT_ACCESS_EXPIRY` | `15m` | Durée de vie access token |
| `JWT_REFRESH_EXPIRY` | `7d` | Durée de vie refresh token |
| `ADMIN_EMAIL` | `admin@example.com` | Email du premier admin |
| `ADMIN_PASSWORD` | `Admin1234!` | Mot de passe du premier admin |
| `LOG_DIR` | `./logs/deployments` | Répertoire des fichiers de logs |
| `FRONTEND_PORT` | `3000` | Port exposé du frontend (Docker) |

Les paramètres suivants sont configurables dans l'interface d'administration (`/admin/settings`) et stockés en base de données :

| Clé | Défaut | Description |
|---|---|---|
| `default_tlog_command` | `ssh root@{domain} tlog` | Commande de logs applicatifs par défaut |
| `notification_enabled` | `false` | Activer les notifications webhook |
| `notification_webhook_url` | *(vide)* | URL du webhook Discord ou Slack |

---

## Structure du projet

```
deploy-manager/
├── back/                  # Backend Spring Boot
│   ├── src/main/java/fr/arthurbr02/deploymanager/
│   │   ├── config/        # Sécurité, OpenAPI, Async, WebSocket, Cleanup
│   │   ├── controller/    # AuthController, HostController, DeploymentController,
│   │   │                  # AuditController, McpController...
│   │   ├── service/       # Logique métier (DeploymentService, HostService,
│   │   │                  # AuditService, NotificationService, TerminalHandler...)
│   │   ├── entity/        # Entités JPA (User, Host, Deployment, AuditLog...)
│   │   ├── repository/    # Repositories Spring Data
│   │   ├── dto/           # DTOs requête/réponse
│   │   ├── security/      # JwtUtil, JwtAuthFilter
│   │   └── enums/         # Role, DeploymentStatus, DeploymentType
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/  # Migrations Flyway (V1 → V13)
├── front/                 # Frontend Vue.js
│   └── src/
│       ├── api/           # Axios + intercepteur refresh
│       ├── stores/        # Pinia (auth, toast)
│       ├── router/        # Vue Router + guards
│       ├── layouts/       # AppLayout (sidebar)
│       ├── views/         # Pages (hosts, deployments, admin, terminal...)
│       └── components/    # Composants réutilisables (TypeBadge, UserAvatar...)
├── doc/
│   ├── specs-claude.md    # Cahier des charges
│   └── progress.md        # Suivi d'avancement
├── plan/
│   ├── plan_implementation_deploy_manager.md
│   ├── plan_improvements_2026.md
│   └── plan_tlog_feature.md
├── design/                # Maquettes HTML/JSX de référence
├── docker-compose.yml
└── .env.example
```

---

## API — Endpoints principaux

### Authentification
| Méthode | Route | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Connexion |
| `POST` | `/api/auth/refresh` | Rafraîchir le token |
| `POST` | `/api/auth/logout` | Déconnexion |
| `POST` | `/api/auth/forgot-password` | Demande de réinitialisation de mot de passe |
| `POST` | `/api/auth/reset-password` | Réinitialisation du mot de passe |

### Profil & Personal Access Tokens
| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/profile` | Mon profil |
| `PUT` | `/api/profile` | Modifier mon profil |
| `POST` | `/api/profile/change-password` | Changer mon mot de passe |
| `POST` | `/api/profile/avatar` | Uploader mon avatar |
| `DELETE` | `/api/profile/avatar` | Supprimer mon avatar |
| `GET` | `/api/profile/tokens` | Lister mes Personal Access Tokens |
| `POST` | `/api/profile/tokens` | Créer un nouveau PAT |
| `DELETE` | `/api/profile/tokens/{id}` | Révoquer un PAT |

### Hôtes
| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/hosts` | Lister les hôtes accessibles |
| `GET` | `/api/hosts/{id}` | Détail d'un hôte |
| `POST` | `/api/admin/hosts` | Créer un hôte *(admin)* |
| `PUT` | `/api/hosts/{id}` | Modifier un hôte |
| `DELETE` | `/api/admin/hosts/{id}` | Supprimer un hôte *(admin, soft delete)* |
| `GET` | `/api/hosts/{id}/tlog` | Stream SSE des logs applicatifs |
| `POST` | `/api/admin/hosts/import` | Import Ansible hosts-all *(admin)* |
| `GET` | `/api/admin/users/{userId}/permissions` | Permissions d'un utilisateur *(admin)* |
| `PUT` | `/api/admin/users/{userId}/permissions` | Modifier les permissions *(admin)* |

### Déploiements
| Méthode | Route | Description |
|---|---|---|
| `POST` | `/api/deployments/sse-token` | Générer un token SSE à usage unique |
| `POST` | `/api/deployments/hosts/{id}/deploy` | Lancer un déploiement (DEPLOY/GENERATE/DELIVER/ROLLBACK) |
| `POST` | `/api/deployments/{id}/cancel` | Annuler un déploiement en cours |
| `GET` | `/api/deployments/{id}/logs` | Stream SSE des logs d'un déploiement |
| `GET` | `/api/deployments/events` | Stream SSE des changements de statut |
| `GET` | `/api/deployments` | Historique paginé (filtres : host, user, status, type) |
| `GET` | `/api/deployments/stats` | Statistiques sur une période |
| `GET` | `/api/deployments/export` | Export CSV de l'historique |

### Administration
| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/admin/users` | Lister les utilisateurs *(admin)* |
| `GET` | `/api/admin/users/{id}` | Détail d'un utilisateur *(admin)* |
| `POST` | `/api/admin/users` | Créer un utilisateur *(admin)* |
| `PUT` | `/api/admin/users/{id}` | Modifier un utilisateur *(admin)* |
| `DELETE` | `/api/admin/users/{id}` | Supprimer un utilisateur *(admin, soft delete)* |
| `GET` | `/api/admin/settings` | Paramètres globaux *(admin)* |
| `PUT` | `/api/admin/settings` | Modifier les paramètres *(admin)* |
| `GET` | `/api/admin/audit` | Logs d'audit paginés *(admin)* |
| `GET` | `/api/admin/audit/user/{userId}` | Logs d'audit filtrés par utilisateur *(admin)* |

### Terminal & MCP
| Méthode | Route | Description |
|---|---|---|
| `WS` | `/api/ws/terminal` | Terminal SSH interactif (WebSocket) |
| `GET` | `/api/mcp/sse` | Point d'entrée pour les clients MCP |
| `POST` | `/api/mcp/messages` | Envoi de messages MCP |

Documentation complète : **http://localhost:8080/api/swagger-ui.html**

---

## Intégration MCP (Model Context Protocol)

Deploy Manager embarque un serveur MCP permettant à des LLMs (comme Claude Desktop) de piloter vos déploiements.

### Configuration

1. Connectez-vous à l'interface web et allez dans votre **Profil**.
2. Créez un **Personal Access Token (PAT)** et copiez-le.
3. Ajoutez le serveur à votre configuration MCP (ex: `claude_desktop_config.json`) :

```json
{
  "mcpServers": {
    "deploy-manager": {
      "command": "curl",
      "args": [
        "-X", "GET",
        "-H", "Authorization: Bearer VOTRE_TOKEN",
        "http://localhost:8080/api/mcp/sse"
      ]
    }
  }
}
```
*Note : Le transport SSE nécessite un client compatible ou un bridge stdio-to-sse.*

### Outils disponibles via MCP

**Tous les utilisateurs :**
- `list_hosts` : Liste les serveurs auxquels vous avez accès.
- `get_host` : Affiche les détails d'un serveur spécifique.
- `update_host` : Modifie les paramètres d'un serveur.
- `deploy` : Lance un déploiement (DEPLOY, GENERATE, DELIVER ou ROLLBACK).
- `get_deployments` : Liste l'historique des déploiements.

**Administrateurs uniquement :**
- `create_host` / `delete_host` : Gestion des serveurs.
- `list_users` / `create_user` / `update_user` / `delete_user` : Gestion des utilisateurs.
- `set_permissions` : Gestion des permissions utilisateurs sur les hôtes.
- `get_settings` / `update_settings` : Paramètres globaux.

---

## Commandes dans les hôtes

Les variables suivantes sont remplacées automatiquement avant l'exécution (avec échappement des quotes pour prévenir l'injection) :

| Variable | Valeur |
|---|---|
| `{host}` | Nom de l'hôte |
| `{ip}` | Adresse IP |
| `{domain}` | Nom de domaine |

Chaque hôte peut définir des commandes spécifiques pour chaque type d'opération :

| Champ | Type | Description |
|---|---|---|
| `deployCommand` | TEXT | Commande de déploiement principal |
| `generateCommand` | TEXT | Commande de génération |
| `deliverCommand` | TEXT | Commande de livraison |
| `rollbackCommand` | TEXT | Commande de rollback |
| `tlogCommand` | TEXT | Commande de logs applicatifs (surcharge le défaut global) |
| `healthcheckUrl` | VARCHAR | URL vérifiée après un déploiement réussi (défaut : `https://{domain}`) |

Exemple : `sh /root/{host}/liv.sh` → `sh /root/vpn/liv.sh`
