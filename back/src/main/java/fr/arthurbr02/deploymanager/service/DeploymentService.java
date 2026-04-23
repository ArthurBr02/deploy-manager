package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.deployment.DeploymentRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentResponse;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentStatsResponse;
import fr.arthurbr02.deploymanager.entity.*;
import fr.arthurbr02.deploymanager.enums.*;
import fr.arthurbr02.deploymanager.exception.ForbiddenException;
import fr.arthurbr02.deploymanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final HostRepository hostRepository;
    private final UserHostPermissionRepository permissionRepository;
    private final AppConfigService configService;

    @Value("${app.log-dir:./logs/deployments}")
    private String logDir;

    // Map of deploymentId -> running Process
    private final Map<UUID, Process> runningProcesses = new ConcurrentHashMap<>();
    // Map of deploymentId -> list of SSE emitters (log streaming)
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    // Global emitters for deployment status events: emitter + allowed host IDs (null = admin, all hosts)
    private record EventEmitter(SseEmitter emitter, Set<UUID> allowedHostIds) {}
    private final List<EventEmitter> globalEmitters = new CopyOnWriteArrayList<>();

    @Transactional
    public DeploymentResponse launch(UUID hostId, DeploymentRequest req, User currentUser) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(hostId)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));

        if (currentUser.getRole() != Role.ADMIN) {
            UserHostPermission perm = permissionRepository.findByUserIdAndHostId(currentUser.getId(), hostId)
                    .orElseThrow(() -> new ForbiddenException("Accès refusé"));
            if (!perm.isCanDeploy()) throw new ForbiddenException("Permission insuffisante");
        }

        if (deploymentRepository.existsByHostIdAndStatus(hostId, DeploymentStatus.IN_PROGRESS)) {
            throw new RuntimeException("Un déploiement est déjà en cours sur cet hôte");
        }

        String command = resolveCommand(host, req.type());
        if (command == null || command.isBlank()) {
            throw new RuntimeException("Aucune commande définie pour ce type de déploiement");
        }

        String resolved = replaceVariables(command, host);

        Deployment deployment = Deployment.builder()
                .hostId(hostId)
                .userId(currentUser.getId())
                .type(req.type())
                .status(DeploymentStatus.IN_PROGRESS)
                .timeout(req.timeout())
                .build();
        deployment = deploymentRepository.save(deployment);
        final UUID deploymentId = deployment.getId();

        String logFile = logDir + "/" + deploymentId + ".log";
        deployment.setLogFilePath(logFile);
        deploymentRepository.save(deployment);

        final Deployment finalDeployment = deployment;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcastStatusEvent(hostId, deploymentId, DeploymentStatus.IN_PROGRESS);
                CompletableFuture.runAsync(() -> runDeployment(finalDeployment, resolved, logFile));
            }
        });

        return DeploymentResponse.from(loadWithJoins(deployment));
    }

    private void runDeployment(Deployment deployment, String command, String logFile) {
        UUID deploymentId = deployment.getId();
        DeploymentStatus finalStatus = DeploymentStatus.SUCCESS;
        try {
            Files.createDirectories(Paths.get(logDir));
            String serverOs = configService.get("server_os", "linux");
            String shellBin, shellArg;
            if ("windows".equalsIgnoreCase(serverOs)) {
                shellBin = configService.get("shell_windows_bin", "cmd.exe");
                shellArg = configService.get("shell_windows_arg", "/c");
            } else {
                shellBin = configService.get("shell_linux_bin", "/bin/sh");
                shellArg = configService.get("shell_linux_arg", "-c");
            }
            ProcessBuilder pb = new ProcessBuilder(shellBin, shellArg, command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            runningProcesses.put(deploymentId, process);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String logLine = line + "\n";
                    writer.write(logLine);
                    writer.flush();
                    broadcastLog(deploymentId, logLine);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) finalStatus = DeploymentStatus.FAILURE;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finalStatus = DeploymentStatus.CANCELLED;
        } catch (Exception e) {
            log.error("Deployment {} failed", deploymentId, e);
            finalStatus = DeploymentStatus.FAILURE;
            broadcastLog(deploymentId, "[ERROR] " + e.getMessage() + "\n");
        } finally {
            runningProcesses.remove(deploymentId);
            persistFinalStatus(deploymentId, finalStatus, logFile);
        }
    }

    private void persistFinalStatus(UUID deploymentId, DeploymentStatus status, String logFile) {
        deploymentRepository.findById(deploymentId).ifPresent(d -> {
            if (d.getStatus() == DeploymentStatus.IN_PROGRESS) {
                d.setStatus(status);
                d.setFinishedAt(LocalDateTime.now());
                d.setLogs(readLogFile(logFile));
                deploymentRepository.save(d);
                broadcastStatusEvent(d.getHostId(), deploymentId, status);
            }
            broadcastLog(deploymentId, null);
            closeEmitters(deploymentId);
        });
    }

    @Transactional
    public DeploymentResponse cancel(UUID deploymentId, User currentUser) {
        Deployment d = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new RuntimeException("Déploiement introuvable"));
        if (d.getStatus() != DeploymentStatus.IN_PROGRESS) {
            throw new RuntimeException("Le déploiement n'est pas en cours");
        }
        if (currentUser.getRole() != Role.ADMIN && !d.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("Non autorisé");
        }
        Process p = runningProcesses.get(deploymentId);
        if (p != null) {
            p.destroyForcibly();
        }
        d.setStatus(DeploymentStatus.CANCELLED);
        String logs = readLogFile(d.getLogFilePath());
        d.setLogs(logs);
        deploymentRepository.save(d);
        broadcastStatusEvent(d.getHostId(), deploymentId, DeploymentStatus.CANCELLED);
        broadcastLog(deploymentId, null);
        closeEmitters(deploymentId);
        return DeploymentResponse.from(loadWithJoins(d));
    }

    public SseEmitter streamLogs(UUID deploymentId, User currentUser) {
        SseEmitter emitter = new SseEmitter(0L);
        Optional<Deployment> opt = deploymentRepository.findById(deploymentId);
        if (opt.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("Déploiement introuvable"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        Deployment d = opt.get();

        if (d.getStatus() != DeploymentStatus.IN_PROGRESS) {
            try {
                if (d.getLogs() != null) {
                    emitter.send(SseEmitter.event().name("log").data(d.getLogs()));
                }
                emitter.send(SseEmitter.event().name("end").data(d.getStatus().name()));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Send accumulated logs from file
        String accumulated = readLogFile(d.getLogFilePath());
        if (accumulated != null && !accumulated.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("log").data(accumulated));
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        emitters.computeIfAbsent(deploymentId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(deploymentId, emitter));
        emitter.onTimeout(() -> removeEmitter(deploymentId, emitter));
        emitter.onError(e -> removeEmitter(deploymentId, emitter));
        return emitter;
    }

    private void broadcastLog(UUID deploymentId, String line) {
        List<SseEmitter> list = emitters.get(deploymentId);
        if (list == null) return;
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                if (line == null) {
                    emitter.send(SseEmitter.event().name("end").data("done"));
                    emitter.complete();
                    dead.add(emitter);
                } else {
                    emitter.send(SseEmitter.event().name("log").data(line));
                }
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    private void removeEmitter(UUID deploymentId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(deploymentId);
        if (list != null) list.remove(emitter);
    }

    private void closeEmitters(UUID deploymentId) {
        List<SseEmitter> list = emitters.remove(deploymentId);
        if (list != null) list.forEach(e -> { try { e.complete(); } catch (Exception ignored) {} });
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkTimeouts() {
        deploymentRepository.findAll().stream()
                .filter(d -> d.getStatus() == DeploymentStatus.IN_PROGRESS
                        && d.getTimeout() > 0
                        && d.getCreatedAt().plusMinutes(d.getTimeout()).isBefore(LocalDateTime.now()))
                .forEach(d -> {
                    Process p = runningProcesses.get(d.getId());
                    if (p != null) p.destroyForcibly();
                    d.setStatus(DeploymentStatus.FAILURE);
                    String logs = readLogFile(d.getLogFilePath());
                    d.setLogs((logs != null ? logs : "") + "\n[TIMEOUT] Déploiement annulé après " + d.getTimeout() + " minutes");
                    deploymentRepository.save(d);
                    broadcastStatusEvent(d.getHostId(), d.getId(), DeploymentStatus.FAILURE);
                    broadcastLog(d.getId(), null);
                    closeEmitters(d.getId());
                });
    }

    @Transactional(readOnly = true)
    public DeploymentStatsResponse getStats(String period) {
        LocalDateTime since = switch (period == null ? "" : period) {
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d"  -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default    -> null;
        };

        long total, success, failure, inProgress;
        if (since != null) {
            total      = deploymentRepository.countByCreatedAtAfter(since);
            success    = deploymentRepository.countByCreatedAtAfterAndStatus(since, DeploymentStatus.SUCCESS);
            failure    = deploymentRepository.countByCreatedAtAfterAndStatus(since, DeploymentStatus.FAILURE);
            inProgress = deploymentRepository.countByCreatedAtAfterAndStatus(since, DeploymentStatus.IN_PROGRESS);
        } else {
            total      = deploymentRepository.count();
            success    = deploymentRepository.countByStatus(DeploymentStatus.SUCCESS);
            failure    = deploymentRepository.countByStatus(DeploymentStatus.FAILURE);
            inProgress = deploymentRepository.countByStatus(DeploymentStatus.IN_PROGRESS);
        }

        Double medianSec = deploymentRepository.medianDurationSeconds(since);
        String medianDuration = formatMedianDuration(medianSec);

        return new DeploymentStatsResponse(total, success, failure, inProgress, medianDuration);
    }

    private String formatMedianDuration(Double seconds) {
        if (seconds == null || seconds == 0) return "—";
        long s = seconds.longValue();
        return String.format("%d min %02d s", s / 60, s % 60);
    }

    public Page<DeploymentResponse> findAll(User currentUser, UUID hostId, String status, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Deployment> spec = buildSpec(currentUser, hostId, status, type);
        return deploymentRepository.findAll(spec, pageable).map(d -> DeploymentResponse.from(loadWithJoins(d)));
    }

    private Specification<Deployment> buildSpec(User user, UUID hostId, String status, String type) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (user.getRole() != Role.ADMIN) {
                List<UUID> accessibleHostIds = permissionRepository.findByUserId(user.getId()).stream()
                        .map(UserHostPermission::getHostId).collect(Collectors.toList());
                predicates.add(root.get("hostId").in(accessibleHostIds));
            }
            if (hostId != null) predicates.add(cb.equal(root.get("hostId"), hostId));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), DeploymentStatus.valueOf(status)));
            if (type != null && !type.isBlank()) predicates.add(cb.equal(root.get("type"), DeploymentType.valueOf(type)));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private String resolveCommand(Host host, DeploymentType type) {
        return switch (type) {
            case DEPLOY -> (host.getDeploymentCommand() != null && !host.getDeploymentCommand().isBlank())
                    ? host.getDeploymentCommand()
                    : configService.get("default_deploy_command", "sh /root/{host}/liv.sh");
            case GENERATE -> host.getGenerateCommand();
            case DELIVER -> host.getDeliverCommand();
        };
    }

    private String replaceVariables(String command, Host host) {
        return command
                .replace("{host}", host.getName() != null ? host.getName() : "")
                .replace("{ip}", host.getIp() != null ? host.getIp() : "")
                .replace("{domain}", host.getDomain() != null ? host.getDomain() : "");
    }

    private String readLogFile(String path) {
        if (path == null) return null;
        try {
            return Files.readString(Paths.get(path));
        } catch (Exception e) {
            return null;
        }
    }

    public SseEmitter subscribeEvents(User user) {
        Set<UUID> allowedHostIds = null;
        if (user.getRole() != Role.ADMIN) {
            allowedHostIds = permissionRepository.findByUserId(user.getId()).stream()
                    .map(UserHostPermission::getHostId)
                    .collect(Collectors.toSet());
        }
        log.info("[SSE] new subscriber userId={} role={} allowedHosts={}", user.getId(), user.getRole(), allowedHostIds == null ? "ALL" : allowedHostIds);
        SseEmitter emitter = new SseEmitter(0L);
        EventEmitter entry = new EventEmitter(emitter, allowedHostIds);
        globalEmitters.add(entry);
        emitter.onCompletion(() -> { log.info("[SSE] emitter completed userId={}", user.getId()); globalEmitters.remove(entry); });
        emitter.onTimeout(() -> { log.info("[SSE] emitter timeout userId={}", user.getId()); globalEmitters.remove(entry); });
        emitter.onError(e -> { log.warn("[SSE] emitter error userId={}: {}", user.getId(), e.getMessage()); globalEmitters.remove(entry); });
        return emitter;
    }

    private void broadcastStatusEvent(UUID hostId, UUID deploymentId, DeploymentStatus status) {
        String data = String.format("{\"hostId\":\"%s\",\"deploymentId\":\"%s\",\"status\":\"%s\"}", hostId, deploymentId, status.name());
        log.info("[SSE] broadcastStatusEvent hostId={} deploymentId={} status={} totalEmitters={}", hostId, deploymentId, status, globalEmitters.size());
        List<EventEmitter> dead = new ArrayList<>();
        for (EventEmitter entry : globalEmitters) {
            if (entry.allowedHostIds() != null && !entry.allowedHostIds().contains(hostId)) {
                log.debug("[SSE] skipping emitter (no access to hostId={})", hostId);
                continue;
            }
            try {
                entry.emitter().send(SseEmitter.event().name("deployment.status").data(data));
                log.debug("[SSE] sent to emitter ok");
            } catch (Exception e) {
                log.warn("[SSE] emitter dead, removing: {}", e.getMessage());
                dead.add(entry);
            }
        }
        globalEmitters.removeAll(dead);
    }

    private Deployment loadWithJoins(Deployment d) {
        return deploymentRepository.findById(d.getId()).orElse(d);
    }
}
