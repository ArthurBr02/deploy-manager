package fr.arthurbr02.deploymanager.dto.auth;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateTokenRequest {
    private String name;
    private LocalDateTime expiresAt;
}
