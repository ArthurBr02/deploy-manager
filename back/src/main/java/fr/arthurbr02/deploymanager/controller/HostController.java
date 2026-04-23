package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.host.*;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.service.HostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hôtes")
public class HostController {

    private final HostService hostService;

    @GetMapping("/hosts")
    @Operation(summary = "Lister les hôtes accessibles")
    public ResponseEntity<List<HostWithStatusResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(hostService.findAll(user));
    }

    @GetMapping("/hosts/{id}")
    @Operation(summary = "Détail d'un hôte")
    public ResponseEntity<HostWithStatusResponse> get(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(hostService.findById(id, user));
    }

    @PostMapping("/admin/hosts")
    @Operation(summary = "Créer un hôte (admin)")
    public ResponseEntity<HostResponse> create(@Valid @RequestBody HostRequest req) {
        return ResponseEntity.ok(hostService.create(req));
    }

    @PutMapping("/hosts/{id}")
    @Operation(summary = "Modifier un hôte")
    public ResponseEntity<HostResponse> update(@PathVariable UUID id, @Valid @RequestBody HostRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(hostService.update(id, req, user));
    }

    @DeleteMapping("/admin/hosts/{id}")
    @Operation(summary = "Supprimer un hôte (admin, soft delete)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        hostService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/users/{userId}/permissions")
    @Operation(summary = "Modifier les permissions d'un utilisateur sur un hôte (admin)")
    public ResponseEntity<Void> setPermission(@PathVariable UUID userId, @Valid @RequestBody PermissionRequest req) {
        hostService.setPermission(userId, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/users/{userId}/permissions")
    @Operation(summary = "Obtenir les permissions d'un utilisateur (admin)")
    public ResponseEntity<List<Map<String, Object>>> getPermissions(@PathVariable UUID userId) {
        return ResponseEntity.ok(hostService.getPermissionsForUser(userId));
    }

    @PostMapping(value = "/admin/hosts/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer un fichier Ansible hosts-all (admin)")
    public ResponseEntity<Map<String, Object>> importAnsible(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(hostService.importAnsibleFile(file));
    }
}
