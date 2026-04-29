package fr.arthurbr02.deploymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_host_permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(UserHostPermissionId.class)
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "can_execute", nullable = false)
    private boolean canExecute;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private Host host;
}
