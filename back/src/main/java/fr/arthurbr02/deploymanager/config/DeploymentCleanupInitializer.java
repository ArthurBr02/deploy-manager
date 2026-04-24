package fr.arthurbr02.deploymanager.config;

import fr.arthurbr02.deploymanager.entity.Deployment;
import fr.arthurbr02.deploymanager.enums.DeploymentStatus;
import fr.arthurbr02.deploymanager.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentCleanupInitializer {

    private final DeploymentRepository deploymentRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOrphanedDeployments() {
        log.info("Checking for orphaned deployments...");
        List<Deployment> orphaned = deploymentRepository.findAll().stream()
                .filter(d -> d.getStatus() == DeploymentStatus.IN_PROGRESS)
                .toList();

        if (!orphaned.isEmpty()) {
            log.info("Found {} orphaned deployments. Marking them as FAILURE.", orphaned.size());
            for (Deployment d : orphaned) {
                d.setStatus(DeploymentStatus.FAILURE);
                d.setFinishedAt(LocalDateTime.now());
                String currentLogs = d.getLogs() != null ? d.getLogs() : "";
                d.setLogs(currentLogs + "\n[SYSTEM] Déploiement marqué en échec suite au redémarrage du serveur.");
            }
            deploymentRepository.saveAll(orphaned);
        } else {
            log.info("No orphaned deployments found.");
        }
    }
}
