package fr.arthurbr02.deploymanager.dto.host;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HostRequest(
    @NotBlank 
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nom invalide")
    String name,

    @NotBlank 
    @Size(min = 7, max = 45)
    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$", message = "IP invalide")
    String ip,

    @NotBlank 
    @Size(min = 1, max = 255)
    @Pattern(regexp = "^([a-zA-Z0-9-]+\\.)*[a-zA-Z0-9-]+\\.[a-z]{2,10}$", message = "Domaine invalide")
    String domain,

    @Size(max = 1000)
    String deploymentCommand,

    @Size(max = 1000)
    String generateCommand,

    @Size(max = 1000)
    String deliverCommand,

    @Size(max = 1000)
    String tlogCommand,

    @Size(max = 1000)
    String rollbackCommand,

    @Size(max = 500)
    String healthcheckUrl,

    @Size(max = 255)
    String dumpFolder,

    Integer defaultTimeout
) {}
