package fr.arthurbr02.deploymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hosts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Host {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ip;

    private String domain;

    @Column(name = "deployment_command", columnDefinition = "TEXT")
    private String deploymentCommand;

    @Column(name = "generate_command", columnDefinition = "TEXT")
    private String generateCommand;

    @Column(name = "deliver_command", columnDefinition = "TEXT")
    private String deliverCommand;

    @Column(name = "default_timeout")
    private Integer defaultTimeout;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
