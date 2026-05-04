package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.auth.*;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.service.AuthService;
import fr.arthurbr02.deploymanager.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;

    @PostMapping("/login")
    @Operation(summary = "Connexion")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(req, request, response));
    }

    @PostMapping("/verify-mfa")
    @Operation(summary = "Vérifier le code 2FA")
    public ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody VerifyMfaRequest req,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        return ResponseEntity.ok(authService.verifyMfaAndLogin(req, request, response));
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

    @GetMapping("/trusted-devices")
    @Operation(summary = "Lister mes appareils de confiance")
    public ResponseEntity<List<TrustedDeviceDto>> getTrustedDevices(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mfaService.getUserTrustedDevices(user.getId()));
    }

    @DeleteMapping("/trusted-devices/{id}")
    @Operation(summary = "Révoquer un appareil de confiance")
    public ResponseEntity<Void> revokeTrustedDevice(@AuthenticationPrincipal User user,
                                                    @PathVariable UUID id) {
        mfaService.revokeTrustedDevice(id, user.getId(), false);
        return ResponseEntity.noContent().build();
    }
}
