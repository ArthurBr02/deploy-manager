package fr.arthurbr02.deploymanager.dto.host;
import jakarta.validation.constraints.NotBlank;

public record HostRequest(
    @NotBlank String name,
    @NotBlank String ip,
    @NotBlank String domain,
    String deploymentCommand,
    String generateCommand,
    String deliverCommand,
    Integer defaultTimeout
) {}
