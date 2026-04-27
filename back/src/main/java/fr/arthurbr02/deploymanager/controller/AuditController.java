package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.entity.AuditLog;
import fr.arthurbr02.deploymanager.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.arthurbr02.deploymanager.dto.audit.AuditLogResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Lister les logs d'audit (admin)")
    public ResponseEntity<Page<AuditLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.findAll(page, size));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lister les logs d'audit d'un utilisateur (admin)")
    public ResponseEntity<Page<AuditLogResponse>> findByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.findByUserId(userId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un log d'audit (admin)")
    public ResponseEntity<AuditLogResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.findById(id));
    }
}
