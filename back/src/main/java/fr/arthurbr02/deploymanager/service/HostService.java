package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.host.*;
import fr.arthurbr02.deploymanager.entity.*;
import fr.arthurbr02.deploymanager.enums.Role;
import fr.arthurbr02.deploymanager.exception.ForbiddenException;
import fr.arthurbr02.deploymanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HostService {

    private final HostRepository hostRepository;
    private final UserHostPermissionRepository permissionRepository;
    private final DeploymentRepository deploymentRepository;

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
            return HostWithStatusResponse.from(h, lastStatus, lastAt, canDeploy, canEdit);
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

        return HostWithStatusResponse.from(host, lastStatus, lastAt, canDeploy, canEdit);
    }

    @Transactional
    public HostResponse create(HostRequest req) {
        Host host = Host.builder()
                .name(req.name())
                .ip(req.ip())
                .domain(req.domain())
                .deploymentCommand(req.deploymentCommand())
                .generateCommand(req.generateCommand())
                .deliverCommand(req.deliverCommand())
                .defaultTimeout(req.defaultTimeout())
                .build();
        return HostResponse.from(hostRepository.save(host));
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
        host.setName(req.name());
        host.setIp(req.ip());
        host.setDomain(req.domain());
        host.setDeploymentCommand(req.deploymentCommand());
        host.setGenerateCommand(req.generateCommand());
        host.setDeliverCommand(req.deliverCommand());
        host.setDefaultTimeout(req.defaultTimeout());
        return HostResponse.from(hostRepository.save(host));
    }

    @Transactional
    public void delete(UUID id) {
        Host host = hostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Hôte introuvable"));
        host.setDeletedAt(LocalDateTime.now());
        hostRepository.save(host);
    }

    @Transactional
    public void setPermission(UUID userId, PermissionRequest req) {
        UserHostPermission perm = permissionRepository.findByUserIdAndHostId(userId, req.hostId())
                .orElse(UserHostPermission.builder().userId(userId).hostId(req.hostId()).build());
        perm.setCanDeploy(req.canDeploy());
        perm.setCanEdit(req.canEdit());
        if (!req.canDeploy() && !req.canEdit()) {
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
}
