package fr.arthurbr02.deploymanager.dto.user;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min=8) String newPassword) {}
