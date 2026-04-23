package fr.arthurbr02.deploymanager.dto.deployment;
import fr.arthurbr02.deploymanager.enums.DeploymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
public record DeploymentRequest(@NotNull DeploymentType type, @Min(0) int timeout) {}
