package fr.arthurbr02.deploymanager.dto.host;
import fr.arthurbr02.deploymanager.entity.Host;
import java.time.LocalDateTime;
import java.util.UUID;

public record HostWithStatusResponse(UUID id, String name, String ip, String domain, String sshUser, Integer sshPort,
                                      String deploymentCommand, String generateCommand, String deliverCommand, String tlogCommand,
                                      String rollbackCommand, String healthcheckUrl, String dumpFolder, boolean isDumpAvailable,
                                      Integer defaultTimeout, String lastDeploymentStatus, LocalDateTime lastDeploymentAt,
                                      boolean canDeploy, boolean canEdit, boolean canExecute) {
    public static HostWithStatusResponse from(Host h, String lastStatus, LocalDateTime lastAt, boolean canDeploy, boolean canEdit, boolean canExecute, boolean isDumpAvailable) {
        return new HostWithStatusResponse(h.getId(), h.getName(), h.getIp(), h.getDomain(), h.getSshUser(), h.getSshPort(),
                h.getDeploymentCommand(), h.getGenerateCommand(), h.getDeliverCommand(), h.getTlogCommand(),
                h.getRollbackCommand(), h.getHealthcheckUrl(), h.getDumpFolder(), isDumpAvailable,
                h.getDefaultTimeout(), lastStatus, lastAt, canDeploy, canEdit, canExecute);
    }
}
