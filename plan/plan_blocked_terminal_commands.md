# Plan: Blocked Terminal Commands

## Context
Add a configurable blocklist of regex patterns for SSH terminal commands. When a USER (not ADMIN) submits a blocked command, it is interrupted server-side (Ctrl+C sent to SSH), an error is shown in the terminal, the event is always audited with a distinct action, and all admin users are notified by email.

## Decisions
- Blocking applies to **USERs only** — ADMINs bypass all checks
- Matching: **full regex** per pattern (one per line), matched with `Pattern.compile(p).matcher(command).find()`
- Feedback: **error message** in terminal (`\r\n[BLOQUÉ] ...`) after sending `\x03` (Ctrl+C) to SSH
- Timing: **post-send block** — keystrokes still echo in real-time; on Enter, the accumulated command is checked and interrupted if blocked
- Email: **all non-deleted ADMIN users** via existing `MailService.sendEmail()`
- Email content: user name+email, host name, blocked command, timestamp, session context ID
- Audit: **always logged** (regardless of `audit_terminal_commands`) with action `TERMINAL_COMMAND_BLOCKED`, same terminal session display but distinct styling handled by frontend
- Storage: key `blocked_commands` in `app_config` table, value = newline-separated regex patterns

---

## Files to Modify

### 1. `back/src/main/java/fr/arthurbr02/deploymanager/util/AuditConstants.java`
Add constant:
```java
public static final String ACTION_TERMINAL_COMMAND_BLOCKED = "TERMINAL_COMMAND_BLOCKED";
```

### 2. `back/src/main/java/fr/arthurbr02/deploymanager/service/TerminalHandler.java`

**Inject:** `MailService mailService` (add to constructor via `@RequiredArgsConstructor`)

**Add map:** `private final Map<String, Boolean> sessionIsAdmin = new ConcurrentHashMap<>();`
Set it in `afterConnectionEstablished`:
```java
sessionIsAdmin.put(wsSession.getId(), user.getRole() == Role.ADMIN);
```
Clear it in `afterConnectionClosed`:
```java
sessionIsAdmin.remove(wsSession.getId());
```

**Change `accumulateCommand` signature** to accept `WebSocketSession wsSession` and `SshSession ssh`, then call it from `handleTextMessage` with those args.

**On Enter in `accumulateCommand`:** after building the command string:
```java
Boolean isAdmin = sessionIsAdmin.getOrDefault(sessionId, true);
if (!isAdmin && isCommandBlocked(command)) {
    handleBlockedCommand(sessionId, command, wsSession, ssh);
} else {
    logCommand(sessionId, command);
}
```

**Add `isCommandBlocked(String command)`:**
```java
private boolean isCommandBlocked(String command) {
    String patterns = configService.get("blocked_commands", "");
    if (patterns.isBlank()) return false;
    return Arrays.stream(patterns.split("\n"))
        .map(String::trim)
        .filter(p -> !p.isEmpty())
        .anyMatch(p -> {
            try {
                return Pattern.compile(p).matcher(command).find();
            } catch (PatternSyntaxException e) {
                log.warn("Invalid blocked command regex: {}", p);
                return false;
            }
        });
}
```

**Add `handleBlockedCommand(...)`:**
```java
private void handleBlockedCommand(String sessionId, String command, WebSocketSession wsSession, SshSession ssh) {
    try {
        // Interrupt the command already sent to SSH
        ssh.out.write(3); // Ctrl+C
        ssh.out.flush();
        // Show error in terminal
        wsSession.sendMessage(new TextMessage(
            "\r\n[31m[BLOQUÉ] Commande interdite par un administrateur.[0m\r\n"
        ));
    } catch (Exception e) {
        log.warn("Error sending block interrupt", e);
    }
    // Always audit
    UUID hostId = sessionHosts.get(sessionId);
    UUID userId = sessionUsers.get(sessionId);
    UUID contextId = sessionContextIds.get(sessionId);
    auditService.logAs(userId, AuditConstants.ENTITY_TERMINAL, hostId,
        AuditConstants.ACTION_TERMINAL_COMMAND_BLOCKED, contextId, null,
        Map.of("command", command));
    // Email all admins
    emailAdminsBlockedCommand(userId, hostId, command, contextId);
}
```

**Add `emailAdminsBlockedCommand(...)`:**
```java
private void emailAdminsBlockedCommand(UUID userId, UUID hostId, String command, UUID contextId) {
    try {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
        Host host = hostRepository.findByIdAndDeletedAtIsNull(hostId).orElse(null);
        List<User> admins = userRepository.findAllByRoleAndDeletedAtIsNull(Role.ADMIN);
        String userName = user != null ? user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")" : userId.toString();
        String hostName = host != null ? host.getName() : hostId.toString();
        String subject = "[Deploy Manager] Commande bloquée";
        String body = "Une commande interdite a été tentée.\n\n"
            + "Utilisateur : " + userName + "\n"
            + "Hôte : " + hostName + "\n"
            + "Commande : " + command + "\n"
            + "Session : " + (contextId != null ? contextId.toString() : "N/A") + "\n"
            + "Date : " + java.time.LocalDateTime.now() + "\n";
        admins.forEach(admin -> mailService.sendEmail(admin.getEmail(), subject, body));
    } catch (Exception e) {
        log.error("Failed to email admins for blocked command", e);
    }
}
```

### 3. `front/src/views/admin/SettingsView.vue`

**In `data()`:** add `blocked_commands: ''` to the `settings` object.

**In the template**, add a new card section after the "Général" card (before "Notifications"):
```html
<!-- Commandes bloquées -->
<div class="bg-white border border-warm-border rounded-xl p-4 sm:p-5 space-y-4 shadow-sm">
  <h2 class="font-semibold text-gray-900">Commandes Terminal bloquées</h2>
  <div>
    <label class="block text-sm font-medium text-gray-700 mb-1">Expressions régulières (une par ligne)</label>
    <textarea v-model="settings.blocked_commands" rows="6"
      class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
      placeholder="rm\s+-rf\ndd\s+if=\nmkfs" />
    <p class="text-xs text-gray-400 mt-1">Ces patterns regex sont testés contre les commandes saisies par les utilisateurs (rôle USER) dans le terminal SSH. Les ADMINs ne sont pas concernés.</p>
  </div>
</div>
```

No migration needed — `app_config` is a generic key-value store; `blocked_commands` is added automatically on first save.

---

### 4. `README.md`
Document the new feature:
- New config key `blocked_commands` (newline-separated regex patterns, USERs only)
- New audit action `TERMINAL_COMMAND_BLOCKED`
- Admin email notification on blocked command attempt
- Settings UI section "Commandes Terminal bloquées"

---

## Verification
1. **Start backend**, go to Settings, add pattern `rm\s+-rf` and save.
2. Open a terminal SSH as a **USER** and type `rm -rf /tmp/test` + Enter — expect `[BLOQUÉ]` message, command interrupted.
3. Open a terminal SSH as an **ADMIN** and type same command — expect it executes normally.
4. Check audit log — expect a `TERMINAL_COMMAND_BLOCKED` entry.
5. Check admin email inbox — expect notification with user, host, command, and session ID.
6. Test an invalid regex pattern (e.g. `[invalid`) — expect it is skipped gracefully (log warning, no crash).
