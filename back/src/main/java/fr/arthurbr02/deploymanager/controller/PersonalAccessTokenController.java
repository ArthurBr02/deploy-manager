package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.auth.CreateTokenRequest;
import fr.arthurbr02.deploymanager.dto.auth.TokenResponse;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.service.PersonalAccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/tokens")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Personal Access Tokens")
public class PersonalAccessTokenController {

    private final PersonalAccessTokenService tokenService;

    @GetMapping
    @Operation(summary = "Lister mes tokens")
    public ResponseEntity<List<TokenResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tokenService.getUserTokens(user.getId()).stream()
                .map(TokenResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau token")
    public ResponseEntity<TokenResponse> create(@AuthenticationPrincipal User user, @RequestBody CreateTokenRequest req) {
        var result = tokenService.createToken(user, req.getName(), req.getExpiresAt());
        TokenResponse resp = TokenResponse.from(result.getValue());
        resp.setToken(result.getKey());
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Révoquer un token")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        tokenService.deleteToken(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
