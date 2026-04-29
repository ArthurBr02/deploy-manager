# Plan d'implémentation : Améliorations Sécurité et Fonctionnalités (2026)

Ce document détaille les étapes pour implémenter les correctifs de sécurité, les améliorations de robustesse et les nouvelles fonctionnalités demandées.

## 1. Sécurité et Intégrité

### 1.1 Correction Injection de Commandes Shell
*   **Objectif** : Empêcher l'interprétation malveillante ou erronée des variables dans les commandes shell.
*   **Actions** :
    *   Modifier `ShellUtil.replaceVariables` pour entourer systématiquement les variables remplacées (`{host}`, `{ip}`, `{domain}`) par des simple quotes après avoir échappé les simple quotes existantes dans la valeur.
    *   Exemple : `{host}` -> `'mon'\''serveur'` au lieu de `mon serveur`.

### 1.2 Hachage des Personal Access Tokens (PAT)
*   **Objectif** : Ne plus stocker les tokens en clair.
*   **Actions** :
    *   Migration BDD : Augmenter la taille du champ `token` si nécessaire pour le hachage.
    *   Backend : Utiliser `PasswordEncoder` (Argon2) pour hacher le token avant insertion.
    *   Backend : Modifier `validateToken` pour comparer le token fourni avec le hachage en base.
    *   Frontend : S'assurer que le token n'est affiché qu'une seule fois à la création.

## 2. Robustesse technique

### 2.1 Gestion des Déploiements Orphelins au démarrage
*   **Objectif** : Nettoyer les états incohérents après un redémarrage du serveur.
*   **Actions** :
    *   Backend : Créer un composant `@EventListener(ApplicationReadyEvent.class)` qui recherche tous les déploiements avec le statut `IN_PROGRESS`.
    *   Les marquer comme `FAILURE` avec un message de log indiquant que le serveur a redémarré pendant l'exécution.

## 3. Nouvelles Fonctionnalités de Déploiement

### 3.1 Commande de Rollback
*   **Objectif** : Permettre un retour arrière rapide.
*   **Actions** :
    *   Migration BDD : Ajouter `rollback_command` (TEXT) à la table `hosts`.
    *   Backend : Ajouter le champ à l'entité `Host` et aux DTOs.
    *   Backend : Ajouter un nouveau `DeploymentType.ROLLBACK`.
    *   Frontend : Ajouter le bouton "Rollback" dans l'interface de l'hôte et l'historique.

### 3.2 Healthcheck Post-Déploiement
*   **Objectif** : Valider automatiquement le succès d'un déploiement.
*   **Actions** :
    *   Migration BDD : Ajouter `healthcheck_url` (VARCHAR) à la table `hosts` (défaut : `https://{domain}`).
    *   Backend : Après un déploiement `SUCCESS`, lancer une requête asynchrone vers cette URL.
    *   Mettre à jour le statut du déploiement ou ajouter une note dans les logs si le healthcheck échoue.

## 4. Administration et Monitoring

### 4.1 Audit Log Avancé
*   **Objectif** : Historiser les modifications de configuration.
*   **Actions** :
    *   Créer une table `audit_logs` (id, entity_name, entity_id, action, old_value, new_value, user_id, created_at).
    *   Utiliser des `@PostUpdate` / `@PostPersist` sur les entités clés (`Host`, `AppConfig`, `User`) ou un `EntityListener` global.
    *   Frontend : Créer une vue `/admin/audit` pour consulter ces logs.

### 4.2 Notifications Externes (Webhooks/Email)
*   **Objectif** : Alerter en cas d'échec.
*   **Actions** :
    *   Backend : Créer un service de notification générique.
    *   Implémenter un driver Discord/Slack (Webhook POST).
    *   Ajouter une configuration globale pour l'URL du Webhook.
    *   Déclencher l'alerte dans `DeploymentService.persistFinalStatus` si le statut est `FAILURE`.

## 5. Terminal SSH Web interactif

### 5.1 Permission d'exécution
*   **Objectif** : Contrôler qui peut utiliser le terminal.
*   **Actions** :
    *   Migration BDD : Ajouter `can_execute` (boolean) à la table `user_host_permissions`.
    *   Backend : Mettre à jour l'entité `UserHostPermission` et les contrôles d'accès.

### 5.2 Intégration Terminal
*   **Objectif** : Fournir une interface console temps réel.
*   **Actions** :
    *   Backend : Configurer Spring WebSocket (STOMP ou raw WebSockets).
    *   Backend : Utiliser une bibliothèque comme `JSch` ou un tunnel vers un processus shell local pour gérer la session SSH.
    *   Frontend : Intégrer `xterm.js` dans une nouvelle vue `HostTerminalView`.

---

## Ordre d'exécution suggéré
1.  **Sprint A** : Sécurité (Injection Shell & PAT Hashing) + Orphelins.
2.  **Sprint B** : Rollback & Healthcheck.
3.  **Sprint C** : Audit Log & Notifications.
4.  **Sprint D** : Terminal SSH Web (Permissions + Backend WebSocket + Frontend xterm.js).
