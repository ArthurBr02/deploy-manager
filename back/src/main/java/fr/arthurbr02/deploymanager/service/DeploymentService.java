package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.deployment.DeploymentRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentResponse;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentStatsResponse;
import fr.arthurbr02.deploymanager.entity.*;
import fr.arthurbr02.deploymanager.enums.*;
import fr.arthurbr02.deploymanager.exception.ForbiddenException;
import fr.arthurbr02.deploymanager.util.ShellUtil;
import fr.arthurbr02.deploymanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.criteria.JoinType;
import Predicate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private final UserRepository userRepository;
    private final UserHostPermissionRepository permissionRepository;
    private final AppConfigService configService;
    private final NotificationService notificationService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

            // Read stdout in a separate thread so we can enforce timeout independently
            CompletableFuture<Void> stdoutReader = CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                     BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String logLine = line + "\n";
                        writer.write(logLine);
                        writer.flush();
                        broadcastLog(deploymentId, logLine);
                    }
                } catch (IOException ignored) {}
            });

            int timeout = deployment.getTimeout();
            if (timeout > 0) {
                boolean finished = process.waitFor(timeout, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroyForcibly();
                    String timeoutMsg = "\n[TIMEOUT] Déploiement annulé après " + timeout + " minutes\n";
                    try (BufferedWriter w = new BufferedWriter(new FileWriter(logFile, true))) {
                        w.write(timeoutMsg);
                        w.flush();
                    } catch (IOException ignored) {}
                    broadcastLog(deploymentId, timeoutMsg);
                    finalStatus = DeploymentStatus.FAILURE;
                }
            } else {
                process.waitFor();
            }

            try { stdoutReader.join(); } catch (Exception ignored) {}

            if (finalStatus != DeploymentStatus.FAILURE && process.exitValue() != 0) {
                finalStatus = DeploymentStatus.FAILURE;
            }

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

                if (status == DeploymentStatus.SUCCESS) {
                    CompletableFuture.runAsync(() -> performHealthcheck(d));
                } else if (status == DeploymentStatus.FAILURE) {
                    notificationService.notifyDeploymentFailure(d);
                }
            }
            broadcastLog(deploymentId, null);
            closeEmitters(deploymentId);
        });
    }

    private void performHealthcheck(Deployment d) {
        Host host = hostRepository.findById(d.getHostId()).orElse(null);
        if (host == null) return;

        String url = host.getHealthcheckUrl();
        if (url == null || url.isBlank()) {
            if (host.getDomain() == null || host.getDomain().isBlank()) return;
            url = "https://{domain}";
        }
        url = ShellUtil.replaceVariables(url, host.getName(), host.getIp(), host.getDomain());
        // Remove single quotes added by ShellUtil.replaceVariables for URL
        url = url.replace("'", "");

        log.info("[Healthcheck] Checking {} for deployment {}", url, d.getId());
        broadcastLog(d.getId(), "\n[SYSTEM] Lancement du healthcheck sur " + url + "...\n");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                String msg = "[SYSTEM] Healthcheck RÉUSSI (Status: " + response.statusCode() + ")";
                log.info(msg);
                appendLogAndNotify(d, msg);
            } else {
                String msg = "[SYSTEM] Healthcheck ÉCHOUÉ (Status: " + response.statusCode() + ")";
                log.warn(msg);
                appendLogAndNotify(d, msg);
            }
        } catch (Exception e) {
            String msg = "[SYSTEM] Healthcheck ERREUR: " + e.getMessage();
            log.error(msg);
            appendLogAndNotify(d, msg);
        }
    }

    private void appendLogAndNotify(Deployment d, String message) {
        deploymentRepository.findById(d.getId()).ifPresent(deployment -> {
            String updatedLogs = (deployment.getLogs() != null ? deployment.getLogs() : "") + "\n" + message + "\n";
            deployment.setLogs(updatedLogs);
            deploymentRepository.save(deployment);
            broadcastLog(d.getId(), message + "\n");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(deployment.getLogFilePath(), true))) {
                writer.write("\n" + message + "\n");
            } catch (IOException e) {
                log.error("Failed to append healthcheck to log file", e);
            }
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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOrphanedDeployments() {
        List<Deployment> inProgress = deploymentRepository.findByStatus(DeploymentStatus.IN_PROGRESS);
        if (inProgress.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (Deployment d : inProgress) {
            log.warn("[Boot] Orphaned deployment {} on host {} marked as FAILURE", d.getId(), d.getHostId());
            d.setStatus(DeploymentStatus.FAILURE);
            d.setFinishedAt(now);
            d.setLogs((d.getLogs() != null ? d.getLogs() : "") + "\n[SYSTÈME] Déploiement interrompu par redémarrage du serveur");
            deploymentRepository.save(d);
        }
        log.info("[Boot] Cleaned up {} orphaned deployment(s)", inProgress.size());
    }

    @Transactional(readOnly = true)
    public DeploymentStatsResponse getStats(String period, UUID hostId, String type, User user) {
        LocalDateTime since = parsePeriod(period);
        DeploymentType deploymentType = parseType(type);

        List<UUID> accessibleHostIds = null;
        if (user.getRole() != Role.ADMIN) {
            if (hostId != null) {
                permissionRepository.findByUserIdAndHostId(user.getId(), hostId)
                        .orElseThrow(() -> new ForbiddenException("Accès refusé à cet hôte"));
            } else {
                accessibleHostIds = permissionRepository.findByUserId(user.getId()).stream()
                        .map(UserHostPermission::getHostId).collect(Collectors.toList());
                if (accessibleHostIds.isEmpty()) {
                    return new DeploymentStatsResponse(0, 0, 0, 0, "—");
                }
            }
        }

        long total      = deploymentRepository.count(statsSpec(since, hostId, deploymentType, null, accessibleHostIds));
        long success    = deploymentRepository.count(statsSpec(since, hostId, deploymentType, DeploymentStatus.SUCCESS, accessibleHostIds));
        long failure    = deploymentRepository.count(statsSpec(since, hostId, deploymentType, DeploymentStatus.FAILURE, accessibleHostIds));
        long inProgress = deploymentRepository.count(statsSpec(since, hostId, deploymentType, DeploymentStatus.IN_PROGRESS, accessibleHostIds));

        Double medianSec = accessibleHostIds != null
                ? deploymentRepository.medianDurationFilteredByHosts(since, accessibleHostIds, type)
                : deploymentRepository.medianDurationFiltered(since, hostId, type);
        String medianDuration = formatMedianDuration(medianSec);

        return new DeploymentStatsResponse(total, success, failure, inProgress, medianDuration);
    }

    private Specification<Deployment> statsSpec(LocalDateTime since, UUID hostId, DeploymentType type, DeploymentStatus status, List<UUID> accessibleHostIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (since != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), since));
            if (hostId != null) predicates.add(cb.equal(root.get("hostId"), hostId));
            else if (accessibleHostIds != null) predicates.add(root.get("hostId").in(accessibleHostIds));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private LocalDateTime parsePeriod(String period) {
        return switch (period == null ? "" : period) {
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d"  -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default    -> null;
        };
    }

    private DeploymentType parseType(String type) {
        if (type == null || type.isBlank()) return null;
        try { return DeploymentType.valueOf(type); } catch (Exception e) { return null; }
    }

    private String formatMedianDuration(Double seconds) {
        if (seconds == null || seconds == 0) return "—";
        long s = seconds.longValue();
        return String.format("%d min %02d s", s / 60, s % 60);
    }

    public Page<DeploymentResponse> findAll(User currentUser, UUID hostId, UUID userId, String search, String status, String type, String period, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Deployment> spec = buildSpec(currentUser, hostId, userId, search, status, type, period);
        return deploymentRepository.findAll(spec, pageable).map(d -> DeploymentResponse.from(loadWithJoins(d)));
    }

    public byte[] exportCsv(User currentUser, UUID hostId, String search, String status, String type, String period) {
        Specification<Deployment> spec = buildSpec(currentUser, hostId, null, search, status, type, period);
        Sort sort = Sort.by("createdAt").descending();
        List<Deployment> deployments = deploymentRepository.findAll(spec, sort);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Hôte,Utilisateur,Type,Statut,Créé le,Terminé le,Durée (s)\n");
        for (Deployment d : deployments) {
            Deployment full = loadWithJoins(d);
            String hostName = full.getHost() != null ? full.getHost().getName() : "";
            String userEmail = full.getUser() != null ? full.getUser().getEmail() : "";
            String duration = "";
            if (full.getCreatedAt() != null && full.getFinishedAt() != null) {
                long secs = Duration.between(full.getCreatedAt(), full.getFinishedAt()).getSeconds();
                duration = String.valueOf(secs);
            }
            sb.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    full.getId(),
                    escapeCsv(hostName),
                    escapeCsv(userEmail),
                    full.getType(),
                    full.getStatus(),
                    full.getCreatedAt() != null ? full.getCreatedAt() : "",
                    full.getFinishedAt() != null ? full.getFinishedAt() : "",
                    duration));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Specification<Deployment> buildSpec(User user, UUID hostId, UUID userId, String search, String status, String type, String period) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (user.getRole() != Role.ADMIN) {
                List<UUID> accessibleHostIds = permissionRepository.findByUserId(user.getId()).stream()
                        .map(UserHostPermission::getHostId).collect(Collectors.toList());
                predicates.add(root.get("hostId").in(accessibleHostIds));
            }
            LocalDateTime since = parsePeriod(period);
            if (since != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), since));
            if (hostId != null) predicates.add(cb.equal(root.get("hostId"), hostId));
            if (userId != null) predicates.add(cb.equal(root.get("userId"), userId));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), DeploymentStatus.valueOf(status)));
            if (type != null && !type.isBlank()) predicates.add(cb.equal(root.get("type"), DeploymentType.valueOf(type)));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                var hostJoin = root.join("host", JoinType.LEFT);
                var userJoin = root.join("user", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(hostJoin.get("name")), pattern),
                    cb.like(cb.lower(userJoin.get("email")), pattern),
                    cb.like(cb.lower(userJoin.get("firstName")), pattern),
                    cb.like(cb.lower(userJoin.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("id").as(String.class)), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String resolveCommand(Host host, DeploymentType type) {
        return switch (type) {
            case DEPLOY -> (host.getDeploymentCommand() != null && !host.getDeploymentCommand().isBlank())
                    ? host.getDeploymentCommand()
                    : configService.get("default_deploy_command", "sh /root/{host}/liv.sh");
            case GENERATE -> host.getGenerateCommand();
            case DELIVER -> host.getDeliverCommand();
            case ROLLBACK -> host.getRollbackCommand();
        };
    }

    private String replaceVariables(String command, Host host) {
        return ShellUtil.replaceVariables(command, host.getName(), host.getIp(), host.getDomain());
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
