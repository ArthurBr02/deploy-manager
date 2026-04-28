# Plan d'Implémentation : Client macOS Natif (SwiftUI) pour Deploy Manager

## Objectif
Créer une application macOS native en SwiftUI pour s'interfacer avec l'API existante de Deploy Manager. Ce projet a pour double objectif de fournir un client bureau ergonomique ("seamless") et de servir de cas pratique approfondi pour l'apprentissage du développement Swift et SwiftUI moderne (Swift 5.9+).

## Périmètre et Impact
- **Périmètre :** Création d'un client lourd totalement indépendant. Le backend Java/Spring Boot existant reste inchangé.
- **Impact :** Aucun risque sur la base de code existante (le client sera un nouveau projet Xcode).

## Solution Proposée : Apprentissage en 6 Modules

### Module 1 : Fondations Swift & Couche Réseau (Network Layer)
*Objectif : Configuration, authentification, requêtes HTTP asynchrones et sécurité.*
- **Concepts Swift :** `struct`, optionnels (`?`, `guard let`), protocole `Codable`, `async/await`, `URLSession`, `@AppStorage` (`UserDefaults`).
- **Tâches :**
  - Gérer l'URL de l'API (ex: `https://api.monserveur.com`) de manière dynamique en la stockant en local via `@AppStorage` pour permettre à l'utilisateur de la configurer et de la modifier.
  - Modéliser `User`, `LoginRequest` et `LoginResponse`.
  - Créer `APIClient` générique qui utilise l'URL configurée.
  - Sauvegarder le JWT (Access/Refresh) de façon sécurisée via le **Keychain macOS** (et non UserDefaults).

### Module 2 : Gestion de l'État Global et Fenêtre de Connexion
*Objectif : Interface de login et bascule d'environnement.*
- **Concepts Swift :** Macro `@Observable`, variables `@State`, injection `@Environment`.
- **Tâches :**
  - Créer `AuthManager` (@Observable) pour retenir l'état de la session (`isAuthenticated`).
  - Développer `LoginView.swift` avec `TextField`, `SecureField` et la gestion des erreurs HTTP.
  - Implémenter le routage racine dans `App.swift`.

### Module 3 : Layout macOS "Pro" et Composants Visuels
*Objectif : Appliquer les standards d'interface Mac (Sidebar) et préparer des composants réutilisables.*
- **Concepts Swift :** `NavigationSplitView`, `Table` (macOS natif), `enum` pour le routage.
- **Tâches :**
  - Configurer `NavigationSplitView` avec une barre latérale pour naviguer entre les vues (Hosts, Déploiements, Réglages).
  - Créer le composant `HostCardView.swift` (Carte réutilisable affichant le statut d'un Host).
  - Construire la vue principale `HostsView` qui liste les serveurs.

### Module 4 : Formulaires, Modales et Validations
*Objectif : Modifier l'infrastructure (Création/Édition/Lancement de déploiement).*
- **Concepts Swift :** Vues de type `Form`, `@FocusState`, présentations `.sheet()` et fermetures (closures).
- **Tâches :**
  - Implémenter `HostFormView` pour l'ajout et la modification des serveurs.
  - Ajouter un bouton de déploiement qui ouvre une pop-up native sélectionnant le type de déploiement.
  - Gérer la validation asynchrone et le rafraîchissement des listes parentes.

### Module 5 : Temps Réel (SSE) et Terminal ANSI (Le "Boss Final")
*Objectif : Gérer le flux de logs d'un déploiement.*
- **Concepts Swift :** `AsyncSequence`, gestion du cycle de vie (`Task`), manipulation avancée avec `AttributedString`, `ScrollViewReader`.
- **Tâches :**
  - Connecter un écouteur sur l'endpoint SSE via `URLSession.shared.bytes(from:)`.
  - Parser les séquences d'échappement ANSI en couleurs reconnues par macOS (`Color` / `NSColor`).
  - Développer un `TerminalView` qui affiche les logs et défile (scroll) automatiquement à chaque nouvelle entrée.

### Module 6 : Utilitaires Système ("Apple Touch" & MenuBar)
*Objectif : Transformer l'application en outil intégré au système d'exploitation macOS.*
- **Concepts Swift :** `MenuBarExtra` (.window style), `LazyVGrid`, `LocalAuthentication` (Touch ID), `UserNotifications`.
- **Tâches :**
  - **Menu Rapide (MenuBarExtra) :** Créer un popover attaché à la barre des menus macOS.
  - **Grille de Statuts :** Créer `StatusBubble` et `StatusGridView` affichant les derniers déploiements sous forme de bulles colorées (avec animations de chargement et le modificateur natif `.help()` pour les tooltips au survol).
  - **Actions Rapides :** Intégrer `HostCardView` dans ce menu pour déclencher un déploiement sans ouvrir l'application principale.
  - **Touch ID :** Sécuriser les déploiements sensibles avec la validation de l'empreinte biométrique.
  - **Notifications :** Notifier l'utilisateur via le système macOS lors du succès/échec d'un déploiement lancé en arrière-plan.

## Vérification & Tests
- Vérifier que le JWT est bien lisible/supprimable depuis l'application Trousseau d'Accès de macOS.
- Confirmer que la macro `@Observable` met bien à jour la fenêtre principale et le MenuBarExtra simultanément (synchronisation de l'état).
- Tester la consommation mémoire du `TerminalView` lors de l'affichage d'un très grand log.
- Vérifier que la fermeture du popover de la barre des menus annule proprement les requêtes HTTP non terminées (annulation de `Task`).