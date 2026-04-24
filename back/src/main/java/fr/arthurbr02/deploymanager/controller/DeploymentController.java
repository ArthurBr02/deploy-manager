package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.deployment.DeploymentRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentResponse;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentStatsResponse;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.service.DeploymentService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
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
    private final fr.arthurbr02.deploymanager.security.JwtUtil jwtUtil;
    private final fr.arthurbr02.deploymanager.repository.UserRepository userRepository;
    private final fr.arthurbr02.deploymanager.service.PersonalAccessTokenService patService;

    @PostMapping("/sse-token")
    @Operation(summary = "Générer un token à usage unique pour SSE")
    public ResponseEntity<java.util.Map<String, String>> generateSseToken(@AuthenticationPrincipal User user) {
        String token = jwtUtil.generateSseToken(user.getId());
        return ResponseEntity.ok(java.util.Map.of("token", token));
    }

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
    @Operation(summary = "Stream SSE des logs d'un déploiement (nécessite un token SSE)")
    public SseEmitter streamLogs(@PathVariable UUID id, @RequestParam String token) {
        User user = validateSseToken(token);
        return deploymentService.streamLogs(id, user);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE des changements de statut de déploiement (nécessite un token SSE)")
    public SseEmitter subscribeEvents(@RequestParam String token) {
        User user = validateSseToken(token);
        return deploymentService.subscribeEvents(user);
    }

    private User validateSseToken(String token) {
        // 1. Try as JWT SSE token or Access Token
        try {
            Claims claims = jwtUtil.validateAccessToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            return userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        } catch (Exception ignored) {}

        // 2. Try as Personal Access Token (PAT)
        return patService.validateToken(token)
                .orElseThrow(() -> new fr.arthurbr02.deploymanager.exception.ForbiddenException("Token SSE, Access ou PAT invalide ou expiré"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des déploiements sur une période")
    public ResponseEntity<DeploymentStatsResponse> stats(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(deploymentService.getStats(period, hostId, type));
    }

    @GetMapping("/export")
    @Operation(summary = "Exporter les déploiements en CSV")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String period) {
        byte[] csv = deploymentService.exportCsv(user, hostId, search, status, type, period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"deployments.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping
    @Operation(summary = "Historique des déploiements avec filtres")
    public ResponseEntity<Page<DeploymentResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deploymentService.findAll(user, hostId, search, status, type, period, page, size));
    }
}
