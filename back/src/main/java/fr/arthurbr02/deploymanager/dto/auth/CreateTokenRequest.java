package fr.arthurbr02.deploymanager.dto.auth;

import lombok.Data;
import java.time.Instant;

@Data
public class CreateTokenRequest {
    private String name;
    private Instant expiresAt;
}
