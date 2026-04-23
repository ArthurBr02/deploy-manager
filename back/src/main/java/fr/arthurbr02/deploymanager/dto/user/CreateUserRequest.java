package fr.arthurbr02.deploymanager.dto.user;
import fr.arthurbr02.deploymanager.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CreateUserRequest(@Email @NotBlank String email, @NotBlank String firstName, @NotBlank String lastName, @NotNull Role role) {}
