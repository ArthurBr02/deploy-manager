package fr.arthurbr02.deploymanager.config;

import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.enums.Role;
import fr.arthurbr02.deploymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.countByRoleAndDeletedAtIsNull(Role.ADMIN) == 0) {
            User admin = User.builder()
                    .email(adminEmail)
                    .firstName("Admin")
                    .lastName("Deploy")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Admin initial créé avec l'email: {}", adminEmail);
        }
    }
}
