package databreeze.dto.audit;

import databreeze.enums.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID actorUserId;
    private AuditAction action;
    private String entityType;
    private UUID entityId;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime createdAt;
}
