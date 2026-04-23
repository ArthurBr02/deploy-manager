package fr.arthurbr02.deploymanager.dto.auth;
import java.util.UUID;
public record UserInfo(UUID id, String email, String firstName, String lastName, String role, String avatar) {}
