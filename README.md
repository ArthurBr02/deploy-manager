# Deploy Manager

Application web interne de gestion et de déploiement d'applications sur des hôtes distants via des commandes bash.

## Stack

| Couche | Technologie |
|---|---|
| Backend | Java 21 · Spring Boot 3.3 · Maven |
| Frontend | Vue 3 · Vite · Tailwind CSS · Pinia |
| Base de données | PostgreSQL 16 · Flyway |
| Auth | JWT (access + refresh) · Argon2 |
| Temps réel | SSE (Server-Sent Events) |

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
| Application | http://localhost |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |

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

---

## Structure du projet

```
deploy-manager/
├── back/                  # Backend Spring Boot
│   ├── src/main/java/fr/arthurbr02/deploymanager/
│   │   ├── config/        # Sécurité, OpenAPI, Async
│   │   ├── controller/    # AuthController, HostController, DeploymentController...
│   │   ├── service/       # Logique métier
│   │   ├── entity/        # Entités JPA
│   │   ├── repository/    # Repositories Spring Data
│   │   ├── dto/           # DTOs requête/réponse
│   │   ├── security/      # JwtUtil, JwtAuthFilter
│   │   └── enums/         # Role, DeploymentStatus, DeploymentType
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/  # Migrations Flyway (V1/V2/V3)
├── front/                 # Frontend Vue.js
│   └── src/
│       ├── api/           # Axios + intercepteur refresh
│       ├── stores/        # Pinia (auth, toast)
│       ├── router/        # Vue Router + guards
│       ├── layouts/       # AppLayout (sidebar)
│       ├── views/         # Pages (hosts, deployments, admin...)
│       └── components/    # Composants réutilisables
├── doc/
│   ├── specs-claude.md    # Cahier des charges
│   └── progress.md        # Suivi d'avancement
├── plan/
│   └── plan_implementation_deploy_manager.md
├── design/                # Maquettes HTML/JSX de référence
├── docker-compose.yml
└── .env.example
```

---

## API — Endpoints principaux

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Connexion |
| `POST` | `/api/auth/refresh` | Rafraîchir le token |
| `POST` | `/api/auth/logout` | Déconnexion |
| `GET` | `/api/hosts` | Lister les hôtes accessibles |
| `POST` | `/api/admin/hosts` | Créer un hôte *(admin)* |
| `POST` | `/api/deployments/hosts/{id}/deploy` | Lancer un déploiement |
| `GET` | `/api/deployments/{id}/logs` | Stream SSE des logs |
| `POST` | `/api/deployments/{id}/cancel` | Annuler un déploiement |
| `GET` | `/api/deployments` | Historique paginé (filtres: host, status, type) |
| `GET` | `/api/admin/users` | Lister les utilisateurs *(admin)* |
| `PUT` | `/api/admin/settings` | Paramètres globaux *(admin)* |
| `POST` | `/api/admin/hosts/import` | Import Ansible hosts-all *(admin)* |

Documentation complète : **http://localhost:8080/api/swagger-ui.html**

---

## Commandes dans les hôtes

Les variables suivantes sont remplacées automatiquement avant l'exécution :

| Variable | Valeur |
|---|---|
| `{host}` | Nom de l'hôte |
| `{ip}` | Adresse IP |
| `{domain}` | Nom de domaine |

Exemple : `sh /root/{host}/liv.sh` → `sh /root/vpn/liv.sh`
