package fr.arthurbr02.deploymanager.dto.host;
import fr.arthurbr02.deploymanager.entity.Host;
import java.time.LocalDateTime;
import java.util.UUID;

public record HostResponse(UUID id, String name, String ip, String domain,
                            String deploymentCommand, String generateCommand, String deliverCommand,
                            Integer defaultTimeout, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static HostResponse from(Host h) {
        return new HostResponse(h.getId(), h.getName(), h.getIp(), h.getDomain(),
                h.getDeploymentCommand(), h.getGenerateCommand(), h.getDeliverCommand(),
                h.getDefaultTimeout(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
