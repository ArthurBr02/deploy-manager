package fr.arthurbr02.deploymanager.dto.user;
import java.util.UUID;
public record CreateUserResponse(UUID id, String email, String firstName, String lastName, String role, String temporaryPassword) {}
