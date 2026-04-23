package fr.arthurbr02.deploymanager.entity;

import fr.arthurbr02.deploymanager.enums.DeploymentStatus;
import fr.arthurbr02.deploymanager.enums.DeploymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Deployment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;

    @Column(columnDefinition = "TEXT")
    private String logs;

    @Column(name = "log_file_path")
    private String logFilePath;

    @Column(nullable = false)
    private Integer timeout;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private Host host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
