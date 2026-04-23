package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.config.*;
import fr.arthurbr02.deploymanager.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Configuration")
public class ConfigController {

    private final AppConfigService configService;

    @GetMapping
    @Operation(summary = "Obtenir tous les paramètres (admin)")
    public ResponseEntity<AppConfigResponse> get() {
        return ResponseEntity.ok(new AppConfigResponse(configService.getAll()));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour les paramètres (admin)")
    public ResponseEntity<AppConfigResponse> update(@RequestBody AppConfigRequest req) {
        configService.saveAll(req.settings());
        return ResponseEntity.ok(new AppConfigResponse(configService.getAll()));
    }
}
