package databreeze.service.audit;

import java.util.Map;
import java.util.UUID;

import databreeze.dto.audit.AuditLogPageResponse;
import databreeze.enums.AuditAction;

public interface AuditLogService {
    void record(UUID actorUserId,
                AuditAction action,
                String entityType,
                UUID entityId,
                Map<String, Object> oldValue,
                Map<String, Object> newValue,
                String ipAddress,
                String userAgent);

    AuditLogPageResponse listLogs(AuditAction action,
                                  String entityType,
                                  UUID actorUserId,
                                  UUID entityId,
                                  int page,
                                  int size);
}
