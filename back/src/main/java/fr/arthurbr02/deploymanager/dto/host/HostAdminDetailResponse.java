package fr.arthurbr02.deploymanager.dto.host;

import fr.arthurbr02.deploymanager.entity.Host;
import java.time.Instant;
import java.util.UUID;

public record HostAdminDetailResponse(UUID id, String name, String ip, String domain, String sshUser, Integer sshPort,
                                       String deploymentCommand, String generateCommand, String deliverCommand,
                                       String tlogCommand, String rollbackCommand, String healthcheckUrl,
                                       String dumpCommand, String dumpFolder, boolean dumpEnabled, String dumpFilename,
                                       boolean isDumpAvailable, String dbPassword,
                                       Integer defaultTimeout, Instant createdAt, Instant updatedAt) {
    public static HostAdminDetailResponse from(Host h, boolean isDumpAvailable) {
        return new HostAdminDetailResponse(
                h.getId(), h.getName(), h.getIp(), h.getDomain(), h.getSshUser(), h.getSshPort(),
                h.getDeploymentCommand(), h.getGenerateCommand(), h.getDeliverCommand(), h.getTlogCommand(),
                h.getRollbackCommand(), h.getHealthcheckUrl(), h.getDumpCommand(), h.getDumpFolder(),
                h.isDumpEnabled(), h.getDumpFilename(), isDumpAvailable, h.getDbPassword(),
                h.getDefaultTimeout(), h.getCreatedAt(), h.getUpdatedAt()
        );
    }
}
