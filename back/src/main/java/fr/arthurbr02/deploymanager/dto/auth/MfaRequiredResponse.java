package fr.arthurbr02.deploymanager.dto.auth;

import java.util.UUID;

public record MfaRequiredResponse(String status, UUID challengeId) implements AuthResponse {}
