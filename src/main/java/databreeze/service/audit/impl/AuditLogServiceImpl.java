package databreeze.service.audit.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import databreeze.dto.audit.AuditLogPageResponse;
import databreeze.dto.audit.AuditLogResponse;
import databreeze.entity.AuditLog;
import databreeze.enums.AuditAction;
import databreeze.repository.AuditLogRepository;
import databreeze.service.audit.AuditLogService;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void record(UUID actorUserId,
                       AuditAction action,
                       String entityType,
                       UUID entityId,
                       Map<String, Object> oldValue,
                       Map<String, Object> newValue,
                       String ipAddress,
                       String userAgent) {
        AuditLog log = AuditLog.builder()
                .actorUserId(actorUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPageResponse listLogs(AuditAction action,
                                         String entityType,
                                         UUID actorUserId,
                                         UUID entityId,
                                         int page,
                                         int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> spec = Specification.where(hasAction(action))
                .and(hasEntityType(entityType))
                .and(hasActorUserId(actorUserId))
                .and(hasEntityId(entityId));

        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        return AuditLogPageResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .workspaceId(log.getWorkspaceId())
                .actorUserId(log.getActorUserId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private Specification<AuditLog> hasAction(AuditAction action) {
        return (root, query, cb) -> action == null ? cb.conjunction() : cb.equal(root.get("action"), action);
    }

    private Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, cb) -> entityType == null || entityType.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("entityType"), entityType);
    }

    private Specification<AuditLog> hasActorUserId(UUID actorUserId) {
        return (root, query, cb) -> actorUserId == null ? cb.conjunction() : cb.equal(root.get("actorUserId"), actorUserId);
    }

    private Specification<AuditLog> hasEntityId(UUID entityId) {
        return (root, query, cb) -> entityId == null ? cb.conjunction() : cb.equal(root.get("entityId"), entityId);
    }
}
