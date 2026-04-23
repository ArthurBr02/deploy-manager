# Instrucciones pour l'agent (CLAUDE.md)

Ce fichier fournit le contexte et les règles pour travailler sur le projet **Deploy Manager**.

## Contexte du Projet
- **Nom** : Deploy Manager
- **Type** : Application web interne de gestion et de déploiement d'applications sur des hôtes distants via des commandes bash locales.
- **Dépôt** : `ArthurBr02/deploy-manager`
- **Langue** : Français pour la documentation et l'interface utilisateur, Anglais pour le code.

## Stack Technique
- **Backend** : Java, Spring Boot
- **Frontend** : Vue.js 3 API OPTIONS, Tailwind CSS, Pinia, Vite
- **Base de données** : PostgreSQL (avec migrations Flyway/Liquibase)
- **Sécurité** : JWT (Access + Refresh), Argon2 pour le hachage
- **Communication temps réel** : SSE (Server-Sent Events)

## Règles de Développement
1. **Suivi d'avancement** : Mettre systématiquement à jour le fichier `doc/progress.md` après l'ajout d'une fonctionnalité ou la réalisation d'une tâche.
2. **Architecture** : Respecter les principes MVC en Spring Boot. Utiliser des DTOs pour la communication API.
3. **Sécurité** : Les modifications de fichiers shell (lors des déploiements) requièrent une attention particulière pour éviter les injections de commandes.
4. **Soft Delete** : Les entités principales (User, Host, etc.) doivent utiliser le Soft Delete (champ `deletedAt`) et ne pas être effacées physiquement de la base.
5. **Gestion des variables** : Toujours vérifier que `{host}`, `{ip}`, `{domain}` sont correctement remplacés dans les commandes avant l'exécution.
6. **Documentation** : Les spécifications se trouvent dans `doc/specs-claude.md`. Le plan d'implémentation est dans `plan/plan_implementation_deploy_manager.md`.
7. **Swagger / OpenAPI** : Mettre à jour systématiquement les annotations OpenAPI/Swagger lors de la création ou de la modification d'un endpoint de l'API.

## Workflow de développement
- Travailler par incréments (Sprint) tels que définis dans le plan d'implémentation.
- Toujours commencer par les fondations (Base de données > Backend API > Frontend).
- Pour créer de nouveaux services Spring, suivre le package `fr.arthurbr02.deploymanager`.
- Pour le design, Read the design/README file for the design to implement.
Implement: Deploy Manager.html
Adapte le code en utilisant les technologies et les règles définies dans ce fichier et dans les autres documents de référence. Assure-toi de suivre le plan d'implémentation et de mettre à jour le suivi de progression au fur et à mesure de l'avancement du projet.
