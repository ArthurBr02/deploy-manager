package fr.arthurbr02.deploymanager.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private String entityName;
    private UUID entityId;
    private String action;
    private String oldValue;
    private String newValue;
    private UUID userId;
    private UUID contextId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String userAvatar;
    private Instant createdAt;
}
