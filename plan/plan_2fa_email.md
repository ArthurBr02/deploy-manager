# Plan : Double authentification par email (2FA)

## Context

Ajouter une 2FA par code email pour renforcer la sécurité des connexions. La 2FA est activée par défaut via un paramètre admin, mais silencieusement bypassée si le SMTP n'est pas configuré. L'utilisateur peut marquer un appareil comme "sûr" pour 30 jours. Un mécanisme anti-brute force invalide le code après 5 tentatives échouées.

---

## Décisions de design

| Sujet | Choix |
|---|---|
| SMTP non configuré | Bypass silencieux (login normal) |
| Brute force | 5 tentatives → code invalidé, recommencer depuis le login |
| Durée appareil de confiance | 30 jours |
| Périmètre | Tous les utilisateurs (ADMIN inclus) |
| Révocation | Utilisateur depuis Profil + Admin depuis page utilisateur |
| Flow API | `/auth/login` → `MFA_REQUIRED` + `challengeId`, puis `/auth/verify-mfa` |
| Format du code | 6 chiffres numériques |
| Paramètre admin | `two_factor_enabled` (défaut: `true`) |

---

## Phase 1 — Base de données (Flyway)

### V20__add_mfa_tables.sql

```sql
-- Codes MFA en attente de validation
CREATE TABLE mfa_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    challenge_id UUID NOT NULL UNIQUE,
    code_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Appareils de confiance
CREATE TABLE trusted_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    ip_address VARCHAR(50),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Paramètre admin : activation 2FA
INSERT INTO app_config (key, value) VALUES ('two_factor_enabled', 'true')
ON CONFLICT (key) DO NOTHING;
```

---

## Phase 2 — Backend

### Nouvelles entités JPA

**`MfaCode.java`** — `entity/MfaCode.java`
- `id` (UUID)
- `userId` (UUID)
- `challengeId` (UUID, unique)
- `codeHash` (String)
- `attempts` (int)
- `expiresAt` (Instant)
- `createdAt` (Instant)

**`TrustedDevice.java`** — `entity/TrustedDevice.java`
- `id` (UUID)
- `userId` (UUID)
- `tokenHash` (String)
- `name` (String)
- `ipAddress` (String)
- `expiresAt` (Instant)
- `createdAt` (Instant)

### Nouveaux repositories

- `MfaCodeRepository` : `findByChallengeId()`, `deleteByUserId()`, `deleteByExpiresAtBefore()` (cleanup)
- `TrustedDeviceRepository` : `findByUserId()`, `findById()`, `deleteByExpiresAtBefore()`

### Nouveaux DTOs

- `MfaRequiredResponse` : `status="MFA_REQUIRED"`, `challengeId` (UUID)
- `VerifyMfaRequest` : `challengeId` (UUID), `code` (String 6 chiffres), `trustDevice` (boolean)
- `TrustedDeviceDto` : `id`, `name`, `ipAddress`, `createdAt`, `expiresAt`

Modifier `LoginResponse` pour pouvoir retourner soit les tokens (login complet) soit `MFA_REQUIRED`.

### `AppConfigService` — ajout clé

Ajouter `two_factor_enabled` aux clés reconnues dans `AppConfigService` (fichier : `service/AppConfigService.java`).

### `MfaService.java` — nouveau service

```
service/MfaService.java
```

Méthodes :
- `isMfaRequired(User user, HttpServletRequest request) : boolean`
  - Retourne `false` si `two_factor_enabled=false` OU si SMTP non configuré OU si cookie trusted device valide
- `initiateMfa(User user) : MfaRequiredResponse`
  - Génère code 6 chiffres, hash BCrypt, persiste `MfaCode`, envoie email via `MailService`, retourne `challengeId`
- `verifyMfa(VerifyMfaRequest req, HttpServletResponse response) : void` (ou lève exception)
  - Charge `MfaCode` par `challengeId`
  - Vérifie non expiré (10 min)
  - Vérifie `attempts < 5`, sinon supprime + lève `MfaBlockedException`
  - Hash et compare le code
  - Si invalide : incrémente `attempts`, si `attempts >= 5` → supprime + lève exception
  - Si valide : supprime `MfaCode`, si `trustDevice=true` → appelle `createTrustedDevice()`
- `createTrustedDevice(UUID userId, HttpServletRequest req, HttpServletResponse resp)`
  - Génère token UUID aléatoire, hash BCrypt, persiste `TrustedDevice` (expires_at = now+30j)
  - Set cookie HTTP-only `trusted_device` avec `maxAge=30j`
- `isDeviceTrusted(UUID userId, HttpServletRequest req) : boolean`
  - Extrait cookie `trusted_device`, cherche en DB par userId, compare hash
- `getUserTrustedDevices(UUID userId) : List<TrustedDeviceDto>`
- `revokeTrustedDevice(UUID deviceId, UUID requestingUserId)`
  - Vérifie que le device appartient à l'utilisateur (ou que requestingUserId est ADMIN)
- `revokeAllTrustedDevices(UUID userId)` — pour usage admin
- `@Scheduled` cleanup : supprimer `MfaCode` et `TrustedDevice` expirés

### `AuthService.java` — modifications

Fichier : `service/AuthService.java`

Modifier `login()` :
```
1. Valider email/password (inchangé)
2. Appeler mfaService.isMfaRequired(user, request)
3. Si true → return mfaService.initiateMfa(user)  [retourne MfaRequiredResponse]
4. Si false → flow normal (génère tokens, set cookie)
```

Ajouter `verifyMfaAndLogin(VerifyMfaRequest req, HttpServletRequest request, HttpServletResponse response)` :
```
1. Charger MfaCode par challengeId → récupérer userId
2. Appeler mfaService.verifyMfa(req, response)
3. Charger User, générer tokens, set cookies (flow login normal)
4. Retourner LoginResponse
```

### `AuthController.java` — nouveaux endpoints

Fichier : `controller/AuthController.java`

```
POST /api/auth/verify-mfa        ← VerifyMfaRequest → LoginResponse
GET  /api/auth/trusted-devices   ← @Auth → List<TrustedDeviceDto>
DELETE /api/auth/trusted-devices/{id}  ← @Auth → 204
```

### Endpoints admin (dans `AdminController` ou `UserController`)

```
GET    /api/admin/users/{userId}/trusted-devices         → List<TrustedDeviceDto>
DELETE /api/admin/users/{userId}/trusted-devices/{id}   → 204
DELETE /api/admin/users/{userId}/trusted-devices         → 204 (révoquer tout)
```

### `AuditConstants.java` — nouveaux événements

Fichier : `util/AuditConstants.java`

Ajouter :
- `MFA_CODE_SENT`
- `MFA_VERIFIED`
- `MFA_FAILED`
- `MFA_BLOCKED` (5 tentatives atteintes)
- `MFA_DEVICE_TRUSTED`
- `MFA_DEVICE_REVOKED`

### Email MFA

Via `MailService.sendEmail()` existant. Sujet : `"[Deploy Manager] Code de connexion"`. Corps en texte simple :
```
Votre code de connexion est : 482 931
Ce code est valable 10 minutes.
Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.
```

### SecurityConfig — aucune modification requise

`/api/auth/verify-mfa` doit être ajouté aux routes `PERMIT_ALL` comme les autres endpoints `/auth/**`.

---

## Phase 3 — Frontend

### Vue login — `views/auth/LoginView.vue`

Intercepter la réponse `MFA_REQUIRED` :
- Stocker `challengeId` dans Pinia ou état local
- Afficher le composant/vue MFA (étape 2 du login)

### Nouveau composant — `views/auth/MfaView.vue` (ou section dans LoginView)

- Input numérique 6 chiffres (ou 6 inputs séparés avec auto-focus)
- Checkbox "Faire confiance à cet appareil pendant 30 jours"
- Bouton "Vérifier"
- Message d'erreur avec tentatives restantes
- Message d'erreur "Code expiré ou trop de tentatives, veuillez vous reconnecter"

### Page Profil — `views/profile/ProfileView.vue`

Nouvelle section "Appareils de confiance" :
- Liste des appareils (nom, IP, date d'expiration)
- Bouton "Révoquer" par appareil

### Page admin utilisateur — `views/admin/UserDetailView.vue`

Nouvelle section "Appareils de confiance" :
- Liste + bouton "Révoquer" par appareil + bouton "Tout révoquer"

### Page paramètres admin — `views/admin/SettingsView.vue`

Ajouter un toggle `two_factor_enabled` dans la section sécurité :
- Label : "Double authentification (2FA)"
- Description : "Envoie un code par e-mail lors de la connexion (nécessite une configuration SMTP)"

---

## Phase 4 — Mise à jour README.md

À la fin de l'implémentation, mettre à jour `README.md` :

1. **Section Fonctionnalités > Sécurité** : ajouter la 2FA avec description du flow (code email, appareil de confiance 30j, anti-brute force 5 tentatives)
2. **Section Paramètres admin** : ajouter la clé `two_factor_enabled`
3. **Section API** : ajouter les nouveaux endpoints (`/auth/verify-mfa`, `/auth/trusted-devices`, endpoints admin)
4. **Section Structure du projet** : ajouter `MfaService`, `MfaCode`, `TrustedDevice` dans les listes

---

## Fichiers critiques à modifier

| Fichier | Action |
|---|---|
| `service/AuthService.java` | Modifier `login()`, ajouter `verifyMfaAndLogin()` |
| `controller/AuthController.java` | 3 nouveaux endpoints |
| `util/AuditConstants.java` | 6 nouvelles constantes |
| `views/admin/SettingsView.vue` | Toggle `two_factor_enabled` |
| `views/auth/LoginView.vue` | Intercepter `MFA_REQUIRED` |
| `README.md` | Mise à jour complète |

---

## Fichiers à créer

| Fichier | Type |
|---|---|
| `db/migration/V20__add_mfa_tables.sql` | Migration Flyway |
| `entity/MfaCode.java` | Entité JPA |
| `entity/TrustedDevice.java` | Entité JPA |
| `repository/MfaCodeRepository.java` | Repository |
| `repository/TrustedDeviceRepository.java` | Repository |
| `service/MfaService.java` | Service métier |
| `dto/MfaRequiredResponse.java` | DTO |
| `dto/VerifyMfaRequest.java` | DTO |
| `dto/TrustedDeviceDto.java` | DTO |
| `views/auth/MfaView.vue` | Composant Vue |

---

## Vérification end-to-end

1. **Sans SMTP** : activer 2FA dans settings → se déconnecter → login → vérifier que le login passe directement (bypass silencieux)
2. **Avec SMTP** : activer 2FA → login → vérifier réception du mail avec code 6 chiffres → saisir le code → vérifier accès
3. **Brute force** : saisir 5 mauvais codes → vérifier message "trop de tentatives" → vérifier que relancer le login permet de recevoir un nouveau code
4. **Appareil de confiance** : cocher la case → vérifier que le cookie `trusted_device` est posé → se déconnecter → re-login → vérifier que la 2FA n'est pas demandée
5. **Révocation** : depuis le Profil, révoquer l'appareil → se déconnecter → re-login → vérifier que la 2FA est redemandée
6. **Admin** : depuis page admin utilisateur, voir les appareils de confiance → révoquer → vérifier
7. **Expiration** : vérifier qu'un code de 10 min+ est rejeté ; qu'un trusted device de 30j+ est ignoré
