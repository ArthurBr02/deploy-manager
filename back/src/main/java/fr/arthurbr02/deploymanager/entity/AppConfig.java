package fr.arthurbr02.deploymanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppConfig {
    @Id
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;
}
