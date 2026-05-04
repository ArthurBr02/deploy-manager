# Plan : Correction du bug SSE — Zombie emitters et "rien ne remonte au front"

## Contexte

Après X déploiements, les événements SSE cessent de remonter au frontend.  
Les logs montrent `totalEmitters=23` suivi de `Relais brisé (pipe)` lors d'un broadcast.  
L'UI se bloque sur IN_PROGRESS et ne reflète plus l'état réel des déploiements.

---

## Analyse des causes

### Cause 1 — Accumulation de zombies (root cause principale)
`SseEmitter(0L)` = timeout infini. Le serveur ne détecte une connexion morte que lors d'un write.  
À chaque reconnexion du frontend, un nouveau SseEmitter est ajouté à `globalEmitters` sans que l'ancien (mort) soit retiré. Résultat : 22 zombies + 1 actif → 23 emitters.  
`DeploymentService.java` lignes 575–577.

### Cause 2 — Nettoyage uniquement paresseux
Les zombies ne sont supprimés que lors d'un broadcast (`broadcastStatusEvent` ligne 596–601).  
Entre deux déploiements il n'y a aucun write → les zombies s'accumulent sans limite.

### Cause 3 — Fenêtre de reconnexion = événements perdus
Quand l'émetteur actif meurt (broken pipe pendant broadcast) :
1. Serveur retire l'émetteur → 0 émetteur actif pour cet utilisateur
2. Frontend reçoit erreur EventSource → attend 3 000 ms avant de se reconnecter
3. Si le déploiement change de statut pendant ces 3 s → événement perdu définitivement
4. UI bloquée sur l'état précédent  
`useDeploymentEvents.js` ligne 55.

---

## Corrections

### Fix 1 — Heartbeat périodique (backend) — `DeploymentService.java`

Ajouter `@Scheduled(fixedDelay = 30_000)` qui envoie un commentaire SSE à tous les émetteurs.  
Les zombies sont détectés proactivement et retirés immédiatement, sans attendre un broadcast.

### Fix 2 — Déduplication par userId (backend) — `DeploymentService.java`

Ajouter `userId` dans le record `EventEmitter`. Lors d'une nouvelle souscription, compléter et retirer tout émetteur existant pour ce même userId avant d'en créer un nouveau.  
1 utilisateur = maximum 1 émetteur actif à tout moment.

### Fix 3 — Replay du statut courant à la connexion (backend) — `DeploymentService.java`

Juste après l'enregistrement du nouvel émetteur dans `subscribeEvents()`, envoyer le statut de tous les déploiements `IN_PROGRESS` accessibles par cet utilisateur.  
Comble la fenêtre de 3 s de reconnexion — le frontend se resynchronise immédiatement.

### Fix 4 — Rechargement de l'hôte au reconnect (frontend) — `useDeploymentEvents.js`

Sur l'événement `open` (après erreur), émettre un callback `onReconnect` pour que `HostDetailView` appelle `loadHost()` et resynchronise l'état de l'UI.

---

## Fichiers modifiés

| Fichier | Changement |
|---------|------------|
| `back/.../service/DeploymentService.java` | Fix 1, 2, 3 |
| `back/.../DeployManagerApplication.java` | `@EnableScheduling` si absent |
| `back/.../repository/DeploymentRepository.java` | `findByStatus()` si absent |
| `front/src/composables/useDeploymentEvents.js` | Fix 4 |

---

## Vérification

1. Lancer 10+ déploiements → vérifier `totalEmitters` ≤ nb d'utilisateurs connectés
2. Vérifier logs `[SSE] heartbeat` toutes les 30 s
3. Couper réseau 3 s → frontend se reconnecte et reçoit le bon statut courant
4. Console navigateur : plus de boucle d'erreurs sans reconnexion réussie
