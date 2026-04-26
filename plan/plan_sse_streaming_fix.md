# Plan : Correction du streaming SSE des logs (déploiement + tlog)

## Problème

L'infrastructure SSE est en place et correcte des deux côtés, mais les logs n'apparaissent qu'en fin de commande au lieu de s'afficher ligne par ligne en temps réel.

## Cause racine : buffering pipe Unix

Quand `ProcessBuilder` démarre un sous-processus, un **pipe Unix** est créé entre la JVM et le process. Les programmes détectent qu'ils n'écrivent pas vers un terminal (TTY) mais vers un pipe, et activent automatiquement le **mode full-buffered** (buffer interne 4–8 Ko).

```
Programme → [buffer interne 4-8 Ko] → [pipe OS 64 Ko] → BufferedReader.readLine() → SSE
```

`readLine()` ne reçoit des données que quand :
1. Le buffer interne du programme se remplit (beaucoup de logs nécessaires), OU  
2. Le processus se termine (flush automatique à l'exit)

→ **Tous les logs arrivent d'un coup à la fin.**

Pour le tlog SSH : le flag `-t -t` alloue un pseudo-terminal (PTY) côté serveur, forçant le mode line-buffered. Cela fonctionne pour SSH, pas pour les commandes locales.

## Solution : `stdbuf -oL -eL` pour forcer le line-buffering

`stdbuf` (GNU coreutils, disponible sur tous les Linux) utilise `LD_PRELOAD` pour forcer le line-buffering via libc stdio. L'env var `LD_PRELOAD` est héritée par tous les processus fils, ce qui couvre la quasi-totalité des programmes (git, bash, curl, etc.).

**Limitation :** Node.js (npm, pm2) ne passe pas par libc stdio. Pour ces programmes, le buffering reste natif. Dans la pratique, npm en mode non-TTY affiche déjà des groupes de lignes qui arrivent assez rapidement — le comportement est acceptable. La correction couvre tous les autres programmes.

## Modifications

### 1. `DeploymentService.java` — Wrapper stdbuf pour les déploiements
- Ajouter méthode helper `buildProcessBuilder()`  
- Sur Linux : `new ProcessBuilder("stdbuf", "-oL", "-eL", shellBin, shellArg, command)`
- Sur Windows : comportement inchangé

### 2. `HostService.java` — Wrapper stdbuf pour tlog non-SSH
- Pour les commandes non-SSH sur Linux : même wrapper `stdbuf`
- Pour les commandes SSH : déjà couvert par `-t -t` (PTY remote)

### 3. Controllers — `response.flushBuffer()` défensif
- Dans `DeploymentController.streamLogs()` et `HostController.streamTlog()`
- Forcer le flush des headers HTTP avant le retour du SseEmitter

## Vérification

1. Lancer un déploiement avec `sleep 1 && echo line1 && sleep 1 && echo line2`
2. Observer que les lignes apparaissent une par une dans le terminal SSE
3. Vérifier dans l'onglet Network du browser que des événements SSE arrivent progressivement
4. Tester le tlog avec un hôte SSH configuré
