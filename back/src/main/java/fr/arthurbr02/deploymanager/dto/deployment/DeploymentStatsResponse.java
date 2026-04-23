package fr.arthurbr02.deploymanager.dto.deployment;

public record DeploymentStatsResponse(
        long total,
        long success,
        long failure,
        long inProgress,
        String medianDuration
) {}
