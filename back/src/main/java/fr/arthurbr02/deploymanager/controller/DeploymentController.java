package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.deployment.DeploymentRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentResponse;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentStatsResponse;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Déploiements")
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping("/hosts/{hostId}/deploy")
    @Operation(summary = "Lancer un déploiement")
    public ResponseEntity<DeploymentResponse> launch(@PathVariable UUID hostId,
                                                      @Valid @RequestBody DeploymentRequest req,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deploymentService.launch(hostId, req, user));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler un déploiement")
    public ResponseEntity<DeploymentResponse> cancel(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deploymentService.cancel(id, user));
    }

    @GetMapping(value = "/{id}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE des logs d'un déploiement")
    public SseEmitter streamLogs(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return deploymentService.streamLogs(id, user);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE des changements de statut de déploiement")
    public SseEmitter subscribeEvents(@AuthenticationPrincipal User user) {
        return deploymentService.subscribeEvents(user);
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des déploiements sur une période")
    public ResponseEntity<DeploymentStatsResponse> stats(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(deploymentService.getStats(period));
    }

    @GetMapping
    @Operation(summary = "Historique des déploiements avec filtres")
    public ResponseEntity<Page<DeploymentResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deploymentService.findAll(user, hostId, status, type, page, size));
    }
}
