package fr.arthurbr02.deploymanager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${app.jwt.access-secret}")
    private String accessSecret;

    @Value("${app.jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${app.jwt.access-expiry}")
    private String accessExpiry;

    @Value("${app.jwt.refresh-expiry}")
    private String refreshExpiry;

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    private long parseDuration(String expiry) {
        if (expiry.endsWith("m")) return Duration.ofMinutes(Long.parseLong(expiry.replace("m", ""))).toMillis();
        if (expiry.endsWith("h")) return Duration.ofHours(Long.parseLong(expiry.replace("h", ""))).toMillis();
        if (expiry.endsWith("d")) return Duration.ofDays(Long.parseLong(expiry.replace("d", ""))).toMillis();
        return Long.parseLong(expiry);
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + parseDuration(accessExpiry)))
                .signWith(accessKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + parseDuration(refreshExpiry)))
                .signWith(refreshKey())
                .compact();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser().verifyWith(accessKey()).build().parseSignedClaims(token).getPayload();
    }

    public Claims validateRefreshToken(String token) {
        return Jwts.parser().verifyWith(refreshKey()).build().parseSignedClaims(token).getPayload();
    }

    public long getRefreshExpiryMillis() {
        return parseDuration(refreshExpiry);
    }

    public String generateSseToken(UUID userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", "sse")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 30000)) // 30 seconds
                .signWith(accessKey())
                .compact();
    }

    public Claims validateSseToken(String token) {
        Claims claims = validateAccessToken(token);
        if (!"sse".equals(claims.get("purpose"))) {
            throw new JwtException("Token invalide pour SSE");
        }
        return claims;
    }
}
