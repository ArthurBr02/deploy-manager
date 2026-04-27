package fr.arthurbr02.deploymanager.dto.deployment;
import fr.arthurbr02.deploymanager.entity.Deployment;
import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(UUID id, UUID hostId, String hostName, UUID userId,
                                  String userFirstName, String userLastName, String userEmail, String userAvatar,
                                  String type, String status, String logs, Integer timeout, Instant createdAt) {
    public static DeploymentResponse from(Deployment d) {
        String hostName = d.getHost() != null ? d.getHost().getName() : null;
        String firstName = d.getUser() != null ? d.getUser().getFirstName() : null;
        String lastName = d.getUser() != null ? d.getUser().getLastName() : null;
        String email = d.getUser() != null ? d.getUser().getEmail() : null;
        String avatar = d.getUser() != null ? d.getUser().getAvatar() : null;
        return new DeploymentResponse(d.getId(), d.getHostId(), hostName, d.getUserId(),
                firstName, lastName, email, avatar, d.getType().name(), d.getStatus().name(),
                d.getLogs(), d.getTimeout(), d.getCreatedAt());
    }
}
