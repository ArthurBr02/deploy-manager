package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.auth.*;
import fr.arthurbr02.deploymanager.exception.ForbiddenException;
import fr.arthurbr02.deploymanager.entity.PasswordResetToken;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.PasswordResetTokenRepository;
import fr.arthurbr02.deploymanager.repository.UserRepository;
import fr.arthurbr02.deploymanager.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final PersonalAccessTokenService patService;

    @Value("${app.security.cookie-secure}")
    private boolean cookieSecure;

    @Transactional(readOnly = true)
    public User validateSseToken(String token) {
        // 1. Try as JWT SSE token or Access Token
        try {
            Claims claims = jwtUtil.validateAccessToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            return userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        } catch (Exception ignored) {}

        // 2. Try as Personal Access Token (PAT)
        return patService.validateToken(token)
                .orElseThrow(() -> new ForbiddenException("Token SSE, Access ou PAT invalide ou expiré"));
    }

    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(req.email())
                .orElseThrow(() -> new RuntimeException("Identifiants invalides"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new RuntimeException("Identifiants invalides");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        setRefreshCookie(response, refreshToken);
        UserInfo info = new UserInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name(), user.getAvatar());
        return new LoginResponse(accessToken, info);
    }

    @Transactional
    public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);
        if (refreshToken == null) throw new RuntimeException("Pas de refresh token");
        try {
            Claims claims = jwtUtil.validateRefreshToken(refreshToken);
            UUID userId = UUID.fromString(claims.getSubject());
            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
            String newAccess = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
            String newRefresh = jwtUtil.generateRefreshToken(user.getId());
            setRefreshCookie(response, newRefresh);
            return new RefreshResponse(newAccess);
        } catch (JwtException e) {
            throw new RuntimeException("Refresh token invalide");
        }
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmailAndDeletedAtIsNull(req.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            resetTokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .build());
            // TODO: send email with reset link
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken prt = resetTokenRepository.findByTokenAndUsedFalse(req.token())
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expiré");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(prt.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        prt.setUsed(true);
        resetTokenRepository.save(prt);
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtUtil.getRefreshExpiryMillis() / 1000));
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }
}
