package fr.arthurbr02.deploymanager.dto.user;
import fr.arthurbr02.deploymanager.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, String role, String avatar, LocalDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole().name(), u.getAvatar(), u.getCreatedAt());
    }
}
