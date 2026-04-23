package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.auth.*;
import fr.arthurbr02.deploymanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Connexion")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(req, response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token")
    public ResponseEntity<RefreshResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demande réinitialisation mot de passe")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialisation mot de passe")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}
