package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.auth.TrustedDeviceDto;
import fr.arthurbr02.deploymanager.dto.user.*;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.service.MfaService;
import fr.arthurbr02.deploymanager.service.UserService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Utilisateurs")
public class UserController {

    private final UserService userService;
    private final MfaService mfaService;

    @GetMapping("/admin/users")
    @Operation(summary = "Lister tous les utilisateurs (admin)")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/admin/users/{id}")
    @Operation(summary = "Détail d'un utilisateur (admin)")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping("/admin/users")
    @Operation(summary = "Créer un utilisateur (admin)")
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.create(req));
    }

    @PutMapping("/admin/users/{id}")
    @Operation(summary = "Modifier un utilisateur (admin)")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @DeleteMapping("/admin/users/{id}")
    @Operation(summary = "Supprimer un utilisateur (admin, soft delete)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        userService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Détail d'un utilisateur (public)")
    public ResponseEntity<UserResponse> getPublic(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/profile")
    @Operation(summary = "Mon profil")
    public ResponseEntity<UserResponse> profile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Modifier mon profil")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal User user, @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), req));
    }

    @PostMapping("/profile/change-password")
    @Operation(summary = "Changer mon mot de passe")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader mon avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@AuthenticationPrincipal User user,
                                                      @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(user.getId(), file));
    }

    @DeleteMapping("/profile/avatar")
    @Operation(summary = "Supprimer mon avatar")
    public ResponseEntity<UserResponse> deleteAvatar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.deleteAvatar(user.getId()));
    }

    @GetMapping("/admin/users/{userId}/trusted-devices")
    @Operation(summary = "Lister les appareils de confiance d'un utilisateur (admin)")
    public ResponseEntity<List<TrustedDeviceDto>> adminGetTrustedDevices(@PathVariable UUID userId) {
        return ResponseEntity.ok(mfaService.getUserTrustedDevices(userId));
    }

    @DeleteMapping("/admin/users/{userId}/trusted-devices/{deviceId}")
    @Operation(summary = "Révoquer un appareil de confiance (admin)")
    public ResponseEntity<Void> adminRevokeTrustedDevice(@PathVariable UUID userId,
                                                         @PathVariable UUID deviceId) {
        mfaService.revokeTrustedDevice(deviceId, userId, true);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/users/{userId}/trusted-devices")
    @Operation(summary = "Révoquer tous les appareils de confiance d'un utilisateur (admin)")
    public ResponseEntity<Void> adminRevokeAllTrustedDevices(@PathVariable UUID userId) {
        mfaService.revokeAllTrustedDevices(userId);
        return ResponseEntity.noContent().build();
    }
}
