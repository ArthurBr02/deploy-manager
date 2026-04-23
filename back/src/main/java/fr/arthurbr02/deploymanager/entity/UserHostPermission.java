package fr.arthurbr02.deploymanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_host_permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(UserHostPermissionId.class)
public class UserHostPermission {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "host_id")
    private UUID hostId;

    @Column(name = "can_deploy", nullable = false)
    private boolean canDeploy;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private Host host;
}
