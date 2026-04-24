package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.PersonalAccessToken;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.PersonalAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService {
    private final PersonalAccessTokenRepository repository;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Transactional
    public Map.Entry<String, PersonalAccessToken> createToken(User user, String name, LocalDateTime expiresAt) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String plainToken = base64Encoder.encodeToString(randomBytes);

        PersonalAccessToken pat = PersonalAccessToken.builder()
                .user(user)
                .name(name)
                .token(passwordEncoder.encode(plainToken))
                .expiresAt(expiresAt)
                .build();

        PersonalAccessToken saved = repository.save(pat);
        return Map.entry(plainToken, saved);
    }

    public List<PersonalAccessToken> getUserTokens(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Transactional
    public void deleteToken(UUID tokenId, UUID userId) {
        repository.findById(tokenId).ifPresent(token -> {
            if (token.getUser().getId().equals(userId)) {
                repository.delete(token);
            }
        });
    }

    @Transactional
    public Optional<User> validateToken(String tokenValue) {
        // Since we store hashes, we must find by user or iterate if we don't have a prefix.
        // But the repository currently finds by exact token.
        // Improvement: Store a prefix or use a specific format to avoid full table scan.
        // For now, we fetch all active tokens and check.
        return repository.findAll().stream()
                .filter(token -> token.getExpiresAt() == null || token.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(token -> passwordEncoder.matches(tokenValue, token.getToken()))
                .findFirst()
                .map(token -> {
                    token.setLastUsedAt(LocalDateTime.now());
                    repository.save(token);
                    return token.getUser();
                });
    }
}
