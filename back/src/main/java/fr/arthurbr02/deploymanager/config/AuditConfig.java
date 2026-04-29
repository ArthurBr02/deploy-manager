package fr.arthurbr02.deploymanager.config;

import fr.arthurbr02.deploymanager.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
                return Optional.empty();
            }
            return Optional.ofNullable(user.getId());
        };
    }
}
