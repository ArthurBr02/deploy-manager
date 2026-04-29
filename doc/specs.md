# Objectif
Nom de l'application : Deploy Manager
But : Permettre aux utilisateurs de gérer et de déployer facilement leurs applications sur différentes plateformes, tout en offrant une interface conviviale et des fonctionnalités avancées pour optimiser les processus de déploiement.

# Fonctionnalités
Il est possible d'importer un fichier "hosts-all" d'Ansible (voir exemple example/hosts-all) pour créer une liste d'hôtes à partir de ce fichier. L'application doit être capable de lire et de parser le fichier "hosts-all" pour extraire les informations nécessaires sur les hôtes, telles que les adresses IP, les noms de domaines et les noms d'hôtes. Une fois importé, l'utilisateur pourra visualiser et gérer ces hôtes directement depuis l'interface de l'application, facilitant ainsi le processus de déploiement et d'administration des systèmes. Lors de l'import, le matching est fait sur le nom de l'hôte (hostname) et non sur l'adresse IP, ce qui permet de gérer les hôtes même si leurs adresses IP changent. Il ne faut mettre à jour que les hôtes dont le nom correspond, et ne pas supprimer les hôtes qui ne sont pas dans le fichier d'import.

Il y a une gestion d'utilisateurs avec des rôles d'administrateur et d'utilisateur standard. Les administrateurs ont des privilèges étendus, leur permettant de gérer les utilisateurs, de configurer les paramètres de l'application et d'accéder à toutes les fonctionnalités. Les utilisateurs standard ont des accès limités, leur permettant uniquement de visualiser et de gérer les hôtes sur lesquels ils ont accès, sans pouvoir modifier les paramètres globaux ou gérer les autres utilisateurs.

Les utilisateurs peuvent se connecter avec un couple email/mot de passe pour accéder à l'application. L'authentification est sécurisée et gérée via JWT (JSON Web Tokens), assurant que les sessions utilisateur sont protégées et que les données sensibles sont sécurisées. Les utilisateurs peuvent également réinitialiser leur mot de passe en cas d'oubli, garantissant ainsi un accès continu à l'application.

Les utilisateurs ont accès à une liste d'hôtes sur lesquels ils peuvent effectuer des déploiements. Chaque hôte peut être configuré avec des paramètres spécifiques, tels que les adresses IP, les noms de domaines et les commandes de déploiement personnalisées. Les utilisateurs peuvent sélectionner un hôte et lancer un processus de déploiement, qui exécutera une commande bash en local (sur le serveur) pour effectuer la livraison de l'application sur l'hôte distant. Les utilisateurs peuvent également suivre l'état du déploiement en temps réel, visualiser les logs en temps réel (il faut donc retourner en direct le retour de la commande bash) et recevoir des notifications en cas de succès ou d'échec du déploiement avec un Toast.

Il faudrait un historique des déploiements pour chaque hôte, permettant aux utilisateurs de consulter les déploiements passés, les résultats et les logs associés. Cela aiderait à diagnostiquer les problèmes et à suivre les performances des déploiements au fil du temps. Il faudra aussi tracer quel utilisateur a lancé quel déploiement pour chaque hôte, afin d'avoir une traçabilité complète des actions effectuées dans l'application.

Il est possible de consulter les déploiements effectués par un utilisateur.

# Technologies utilisées
- Backend : Java avec Spring Boot pour la gestion des API et de la logique métier.
- Frontend : VueJS pour une interface utilisateur dynamique et réactive + Tailwind CSS pour le design et la mise en page.
- Base de données : PostgreSQL pour stocker les informations sur les utilisateurs, les hôtes et les configurations de déploiement.
- Authentification : JWT (JSON Web Tokens) pour sécuriser les endpoints de l'API et gérer les sessions utilisateur.
- Exécution d'une commande bash en local pour effectuer la livraison d'une application sur un hôte distant (comme par défaut à définir puis commande redéfinissable pour chaque hôte par l'admin). 

# Technique
Je veux que les événements soient gérés de manière asynchrone en utilisant un système type socket ou sse (Server-Sent Events) pour permettre une communication en temps réel entre le serveur et le client. Cela permettra de mettre à jour l'interface utilisateur en temps réel avec les logs du déploiement et les notifications de succès ou d'échec, offrant ainsi une expérience utilisateur fluide et réactive.

Migration SQL avec flyway. Initialisation avec Flyway.

Utilisateur défini par:
- id (UUID)
- email (String)
- password (String, chiffré avec argon2)
- role (Enum : ADMIN, USER)
- createdAt (DateTime)
- updatedAt (DateTime)
- deletedAt (DateTime, nullable)

Hôte défini par:
- id (UUID)
- name (String)
- ip (String)
- domain (String)
- deploymentCommand (String, par défaut "sh /root/{host}/liv.sh")
- createdAt (DateTime)
- updatedAt (DateTime)
- deletedAt (DateTime, nullable)

Déploiement défini par:
- id (UUID)
- hostId (UUID, référence à l'hôte)
- userId (UUID, référence à l'utilisateur qui a lancé le déploiement)
- status (Enum : PENDING, IN_PROGRESS, SUCCESS, FAILURE)
- logs (Text, pour stocker les logs du déploiement)
- createdAt (DateTime)