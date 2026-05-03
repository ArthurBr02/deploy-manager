# Plan d'Implémentation : Client macOS Natif (SwiftUI) pour Deploy Manager

**Date :** Mai 2026
**Cible :** Apple Silicon (M-series) / macOS Sonoma & ultérieurs
**Backend existant :** Java 21 / Spring Boot 3.3 / SSE / PostgreSQL

## Objectif

Développer une suite d'extensions natives macOS (application principale, barre des menus, widgets, intégrations système) agissant comme une "télécommande" et un moniteur en temps réel pour Deploy Manager. Ce projet a pour double objectif de fournir un client bureau ergonomique et de servir de cas pratique pour l'apprentissage du développement Swift et SwiftUI moderne (Swift 5.9+).

## Périmètre et Impact

- **Périmètre :** Nouveau projet Xcode, totalement indépendant. Le backend Java/Spring Boot reste inchangé.
- **Impact :** Aucun risque sur la base de code existante.

---

## Architecture & Sécurité (Fondations)

### Gestion des secrets (Keychain)

L'URL de l'API et le **Personal Access Token (PAT)** de l'utilisateur sont stockés chiffrés dans le Trousseau d'accès (Keychain) via le framework `Security`. Le PAT remplace la gestion de tokens JWT éphémères et permet une reconnexion silencieuse au redémarrage.

### Réseau en temps réel (SSE)

`URLSession` écoute les flux Server-Sent Events du backend (`/api/deployments/events`, `/api/hosts/{id}/tlog`).

### Cycle de vie intelligent (Sleep/Wake)

Écoute de `NSWorkspace.willSleepNotification` et `NSWorkspace.didWakeNotification` pour couper proprement les flux SSE à la mise en veille et se reconnecter automatiquement avec backoff exponentiel à la sortie de veille.

---

## Modules

### Module 1 — Fondations & Couche Réseau
*Objectif : Configuration, authentification PAT et requêtes HTTP asynchrones.*

**Concepts Swift :** `struct`, optionnels (`?`, `guard let`), `Codable`, `async/await`, `URLSession`, Keychain (`Security` framework), `@AppStorage`.

**Tâches :**
- Développer `KeychainManager` pour stocker/lire/supprimer l'URL de l'API et le PAT.
- Modéliser les entités principales : `User`, `Host`, `Deployment`.
- Créer un `APIClient` générique utilisant l'URL et le PAT configurés.

---

### Module 2 — État Global & Écran de Configuration
*Objectif : Interface de configuration (URL + PAT) et gestion de la session.*

**Concepts Swift :** Macro `@Observable`, `@State`, injection `@Environment`.

**Tâches :**
- Créer `AuthManager` (`@Observable`) pour retenir l'état de la session (`isAuthenticated`).
- Développer `SettingsView` permettant la saisie de l'URL du serveur et du PAT.
- Implémenter le routage racine dans `App.swift` (configuration requise → vue principale).

---

### Module 3 — Layout macOS "Pro" et Composants Visuels
*Objectif : Appliquer les standards d'interface Mac (Sidebar) et préparer des composants réutilisables.*

**Concepts Swift :** `NavigationSplitView`, `Table` (macOS natif), `enum` pour le routage.

**Tâches :**
- Configurer `NavigationSplitView` avec barre latérale (Hosts, Déploiements, Réglages).
- Créer `HostCardView` (carte réutilisable affichant le statut d'un Host).
- Construire `HostsView` listant les serveurs.

---

### Module 4 — Formulaires, Modales et Validations
*Objectif : Modifier l'infrastructure (Création / Édition / Lancement de déploiement).*

**Concepts Swift :** `Form`, `@FocusState`, `.sheet()`, closures.

**Tâches :**
- Implémenter `HostFormView` pour l'ajout et la modification des serveurs.
- Ajouter un bouton de déploiement ouvrant une modale de sélection du type de déploiement.
- Gérer la validation asynchrone et le rafraîchissement des listes parentes.

---

### Module 5 — Temps Réel : SSE + Terminal ANSI
*Objectif : Gérer le flux de logs d'un déploiement en direct.*

**Concepts Swift :** `AsyncSequence`, gestion du cycle de vie (`Task`), `AttributedString`, `ScrollViewReader`.

**Tâches :**
- Connecter un écouteur SSE via `URLSession.shared.bytes(from:)`.
- Parser les séquences d'échappement ANSI en couleurs macOS (`Color` / `NSColor`).
- Développer `TerminalView` avec défilement automatique à chaque nouvelle entrée.
- Intégrer la gestion Sleep/Wake pour couper et reconnecter le flux proprement.

---

### Module 6 — Barre des Menus (Centre de Contrôle)
*Objectif : Moniteur résident et actions rapides depuis la barre des menus macOS.*

**Concepts Swift :** `MenuBarExtra` (`.window` style), `NSStatusItem`, `NSPopover`, `LocalAuthentication` (Touch ID), `UserNotifications`.

**Tâches :**
- Créer le popover de barre des menus listant les hôtes favoris avec pastilles d'état.
- Développer `StatusBubble` et `StatusGridView` affichant les derniers déploiements (bulles colorées, animations, `.help()` pour tooltips).
- Intégrer `HostCardView` pour déclencher un déploiement sans ouvrir l'application principale.
- **Tlog Whisperer :** écoute en arrière-plan du flux `/tlog` ; si une erreur est détectée (regex `error|panic`), l'icône passe au rouge et déclenche une notification native.
- Sécuriser les déploiements sensibles avec validation **Touch ID** (`LocalAuthentication`).
- Notifier l'utilisateur via `UserNotifications` au succès/échec d'un déploiement lancé en arrière-plan.

---

### Module 7 — Live Activities (Deploy Tracker)
*Objectif : Suivi visuel d'un déploiement en cours dans l'encoche macOS.*

**Concepts Swift :** `ActivityKit`, `ActivityAttributes`.

**Tâches :**
- Démarrer une Activité en Direct au lancement d'un déploiement.
- Afficher le type d'opération (`DEPLOY`, `ROLLBACK`…) et le nom de l'hôte.
- Animer l'indicateur de progression jusqu'à réception de `SUCCESS` ou `FAILURE` via SSE.
- Intégrer un bouton [Cancel] pour interrompre le processus distant.

---

### Module 8 — Widgets de Bureau
*Objectif : Tableau de bord interactif sur le fond d'écran macOS.*

**Concepts Swift :** `WidgetKit`, `SwiftUI Charts`, `AppIntents` (pour widgets interactifs).

**Types de widgets :**
- **Vue Globale :** liste des hôtes avec hash du dernier commit déployé et environnement.
- **Statistiques :** graphique (framework `Charts`) du taux de succès et du temps moyen des 7 derniers jours (`GET /api/deployments/stats`).
- **Actions Rapides :** boutons interactifs natifs pour lancer un déploiement ou un rollback sans ouvrir l'application.

---

### Module 9 — Intégrations Système (Spotlight, Siri & IA)
*Objectif : Transformer Deploy Manager en outil intégré au système macOS.*

**Concepts Swift :** `AppIntents`, Model Context Protocol (MCP).

**Tâches :**
- Encapsuler les actions de l'API (`Deploy`, `Rollback`) en `AppIntent`.
- Permettre le lancement via Spotlight (`Cmd+Espace`) ou Siri : *"Déploie le projet en Staging"*.
- (Optionnel) Connecter le serveur MCP du backend aux modèles Apple Intelligence pour exécuter des requêtes en langage naturel.

---

## Feuille de Route (6 Semaines)

| Semaine | Modules | Livrable |
|---------|---------|----------|
| 1 | 1 + 2 | Fondations : Keychain, APIClient, écran de configuration |
| 2 | 3 + 4 | Application principale : liste des hôtes, formulaires |
| 3 | 5 | SSE + TerminalView + gestion Sleep/Wake |
| 4 | 6 | Barre des menus : monitoring, Touch ID, notifications |
| 5 | 7 + 8 | Live Activities + Widgets de bureau |
| 6 | 9 | Intégrations Spotlight/Siri, AppIntents |

---

## Vérification & Tests

- Vérifier que le PAT est lisible/supprimable depuis l'application Trousseau d'accès de macOS.
- Confirmer que `@Observable` met à jour la fenêtre principale et le `MenuBarExtra` simultanément.
- Tester la consommation mémoire de `TerminalView` sur de très grands logs.
- Vérifier que la fermeture du popover de barre des menus annule proprement les `Task` HTTP en cours.
- Tester le comportement SSE après une mise en veille prolongée (reconnexion avec backoff).
