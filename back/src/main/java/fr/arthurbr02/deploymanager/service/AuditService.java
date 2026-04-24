package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.AuditLog;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String entityName, UUID entityId, String action, String oldValue, String newValue) {
        UUID userId = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            userId = user.getId();
        }

        AuditLog log = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .userId(userId)
                .build();
        auditLogRepository.save(log);
    }

    public Page<AuditLog> findAll(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }
}
