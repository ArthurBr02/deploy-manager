package fr.arthurbr02.deploymanager.dto.user;
import jakarta.validation.constraints.NotBlank;
public record UpdateProfileRequest(@NotBlank String firstName, @NotBlank String lastName, String avatar) {}
