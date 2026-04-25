package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.host.*;
import fr.arthurbr02.deploymanager.entity.*;
import fr.arthurbr02.deploymanager.enums.Role;
import fr.arthurbr02.deploymanager.exception.ForbiddenException;
import fr.arthurbr02.deploymanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostService {

    private final HostRepository hostRepository;
    private final UserRepository userRepository;
    private final UserHostPermissionRepository permissionRepository;
    private final DeploymentRepository deploymentRepository;
    private final AppConfigService configService;
    private final AuditService auditService;
    private final MailService mailService;

    public List<HostWithStatusResponse> findAll(User currentUser) {
        List<Host> hosts = currentUser.getRole() == Role.ADMIN
                ? hostRepository.findAllByDeletedAtIsNull()
                : hostRepository.findAccessibleByUserId(currentUser.getId());

        List<UserHostPermission> permissions = currentUser.getRole() == Role.ADMIN
                ? Collections.emptyList()
                : permissionRepository.findByUserId(currentUser.getId());

        Map<UUID, UserHostPermission> permMap = permissions.stream()
                .collect(Collectors.toMap(UserHostPermission::getHostId, p -> p));

        return hosts.stream().map(h -> {
            Deployment last = deploymentRepository.findLastByHostId(h.getId()).orElse(null);
            String lastStatus = null;
            LocalDateTime lastAt = null;
            if (last != null) {
                lastStatus = last.getStatus().name();
                lastAt = last.getCreatedAt();
            }
            boolean canDeploy = currentUser.getRole() == Role.ADMIN || (permMap.containsKey(h.getId()) && permMap.get(h.getId()).isCanDeploy());
            boolean canEdit = currentUser.getRole() == Role.ADMIN || (permMap.containsKey(h.getId()) && permMap.get(h.getId()).isCanEdit());
            boolean canExecute = currentUser.getRole() == Role.ADMIN || (permMap.containsKey(h.getId()) && permMap.get(h.getId()).isCanExecute());
            return HostWithStatusResponse.from(h, lastStatus, lastAt, canDeploy, canEdit, canExecute, isDumpAvailable(h));
        }).collect(Collectors.toList());
    }

    public HostWithStatusResponse findById(UUID id, User currentUser) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));

        UserHostPermission perm = null;
        if (currentUser.getRole() != Role.ADMIN) {
            perm = permissionRepository.findByUserIdAndHostId(currentUser.getId(), id)
                    .orElseThrow(() -> new ForbiddenException("Accès refusé"));
        }

        Deployment last = deploymentRepository.findLastByHostId(id).orElse(null);
        String lastStatus = last != null ? last.getStatus().name() : null;
        LocalDateTime lastAt = last != null ? last.getCreatedAt() : null;

        boolean canDeploy = currentUser.getRole() == Role.ADMIN || (perm != null && perm.isCanDeploy());
        boolean canEdit = currentUser.getRole() == Role.ADMIN || (perm != null && perm.isCanEdit());
        boolean canExecute = currentUser.getRole() == Role.ADMIN || (perm != null && perm.isCanExecute());

        return HostWithStatusResponse.from(host, lastStatus, lastAt, canDeploy, canEdit, canExecute, isDumpAvailable(host));
    }

    private boolean isDumpAvailable(Host h) {
        String folder = h.getDumpFolder();
        if (folder == null || folder.isBlank()) {
            folder = configService.get("default_dump_folder", "/var/www/dumps");
        }
        File file = new File(folder, h.getName() + ".sql");
        return file.exists() && file.isFile();
    }

    @Transactional
    public HostResponse create(HostRequest req) {
        Host host = Host.builder()
                .name(req.name())
                .ip(req.ip())
                .domain(req.domain())
                .sshUser(req.sshUser() != null && !req.sshUser().isBlank() ? req.sshUser() : "root")
                .sshPort(req.sshPort() != null ? req.sshPort() : 22)
                .deploymentCommand(req.deploymentCommand())
                .generateCommand(req.generateCommand())
                .deliverCommand(req.deliverCommand())
                .tlogCommand(req.tlogCommand())
                .rollbackCommand(req.rollbackCommand())
                .healthcheckUrl(req.healthcheckUrl())
                .dumpFolder(req.dumpFolder())
                .defaultTimeout(req.defaultTimeout())
                .build();
        host = hostRepository.save(host);
        auditService.log("Host", host.getId(), "CREATE", null, host.getName());
        return HostResponse.from(host, isDumpAvailable(host));
    }

    @Transactional
    public HostResponse update(UUID id, HostRequest req, User currentUser) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));
        if (currentUser.getRole() != Role.ADMIN) {
            UserHostPermission perm = permissionRepository.findByUserIdAndHostId(currentUser.getId(), id)
                    .orElseThrow(() -> new ForbiddenException("Accès refusé"));
            if (!perm.isCanEdit()) throw new ForbiddenException("Permission insuffisante");
        }
        String oldName = host.getName();
        host.setName(req.name());
        host.setIp(req.ip());
        host.setDomain(req.domain());
        host.setSshUser(req.sshUser() != null && !req.sshUser().isBlank() ? req.sshUser() : "root");
        host.setSshPort(req.sshPort() != null ? req.sshPort() : 22);
        host.setDeploymentCommand(req.deploymentCommand());
        host.setGenerateCommand(req.generateCommand());
        host.setDeliverCommand(req.deliverCommand());
        host.setTlogCommand(req.tlogCommand());
        host.setRollbackCommand(req.rollbackCommand());
        host.setHealthcheckUrl(req.healthcheckUrl());
        host.setDumpFolder(req.dumpFolder());
        host.setDefaultTimeout(req.defaultTimeout());
        host = hostRepository.save(host);
        auditService.log("Host", host.getId(), "UPDATE", oldName, host.getName());
        return HostResponse.from(host, isDumpAvailable(host));
    }

    public SseEmitter streamTlog(UUID hostId, User user) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(hostId)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));

        if (user.getRole() != Role.ADMIN) {
            permissionRepository.findByUserIdAndHostId(user.getId(), hostId)
                    .orElseThrow(() -> new ForbiddenException("Accès refusé"));
        }

        String sshUser = (host.getSshUser() != null && !host.getSshUser().isBlank()) ? host.getSshUser() : "root";
        int sshPort = (host.getSshPort() != null && host.getSshPort() > 0) ? host.getSshPort() : 22;
        
        String command = (host.getTlogCommand() != null && !host.getTlogCommand().isBlank())
                ? host.getTlogCommand()
                : configService.get("default_tlog_command", "ssh -p " + sshPort + " " + sshUser + "@{domain} tlog");

        String effectiveDomain = (host.getDomain() != null && !host.getDomain().isBlank()) ? host.getDomain() : host.getIp();
        String resolved = fr.arthurbr02.deploymanager.util.ShellUtil.replaceVariables(command, host.getName(), host.getIp(), effectiveDomain);

        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            Process process = null;
            try {
                String serverOs = configService.get("server_os", "linux");
                String shellBin, shellArg;
                if ("windows".equalsIgnoreCase(serverOs)) {
                    shellBin = configService.get("shell_windows_bin", "cmd.exe");
                    shellArg = configService.get("shell_windows_arg", "/c");
                } else {
                    shellBin = configService.get("shell_linux_bin", "/bin/sh");
                    shellArg = configService.get("shell_linux_arg", "-c");
                }

                ProcessBuilder pb = new ProcessBuilder(shellBin, shellArg, resolved);
                pb.redirectErrorStream(true);
                process = pb.start();
                final Process finalProcess = process;

                emitter.onCompletion(() -> {
                    log.info("[Tlog] Emitter completed, killing process for host {}", hostId);
                    finalProcess.destroyForcibly();
                });
                emitter.onTimeout(() -> {
                    log.info("[Tlog] Emitter timeout, killing process for host {}", hostId);
                    finalProcess.destroyForcibly();
                });
                emitter.onError(e -> {
                    log.warn("[Tlog] Emitter error, killing process for host {}: {}", hostId, e.getMessage());
                    finalProcess.destroyForcibly();
                });

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitter.send(SseEmitter.event().name("log").data(line + "\n"));
                    }
                }

                int exitCode = process.waitFor();
                emitter.send(SseEmitter.event().name("end").data("Exit code: " + exitCode));
                emitter.complete();

            } catch (Exception e) {
                log.error("Tlog error for host {}", hostId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
                if (process != null) process.destroyForcibly();
            }
        });

        return emitter;
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));
        host.setDeletedAt(LocalDateTime.now());
        host.setDeletedBy(currentUser.getId());
        hostRepository.save(host);
        auditService.log("Host", id, "DELETE", host.getName(), null);
    }

    @Transactional
    public void setPermission(UUID userId, PermissionRequest req) {
        UserHostPermission perm = permissionRepository.findByUserIdAndHostId(userId, req.hostId())
                .orElse(UserHostPermission.builder().userId(userId).hostId(req.hostId()).build());
        perm.setCanDeploy(req.canDeploy());
        perm.setCanEdit(req.canEdit());
        perm.setCanExecute(req.canExecute());
        if (!req.canDeploy() && !req.canEdit() && !req.canExecute()) {
            permissionRepository.deleteByUserIdAndHostId(userId, req.hostId());
        } else {
            permissionRepository.save(perm);
        }
    }

    public List<Map<String, Object>> getPermissionsForUser(UUID userId) {
        return permissionRepository.findByUserId(userId).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hostId", p.getHostId());
            m.put("hostName", p.getHost() != null ? p.getHost().getName() : null);
            m.put("canDeploy", p.isCanDeploy());
            m.put("canEdit", p.isCanEdit());
            m.put("canExecute", p.isCanExecute());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> importAnsibleFile(MultipartFile file) {
        int updated = 0;
        int created = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            Map<String, String> current = null;
            List<Map<String, String>> hosts = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("[") || line.startsWith("#")) {
                    if (current != null && current.containsKey("hostname")) hosts.add(current);
                    current = null;
                    continue;
                }
                String[] tokens = line.split("\\s+");
                if (!tokens[0].contains("=")) {
                    // First token is a hostname
                    if (current != null && current.containsKey("hostname")) hosts.add(current);
                    current = new LinkedHashMap<>();
                    current.put("hostname", tokens[0]);
                    // Parse any inline key=value pairs on the same line
                    for (int i = 1; i < tokens.length; i++) {
                        String[] kv = tokens[i].split("=", 2);
                        if (kv.length == 2) current.put(kv[0], kv[1].replaceAll("^\"|\"$", ""));
                    }
                } else if (current != null) {
                    // Continuation line with only key=value pairs
                    for (String part : tokens) {
                        String[] kv = part.split("=", 2);
                        if (kv.length == 2) current.put(kv[0], kv[1].replaceAll("^\"|\"$", ""));
                    }
                }
            }
            if (current != null && current.containsKey("hostname")) hosts.add(current);

            for (Map<String, String> h : hosts) {
                String hostname = h.get("hostname");
                if (!h.containsKey("ansible_host")) {
                    skipped++;
                    continue;
                }
                Optional<Host> existing = hostRepository.findByNameAndDeletedAtIsNull(hostname);
                if (existing.isPresent()) {
                    Host host = existing.get();
                    host.setIp(h.get("ansible_host"));
                    if (h.containsKey("domain_name")) host.setDomain(h.get("domain_name"));
                    hostRepository.save(host);
                    updated++;
                } else {
                    Host host = Host.builder()
                            .name(hostname)
                            .ip(h.get("ansible_host"))
                            .domain(h.getOrDefault("domain_name", null))
                            .build();
                    hostRepository.save(host);
                    created++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du parsing du fichier: " + e.getMessage());
        }
        return Map.of("updated", updated, "created", created, "skipped", skipped);
    }

    public Resource getDump(UUID id, User user) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));

        // Access check
        if (user.getRole() != Role.ADMIN) {
            permissionRepository.findByUserIdAndHostId(user.getId(), id)
                    .orElseThrow(() -> new ForbiddenException("Accès refusé"));
        }

        String folder = host.getDumpFolder();
        if (folder == null || folder.isBlank()) {
            folder = configService.get("default_dump_folder", "/var/www/dumps");
        }
        File file = new File(folder, host.getName() + ".sql");
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("Dump indisponible");
        }

        return new FileSystemResource(file);
    }

    public void requestDump(UUID id, User requester) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));

        // Access check
        if (requester.getRole() != Role.ADMIN) {
            permissionRepository.findByUserIdAndHostId(requester.getId(), id)
                    .orElseThrow(() -> new ForbiddenException("Accès refusé"));
        }

        List<User> admins = userRepository.findAllByRoleAndDeletedAtIsNull(Role.ADMIN);
        String subject = "[Deploy Manager] Demande de dump SQL : " + host.getName();
        String text = String.format("L'utilisateur %s %s (%s) demande un dump SQL pour l'hôte %s (%s).",
                requester.getFirstName(), requester.getLastName(), requester.getEmail(),
                host.getName(), host.getIp());

        for (User admin : admins) {
            mailService.sendEmail(admin.getEmail(), subject, text);
        }
    }
}
