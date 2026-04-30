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
- [x] Enrichissement des logs d'audit avec les informations utilisateur (Backend & UI)
- [x] Refonte de la page de détail utilisateur (Historique des déploiements et logs d'audit filtrés)
- [x] Système de notifications externes (Webhooks Discord/Slack)

### Sprint 17 : Expérience Utilisateur et Persistence
- [x] Système de tris et filtres persistants (URL + localStorage) sur toutes les listes
- [x] Amélioration du filtrage des logs d'audit côté backend (par utilisateur, entité, action et recherche)
- [x] Ajout de filtres de recherche pour la gestion des droits d'accès par utilisateur

### Sprint 17 : Terminal SSH Web
- [x] Permission `can_execute` sur les hôtes
- [x] Backend : Intégration WebSocket et gestion session SSH
- [x] Frontend : Interface terminal avec xterm.js

## Phase 10 : Design Adaptatif et Mobilité

### Sprint 18 : Menu Mobile et Layout
- [x] Menu latéral pliable (Sidebar) avec backdrop sur mobile
- [x] Header mobile avec bouton menu et accès profil
- [x] Adaptation des vues principales (Hôtes, Déploiements, Détail Hôte)
- [x] Optimisation des formulaires et tables pour les petits écrans

## Phase 11 : Fonctionnalités Avancées (2026)

### Sprint 19 : MCP et Dumps SQL
- [x] Paramètres de désactivation du MCP
- [x] Gestion des dumps SQL (Backend & UI)
- [x] Système de demande de dump par e-mail

## Phase 12 : Streaming SSE

### Sprint 20 : Correction du buffering pipe pour les logs en temps réel
- [x] Analyse : Identification du buffering pipe Unix et du buffering Proxy Nginx (Gzip) comme causes racines.
- [x] Backend : Remplacement de la lecture par ligne (`readLine`) par une lecture brute par blocs (`InputStreamReader.read`) pour un streaming caractère par caractère.
- [x] Backend : Utilisation de `unbuffer` (paquet `expect`) pour simuler un TTY et forcer Maven/Git à flusher.
- [x] Backend : Ajout d'un padding de sécurité de 4 Ko à l'ouverture du flux SSE pour forcer le flush des tampons Nginx.
- [x] Backend : Ajout des en-têtes `X-Accel-Buffering: no` et `Cache-Control: no-transform` pour bloquer le buffering et la compression Gzip des proxys.
- [x] Documentation : Mise à jour du README avec les prérequis (`expect`) et la configuration Nginx optimale.

## Phase 13 : Audit Détaillé et Terminal

### Sprint 21 : Traçabilité complète et vue Audit améliorée
- [x] Backend : AuditService — surcharge JSON (snapshots complets), findById
- [x] Backend : AuditController — endpoint GET /admin/audit/{id}
- [x] Backend : UserService — audit CRUD (CREATE/UPDATE/DELETE) avec snapshot (sans password)
- [x] Backend : HostService — snapshots JSON complets pour Host (CREATE/UPDATE/DELETE) + audit UserHostPermission
- [x] Backend : TerminalHandler — log TERMINAL_CONNECT / TERMINAL_DISCONNECT / TERMINAL_COMMAND (si activé)
- [x] Backend : AppConfigService — paramètre `audit_terminal_commands` (défaut : false)
- [x] Frontend : AuditDetailModal.vue — modal diff champ par champ (JSON diff ou métadonnées)
- [x] Frontend : AuditView.vue — bouton détail, liens entités cliquables, nouveaux badges (Terminal), responsive mobile
- [x] Frontend : SettingsView.vue — toggle "Logger les commandes Terminal SSH" avec avertissement
- [x] Frontend : adminAuditService.js — méthode getById
- [x] Backend & Frontend : Groupement des commandes d'une même session terminal via `context_id`

## Phase 14 : Améliorations Dumps SQL (Issues #6 et #10)

### Sprint 22 : Options de configuration des dumps SQL par hôte
- [x] Migration BDD (V17) : colonnes `dump_enabled` (BOOLEAN, défaut TRUE) et `dump_filename` (VARCHAR)
- [x] Backend : Entité `Host` — ajout des champs `dumpEnabled` et `dumpFilename`
- [x] Backend : DTOs (`HostRequest`, `HostResponse`, `HostWithStatusResponse`) — exposition des nouveaux champs
- [x] Backend : `HostService` — `isDumpAvailable` et `getDump` utilisent le nom de fichier configurable ; `requestDump` et `getDump` vérifient `dumpEnabled` (403 si désactivé)
- [x] Frontend : `HostDetailView` — boutons dump masqués si `dumpEnabled = false`
- [x] Frontend : `HostEditForm` et `HostEditView` — toggle `dumpEnabled` + input `dumpFilename`

### Sprint 23 : Commande de dump + permission canDump (Issue #16)
- [x] Migration BDD (V18) : colonne `dump_command` sur `hosts`, colonne `can_dump` sur `user_host_permissions`
- [x] Backend : Entité `Host` — champ `dumpCommand` ; entité `UserHostPermission` — champ `canDump`
- [x] Backend : DTOs (`HostRequest`, `PermissionRequest`, `HostResponse`, `HostWithStatusResponse`) — nouveaux champs
- [x] Backend : `HostService.generateDump()` — exécution de la commande avec remplacement `{dump_name}`, `{host}`, `{ip}`, `{domain}`
- [x] Backend : `HostService.requestDump()` — emails envoyés aux admins ET aux utilisateurs `canDump` de l'hôte
- [x] Backend : `HostController` — endpoint `POST /hosts/{id}/dump/generate`
- [x] Frontend : `hostsService.js` — méthode `generateDump`
- [x] Frontend : `HostDetailView` — bouton "Générer le dump" (visible si `canDump && dumpCommand && dumpEnabled`)
- [x] Frontend : `HostEditForm` + `HostEditView` — champ `dumpCommand`
- [x] Frontend : `UserDetailView` — toggle permission "Dump" dans la gestion des droits

## Maintenance & Hotfixes
- [x] Correction d'un crash `TypeError` dans `UserDetailView` lors de l'affichage de logs d'audit with `entityId` nul.
- [x] Correction d'une erreur `400 Bad Request` lors de la modification d'un hôte sans domaine (relaxation de la validation DTO).
- [x] Correction de la connexion WebSocket du terminal (URL `/api/ws/terminal`) et d'une erreur de nettoyage de l'addon `xterm`.
- [x] Amélioration SSH : Ajout de la configuration de l'utilisateur et du port SSH par hôte (Backend & UI).
- [x] Documentation : Mise à jour du guide de déploiement (README) pour la configuration explicite du répertoire de logs en production.
- [x] Backend : Activation des logs applicatifs dans un fichier (`deploy-manager.log`) via la configuration Spring Boot.
- [x] Correction du fuseau horaire : Passage de `LocalDateTime` à `Instant` dans tout le backend pour assurer un affichage correct (UTC -> Local) dans le frontend.
- [x] Correctif SSE : Résolution de la saturation des connexions (limite navigateur de 6) via l'utilisation de `sse-token` à usage unique, la fermeture systématique des flux sur erreur (`onerror`) et le maintien du flux jusqu'à la fin du healthcheck asynchrone.
- [x] Correctif #7 : Ajout des champs "Commande de Rollback" et "URL Healthcheck" dans la page de modification d'un hôte (`HostEditView.vue`) — champs présents dans le backend mais absents du formulaire frontend.
