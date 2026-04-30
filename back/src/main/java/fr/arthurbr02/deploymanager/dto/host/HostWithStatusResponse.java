package fr.arthurbr02.deploymanager.dto.host;
import fr.arthurbr02.deploymanager.entity.Host;
import java.time.Instant;
import java.util.UUID;

public record HostWithStatusResponse(UUID id, String name, String ip, String domain, String sshUser, Integer sshPort,
                                      String deploymentCommand, String generateCommand, String deliverCommand, String tlogCommand,
                                      String rollbackCommand, String healthcheckUrl, String dumpCommand, String dumpFolder,
                                      boolean dumpEnabled, String dumpFilename, boolean isDumpAvailable,
                                      Integer defaultTimeout, String lastDeploymentStatus, Instant lastDeploymentAt,
                                      boolean canDeploy, boolean canEdit, boolean canExecute, boolean canDump) {
    public static HostWithStatusResponse from(Host h, String lastStatus, Instant lastAt, boolean canDeploy, boolean canEdit, boolean canExecute, boolean canDump, boolean isDumpAvailable) {
        return new HostWithStatusResponse(h.getId(), h.getName(), h.getIp(), h.getDomain(), h.getSshUser(), h.getSshPort(),
                h.getDeploymentCommand(), h.getGenerateCommand(), h.getDeliverCommand(), h.getTlogCommand(),
                h.getRollbackCommand(), h.getHealthcheckUrl(), h.getDumpCommand(), h.getDumpFolder(),
                h.isDumpEnabled(), h.getDumpFilename(), isDumpAvailable,
                h.getDefaultTimeout(), lastStatus, lastAt, canDeploy, canEdit, canExecute, canDump);
    }
}
