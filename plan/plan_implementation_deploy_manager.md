# Deploy Manager — Plan d'implémentation

Ce document définit les phases et les sprints pour l'implémentation complète du projet Deploy Manager, basé sur le cahier des charges (`specs-claude.md`).

## Phase 1 : Fondations et Sécurité

### Sprint 1 : Initialisation des projets et Base de données
*   **Backend** : Initialisation du projet Spring Boot (Web, Security, Data JPA, PostgreSQL).
*   **Frontend** : Initialisation du projet Vue.js avec Vite, Tailwind CSS, Pinia, Vue Router.
*   **Base de données** : Configuration de PostgreSQL et mise en place des scripts de migration (ex: Flyway ou Liquibase) pour le schéma initial (`User`, `AppConfig`).
*   **Configuration** : Mise en place des variables d'environnement (DB, JWT, Admin initial).

### Sprint 2 : Authentification et Gestion de profil
*   **Backend** : Implémentation de la sécurité Spring Security.
*   **Backend** : Création des endpoints d'authentification (Login, Refresh Token, Reset Password).
*   **Backend** : Logique de hachage Argon2 et génération JWT (Access/Refresh).
*   **Backend** : Création automatique du premier admin au démarrage.
*   **Frontend** : Pages de Login et layout principal.
*   **Frontend** : Store Pinia pour la gestion de l'utilisateur et des tokens (intercepteur Axios pour le refresh).
*   **Frontend** : Page `/profile` pour la modification des informations personnelles (nom, prénom, avatar, mot de passe).

## Phase 2 : Gestion des Utilisateurs et des Hôtes

### Sprint 3 : Administration des utilisateurs
*   **Backend** : Endpoints CRUD pour les utilisateurs (`/api/admin/users`).
*   **Backend** : Logique de soft delete et protection du dernier admin.
*   **Frontend** : Page `/admin/users` (liste, création avec génération de mot de passe, édition, suppression).

### Sprint 4 : Gestion des Hôtes et des Permissions
*   **Base de données** : Migration pour les tables `Host` et `UserHostPermission`.
*   **Backend** : Endpoints CRUD pour les hôtes (`/api/admin/hosts` et `/api/hosts`).
*   **Backend** : Endpoints pour la gestion des permissions utilisateurs sur les hôtes.
*   **Frontend** : Page de la liste des hôtes `/hosts` (filtrée par permissions).
*   **Frontend** : Formulaires de création/édition d'un hôte, et assignation des droits depuis la fiche utilisateur.

### Sprint 5 : Configuration globale et Import Ansible
*   **Backend** : Endpoints pour la gestion de `AppConfig` (paramètres par défaut, SMTP).
*   **Backend** : Logique d'import du fichier Ansible `hosts-all` (parsing et mise à jour conditionnelle).
*   **Frontend** : Page `/admin/settings` (paramètres globaux et bouton d'import).

## Phase 3 : Moteur de Déploiement

### Sprint 6 : Logique d'exécution des déploiements
*   **Base de données** : Migration pour la table `Deployment`.
*   **Backend** : Service d'exécution de commandes shell natives (`ProcessBuilder`).
*   **Backend** : Logique de remplacement des variables (`{host}`, `{ip}`, etc.) dans les commandes.
*   **Backend** : Gestion de l'état du déploiement (PENDING, IN_PROGRESS, SUCCESS, FAILURE).
*   **Backend** : Mécanisme de blocage (un seul déploiement actif par hôte).

### Sprint 7 : Temps réel (SSE), Logs et Timeout
*   **Backend** : Implémentation du flux SSE (`SseEmitter`) pour les logs en temps réel.
*   **Backend** : Écriture des logs dans un fichier temporaire, puis persistance en BDD à la fin.
*   **Backend** : Job planifié pour la vérification des timeouts et annulation du processus (kill shell).
*   **Backend** : Endpoint d'annulation manuelle d'un déploiement en cours.
*   **Frontend** : Intégration SSE pour afficher la console de logs en direct dans la fiche hôte.
*   **Frontend** : Modale de confirmation de lancement avec saisie du timeout.

## Phase 4 : Historique et Finalisation (v1)

### Sprint 8 : Historique et UI
*   **Backend** : Endpoints de recherche et pagination des déploiements.
*   **Frontend** : Page globale `/deployments` avec filtres (hôte, statut, type).
*   **Frontend** : Onglet d'historique dans la fiche d'un hôte.
*   **Frontend** : Notifications (Toasts) de succès/échec à la fin d'un déploiement.

### Sprint 9 : Tests, Corrections et Déploiement
*   **Création** : Tests unitaires et d'intégration critiques (Backend & Frontend).
*   **Révision** : Vérification globale de la sécurité (droits d'accès selon les rôles).
*   **Déploiement** : Dockerisation (Dockerfile back/front, docker-compose) et documentation de déploiement.

## Phase 5 : Serveur MCP (v2)

### Sprint 10 : Intégration Model Context Protocol
*   **Backend** : Implémentation du serveur MCP pour exposer les outils à l'IA (`list_hosts`, `deploy`, `get_deployment_status`, etc.).
*   **Sécurité** : Création d'un système de token API dédié pour l'authentification MCP.
