package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.PersonalAccessToken;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.PersonalAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService {
    private final PersonalAccessTokenRepository repository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Transactional
    public String createToken(User user, String name, LocalDateTime expiresAt) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = base64Encoder.encodeToString(randomBytes);

        PersonalAccessToken pat = PersonalAccessToken.builder()
                .user(user)
                .name(name)
                .token(token) // In a production environment, we should store a hash of the token
                .expiresAt(expiresAt)
                .build();

        repository.save(pat);
        return token;
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
        return repository.findByToken(tokenValue)
                .filter(token -> token.getExpiresAt() == null || token.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(token -> {
                    token.setLastUsedAt(LocalDateTime.now());
                    repository.save(token);
                    return token.getUser();
                });
    }
}
