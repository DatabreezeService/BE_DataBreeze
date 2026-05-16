package databreeze.api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.audit.AuditLogPageResponse;
import databreeze.dto.common.ApiResponse;
import databreeze.enums.AuditAction;
import databreeze.security.AdminAccess;
import databreeze.security.UserPrincipal;
import databreeze.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Admin Audit Logs", description = "Xem audit log cho admin")
@SecurityRequirement(name = "bearer")
public class AdminAuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Danh sach audit log")
    public ApiResponse<AuditLogPageResponse> listLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("OK", auditLogService.listLogs(action, entityType, actorUserId, entityId, page, size));
    }
}
