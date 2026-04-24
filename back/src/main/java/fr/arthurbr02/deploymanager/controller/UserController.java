package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.user.*;
import fr.arthurbr02.deploymanager.entity.User;
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
}
