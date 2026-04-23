package fr.arthurbr02.deploymanager.entity;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserHostPermissionId implements Serializable {
    private UUID userId;
    private UUID hostId;
}
