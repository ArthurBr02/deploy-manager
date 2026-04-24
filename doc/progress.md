# Suivi de progression - Deploy Manager

Ce fichier permet de suivre l'avancement du projet selon le plan d'implémentation.
Cochez les cases au fur et à mesure de l'avancement (`[x]`).

## Phase 1 : Fondations et Sécurité

### Sprint 1 : Initialisation des projets et Base de données
- [x] Initialisation projet Spring Boot (Java 21, Spring Boot 3.3, Maven)
- [x] Initialisation projet Vue.js + Tailwind
- [x] Configuration de PostgreSQL et scripts de migration (Flyway V1/V2/V3)
- [x] Configuration variables d'environnement (application.yml)

### Sprint 2 : Authentification et Gestion de profil
- [x] Implémentation Spring Security + hachage Argon2
- [x] Endpoints Auth (Login, Refresh, Logout, ForgotPassword, ResetPassword) + admin initial (AdminInitializer)
- [x] Implémentation UI (Login, Layout, Store Pinia Axios)
- [x] UI Page Profil (modification infos)

## Phase 2 : Gestion des Utilisateurs et des Hôtes

### Sprint 3 : Administration des utilisateurs
- [x] Endpoints CRUD Users (soft delete, gestion profil, changement mot de passe)
- [x] UI Liste utilisateurs et formulaires d'édition

### Sprint 4 : Gestion des Hôtes et des Permissions
- [x] Migrations `Host` et `UserHostPermission`
- [x] Endpoints CRUD Hosts et assignation permissions (+ import Ansible)
- [x] UI Liste des hôtes selon droits
- [x] UI Édition hôte et permissions utilisateur

### Sprint 5 : Configuration globale et Import Ansible
- [x] Endpoints `AppConfig` (GET/PUT /admin/settings)
- [x] Logique d'import fichier `hosts-all` (Ansible inventory parser)
- [x] UI Page paramètres et import

## Phase 3 : Moteur de Déploiement

### Sprint 6 : Logique d'exécution des déploiements
- [x] Migration `Deployment`
- [x] Service exécution Shell et remplacement variables ({host}, {ip}, {domain})
- [x] Gestion statut et blocage appels concurrents (existsByHostIdAndStatus)

### Sprint 7 : Temps réel (SSE), Logs et Timeout
- [x] SSE pour streaming des logs (SseEmitter, CopyOnWriteArrayList)
- [x] Persistance des logs (fichier .log puis BDD)
- [x] Gestion Timeout (job planifié @Scheduled) et annulation (destroyForcibly)
- [x] UI Modale de lancement et Console SSE en direct

## Phase 4 : Historique et Finalisation (v1)

### Sprint 8 : Historique et UI
- [x] Endpoints historique (pagination JPA Specification, filtres hostId/status/type)
- [x] UI Page globale historique
- [x] UI Notifications Toasts fin de déploiement

### Sprint 9 : Tests, Corrections et Déploiement
- [x] Ajout des champs d'audit (created_by, updated_by, deleted_by) aux entités
- [x] Sécurité (CORS, JWT, Injection Shell, Validation DTO, SSE Tokens)
- [x] Dockerisation (Dockerfile back/front, docker-compose, nginx.conf, .env.example)

## Phase 6 : Améliorations UI/UX

### Sprint 12 : Uniformisation de l'affichage utilisateur
- [x] Ajout de `userAvatar` dans `DeploymentResponse` (Backend)
- [x] Création du composant `UserAvatar` (Initials fallback + Image)
- [x] Création du composant `UserBadge` (Avatar + Nom complet)
- [x] Intégration dans `AppLayout`, `ProfileView`, `DeploymentTable`, `DeploymentLogsModal`, `UsersView` et `HostDetailView`

## Phase 7 : Monitoring et Logs Applicatifs

### Sprint 13 : Logs en temps réel (tlog)
- [x] Migration BDD (V9__add_tlog_command.sql) et entité Host
- [x] Backend : Service de streaming SSE pour tlog (HostService)
- [x] Backend : Endpoint SSE tlog (HostController)
- [x] Frontend : Formulaire d'édition (tlogCommand) et paramètres globaux
- [x] Frontend : Layout Split-Screen vertical (HostDetailView)
- [x] Frontend : Intégration SSE tlog et gestion cycle de vie flux
- [x] Validation : Vérification fermeture des processus distants

## Phase 8 : Sécurité et Robustesse (2026)

### Sprint 14 : Correctifs de Sécurité et Cycle de vie
- [x] Correction Injection Shell (Echappement systématique des variables)
- [x] Hachage des Personal Access Tokens (Argon2)
- [x] Nettoyage automatique des déploiements orphelins au démarrage

### Sprint 15 : Améliorations Déploiement
- [x] Commande de Rollback (BDD, Backend et UI)
- [x] Healthcheck post-déploiement automatique (Configuration et logique)

## Phase 9 : Administration et Monitoring

### Sprint 16 : Audit et Notifications
- [x] Audit Log Avancé (Historique des modifications de configuration)
- [x] Harmonisation du style de la page Audit avec les autres tables admin
- [x] Système de notifications externes (Webhooks Discord/Slack)

### Sprint 17 : Terminal SSH Web
- [x] Permission `can_execute` sur les hôtes
- [x] Backend : Intégration WebSocket et gestion session SSH
- [x] Frontend : Interface terminal avec xterm.js

