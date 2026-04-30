package fr.arthurbr02.deploymanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.arthurbr02.deploymanager.entity.AuditLog;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import fr.arthurbr02.deploymanager.dto.audit.AuditLogResponse;
import fr.arthurbr02.deploymanager.repository.UserRepository;
import fr.arthurbr02.deploymanager.util.AuditConstants;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String entityName, UUID entityId, String action, String oldValue, String newValue) {
        logAs(resolveUserId(), entityName, entityId, action, null, oldValue, newValue);
    }

    @Transactional
    public void log(String entityName, UUID entityId, String action, Object oldEntity, Object newEntity) {
        logAs(resolveUserId(), entityName, entityId, action, null, serialize(oldEntity), serialize(newEntity));
    }

    @Transactional
    public void logAs(UUID userId, String entityName, UUID entityId, String action, Object oldEntity, Object newEntity) {
        logAs(userId, entityName, entityId, action, null, serialize(oldEntity), serialize(newEntity));
    }

    @Transactional
    public void logAs(UUID userId, String entityName, UUID entityId, String action, UUID contextId, Object oldEntity, Object newEntity) {
        AuditLog entry = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .oldValue(serialize(oldEntity))
                .newValue(serialize(newEntity))
                .userId(userId)
                .contextId(contextId)
                .build();
        auditLogRepository.save(entry);
    }

    public Page<AuditLogResponse> findAll(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).map(this::toResponse);
    }

    public Page<AuditLogResponse> findByUserId(UUID userId, int page, int size) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).map(this::toResponse);
    }

    @Transactional
    public void logPermissionsChange(UUID actorId, UUID targetUserId, Object before, Object after) {
        Instant windowStart = Instant.now().minusSeconds(60);
        auditLogRepository.findTopByEntityNameAndEntityIdAndUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
                AuditConstants.ENTITY_USER_HOST_PERMISSION, targetUserId, actorId, windowStart)
                .ifPresentOrElse(
                        existing -> {
                            existing.setNewValue(serialize(after));
                            auditLogRepository.save(existing);
                        },
                        () -> logAs(actorId, AuditConstants.ENTITY_USER_HOST_PERMISSION,
                                targetUserId, AuditConstants.ACTION_UPDATE, before, after)
                );
    }

    public AuditLogResponse findById(UUID id) {
        return auditLogRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Log introuvable"));
    }

    private UUID resolveUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User user) return user.getId();
        } catch (Exception ignored) {}
        return null;
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Audit serialization failed: {}", e.getMessage());
            return obj.toString();
        }
    }

    private AuditLogResponse toResponse(AuditLog logEntry) {
        AuditLogResponse response = AuditLogResponse.builder()
                .id(logEntry.getId())
                .entityName(logEntry.getEntityName())
                .entityId(logEntry.getEntityId())
                .action(logEntry.getAction())
                .oldValue(logEntry.getOldValue())
                .newValue(logEntry.getNewValue())
                .userId(logEntry.getUserId())
                .contextId(logEntry.getContextId())
                .createdAt(logEntry.getCreatedAt())
                .build();

        if (logEntry.getUserId() != null) {
            userRepository.findById(logEntry.getUserId()).ifPresent(user -> {
                response.setUserFirstName(user.getFirstName());
                response.setUserLastName(user.getLastName());
                response.setUserEmail(user.getEmail());
                response.setUserAvatar(user.getAvatar());
            });
        }

        return response;
    }
}
