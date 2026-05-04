package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.auth.MfaRequiredResponse;
import fr.arthurbr02.deploymanager.dto.auth.TrustedDeviceDto;
import fr.arthurbr02.deploymanager.dto.auth.VerifyMfaRequest;
import fr.arthurbr02.deploymanager.entity.MfaCode;
import fr.arthurbr02.deploymanager.entity.TrustedDevice;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.MfaCodeRepository;
import fr.arthurbr02.deploymanager.repository.TrustedDeviceRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final MfaCodeRepository mfaCodeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final AppConfigService configService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.cookie-secure}")
    private boolean cookieSecure;

    public boolean isMfaEnabled() {
        return configService.getBoolean("two_factor_enabled");
    }

    public boolean isSmtpConfigured() {
        String host = configService.get("smtp_host");
        return host != null && !host.isBlank();
    }

    public boolean isMfaRequired(UUID userId, HttpServletRequest request) {
        if (!isMfaEnabled() || !isSmtpConfigured()) return false;
        return !isDeviceTrusted(userId, request);
    }

    @Transactional
    public MfaRequiredResponse initiateMfa(User user) {
        mfaCodeRepository.deleteByUserId(user.getId());

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        UUID challengeId = UUID.randomUUID();

        mfaCodeRepository.save(MfaCode.builder()
                .userId(user.getId())
                .challengeId(challengeId)
                .codeHash(passwordEncoder.encode(code))
                .attempts(0)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build());

        String body = "Votre code de connexion est : " + code.substring(0, 3) + " " + code.substring(3) + "\n\n"
                + "Ce code est valable 10 minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.";
        mailService.sendEmail(user.getEmail(), "[Deploy Manager] Code de connexion", body);

        return new MfaRequiredResponse("MFA_REQUIRED", challengeId);
    }

    @Transactional
    public UUID verifyMfaAndGetUserId(VerifyMfaRequest req, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        MfaCode mfaCode = mfaCodeRepository.findByChallengeId(req.challengeId())
                .orElseThrow(() -> new RuntimeException("Challenge invalide ou expiré"));

        if (mfaCode.getExpiresAt().isBefore(Instant.now())) {
            mfaCodeRepository.delete(mfaCode);
            throw new RuntimeException("Code expiré, veuillez vous reconnecter");
        }

        if (mfaCode.getAttempts() >= 5) {
            mfaCodeRepository.delete(mfaCode);
            throw new RuntimeException("Trop de tentatives, veuillez vous reconnecter");
        }

        if (!passwordEncoder.matches(req.code(), mfaCode.getCodeHash())) {
            mfaCode.setAttempts(mfaCode.getAttempts() + 1);
            if (mfaCode.getAttempts() >= 5) {
                mfaCodeRepository.delete(mfaCode);
                throw new RuntimeException("Trop de tentatives, veuillez vous reconnecter");
            }
            mfaCodeRepository.save(mfaCode);
            int remaining = 5 - mfaCode.getAttempts();
            throw new RuntimeException("Code incorrect. " + remaining + " tentative(s) restante(s)");
        }

        UUID userId = mfaCode.getUserId();
        mfaCodeRepository.delete(mfaCode);

        if (req.trustDevice()) {
            createTrustedDevice(userId, httpReq, httpResp);
        }

        return userId;
    }

    public boolean isDeviceTrusted(UUID userId, HttpServletRequest request) {
        String cookieValue = extractTrustedDeviceCookie(request);
        if (cookieValue == null || !cookieValue.contains(":")) return false;

        String[] parts = cookieValue.split(":", 2);
        UUID deviceId;
        try {
            deviceId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        String rawToken = parts[1];

        return trustedDeviceRepository.findByIdAndUserId(deviceId, userId)
                .filter(d -> d.getExpiresAt().isAfter(Instant.now()))
                .map(d -> hashToken(rawToken).equals(d.getTokenHash()))
                .orElse(false);
    }

    @Transactional
    public void createTrustedDevice(UUID userId, HttpServletRequest req, HttpServletResponse resp) {
        String rawToken = UUID.randomUUID().toString();
        String userAgent = req.getHeader("User-Agent");
        String name = userAgent != null ? userAgent.substring(0, Math.min(255, userAgent.length())) : "Appareil inconnu";

        TrustedDevice device = trustedDeviceRepository.save(TrustedDevice.builder()
                .userId(userId)
                .tokenHash(hashToken(rawToken))
                .name(name)
                .ipAddress(getClientIp(req))
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build());

        String cookieValue = device.getId().toString() + ":" + rawToken;
        ResponseCookie cookie = ResponseCookie.from("trusted_device", cookieValue)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public List<TrustedDeviceDto> getUserTrustedDevices(UUID userId) {
        return trustedDeviceRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(d -> new TrustedDeviceDto(d.getId(), d.getName(), d.getIpAddress(), d.getCreatedAt(), d.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void revokeTrustedDevice(UUID deviceId, UUID requestingUserId, boolean isAdmin) {
        TrustedDevice device = trustedDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Appareil introuvable"));
        if (!isAdmin && !device.getUserId().equals(requestingUserId)) {
            throw new RuntimeException("Non autorisé");
        }
        trustedDeviceRepository.delete(device);
    }

    @Transactional
    public void revokeAllTrustedDevices(UUID userId) {
        trustedDeviceRepository.deleteByUserId(userId);
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cleanupExpired() {
        mfaCodeRepository.deleteByExpiresAtBefore(Instant.now());
        trustedDeviceRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private String extractTrustedDeviceCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "trusted_device".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
