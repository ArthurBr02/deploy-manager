# Suivi de progression - Deploy Manager

Ce fichier permet de suivre l'avancement du projet selon le plan d'implémentation.
Cochez les cases au fur et à mesure de l'avancement (`[x]`).

## Phase 1 : Fondations et Sécurité

### Sprint 1 : Initialisation des projets et Base de données
- [ ] Initialisation projet Spring Boot
- [ ] Initialisation projet Vue.js + Tailwind
- [ ] Configuration de PostgreSQL et scripts de migration
- [ ] Configuration variables d'environnement

### Sprint 2 : Authentification et Gestion de profil
- [ ] Implémentation Spring Security + hachage Argon2
- [ ] Endpoints Auth (Login, Refresh, Reset) + admin initial
- [ ] Implémentation UI (Login, Layout, Store Pinia Axios)
- [ ] UI Page Profil (modification infos)

## Phase 2 : Gestion des Utilisateurs et des Hôtes

### Sprint 3 : Administration des utilisateurs
- [ ] Endpoints CRUD Users (soft delete)
- [ ] UI Liste utilisateurs et formulaires d'édition

### Sprint 4 : Gestion des Hôtes et des Permissions
- [ ] Migrations `Host` et `UserHostPermission`
- [ ] Endpoints CRUD Hosts et assignation permissions
- [ ] UI Liste des hôtes selon droits
- [ ] UI Édition hôte et permissions utilisateur

### Sprint 5 : Configuration globale et Import Ansible
- [ ] Endpoints `AppConfig`
- [ ] Logique d'import fichier `hosts-all`
- [ ] UI Page paramètres et import

## Phase 3 : Moteur de Déploiement

### Sprint 6 : Logique d'exécution des déploiements
- [ ] Migration `Deployment`
- [ ] Service exécution Shell et remplacement variables
- [ ] Gestion statut et blocage appels concurrents

### Sprint 7 : Temps réel (SSE), Logs et Timeout
- [ ] SSE pour streaming des logs
- [ ] Persistance des logs (fichier puis BDD)
- [ ] Gestion Timeout (job planifié) et annulation (kill shell)
- [ ] UI Modale de lancement et Console SSE en direct

## Phase 4 : Historique et Finalisation (v1)

### Sprint 8 : Historique et UI
- [ ] Endpoints historique (pagination, filtres)
- [ ] UI Page globale historique
- [ ] UI Notifications Toasts fin de déploiement

### Sprint 9 : Tests, Corrections et Déploiement
- [ ] Sécurité et tests
- [ ] Dockerisation

## Phase 5 : Serveur MCP (v2)

### Sprint 10 : Intégration Model Context Protocol
- [ ] Implémentation serveur MCP et outils (`deploy`, `list_hosts`...)
- [ ] Token API dédié
