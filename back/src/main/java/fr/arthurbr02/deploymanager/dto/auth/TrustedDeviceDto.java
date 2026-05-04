package fr.arthurbr02.deploymanager.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record TrustedDeviceDto(UUID id, String name, String ipAddress, Instant createdAt, Instant expiresAt) {}
