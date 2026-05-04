package fr.arthurbr02.deploymanager.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VerifyMfaRequest(
        @NotNull UUID challengeId,
        @NotBlank @Size(min = 6, max = 6) @Pattern(regexp = "\\d{6}") String code,
        boolean trustDevice
) {}
