package fr.arthurbr02.deploymanager.dto.host;
import fr.arthurbr02.deploymanager.entity.Host;
import java.time.LocalDateTime;
import java.util.UUID;

public record HostWithStatusResponse(UUID id, String name, String ip, String domain,
                                      String deploymentCommand, String generateCommand, String deliverCommand,
                                      Integer defaultTimeout, String lastDeploymentStatus, LocalDateTime lastDeploymentAt,
                                      boolean canDeploy, boolean canEdit) {
    public static HostWithStatusResponse from(Host h, String lastStatus, LocalDateTime lastAt, boolean canDeploy, boolean canEdit) {
        return new HostWithStatusResponse(h.getId(), h.getName(), h.getIp(), h.getDomain(),
                h.getDeploymentCommand(), h.getGenerateCommand(), h.getDeliverCommand(),
                h.getDefaultTimeout(), lastStatus, lastAt, canDeploy, canEdit);
    }
}
