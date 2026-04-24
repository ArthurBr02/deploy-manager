package fr.arthurbr02.deploymanager.dto.auth;

import fr.arthurbr02.deploymanager.entity.PersonalAccessToken;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TokenResponse {
    private UUID id;
    private String name;
    private String token; // Only populated on creation
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;

    public static TokenResponse from(PersonalAccessToken pat) {
        return TokenResponse.builder()
                .id(pat.getId())
                .name(pat.getName())
                .createdAt(pat.getCreatedAt())
                .expiresAt(pat.getExpiresAt())
                .lastUsedAt(pat.getLastUsedAt())
                .build();
    }
}
