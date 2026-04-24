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

import fr.arthurbr02.deploymanager.dto.audit.AuditLogResponse;
import fr.arthurbr02.deploymanager.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

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

    public Page<AuditLogResponse> findAll(int page, int size) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return logs.map(this::toResponse);
    }

    public Page<AuditLogResponse> findByUserId(UUID userId, int page, int size) {
        Page<AuditLog> logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return logs.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = AuditLogResponse.builder()
                .id(log.getId())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .userId(log.getUserId())
                .createdAt(log.getCreatedAt())
                .build();

        if (log.getUserId() != null) {
            userRepository.findById(log.getUserId()).ifPresent(user -> {
                response.setUserFirstName(user.getFirstName());
                response.setUserLastName(user.getLastName());
                response.setUserEmail(user.getEmail());
                response.setUserAvatar(user.getAvatar());
            });
        }

        return response;
    }
}
