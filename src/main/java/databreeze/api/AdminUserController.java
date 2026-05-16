package databreeze.api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.admin.AdminResetPasswordRequest;
import databreeze.dto.admin.AdminUserCreateRequest;
import databreeze.dto.admin.AdminUserPageResponse;
import databreeze.dto.admin.AdminUserResponse;
import databreeze.dto.admin.AdminUserUpdateRequest;
import databreeze.dto.common.ApiResponse;
import databreeze.enums.AuditAction;
import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import databreeze.security.AdminAccess;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.admin.AdminUserService;
import databreeze.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "CRUD user cho admin")
@SecurityRequirement(name = "bearer")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Danh sach user co filter va paging")
    public ApiResponse<AdminUserPageResponse> listUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) SystemRole systemRole,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) AuthProvider authProvider,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("OK", adminUserService.listUsers(q, status, systemRole, userType, authProvider, emailVerified, page, size));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Chi tiet user")
    public ApiResponse<AdminUserResponse> getUser(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID userId) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("OK", adminUserService.getUser(userId));
    }

    @PostMapping
    @Operation(summary = "Tao user moi")
    public ApiResponse<AdminUserResponse> createUser(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody AdminUserCreateRequest request) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("Da tao user.", adminUserService.createUser(CurrentUser.requireUserId(principal), request));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Cap nhat user")
    public ApiResponse<AdminUserResponse> updateUser(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID userId,
                                                     @Valid @RequestBody AdminUserUpdateRequest request) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("Da cap nhat user.", adminUserService.updateUser(CurrentUser.requireUserId(principal), userId, request));
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "Khoa user")
    public ApiResponse<AdminUserResponse> deactivateUser(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable UUID userId) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("Da khoa user.", adminUserService.deactivateUser(CurrentUser.requireUserId(principal), userId));
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Mo khoa user")
    public ApiResponse<AdminUserResponse> activateUser(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable UUID userId) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("Da mo khoa user.", adminUserService.activateUser(CurrentUser.requireUserId(principal), userId));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Reset mat khau user")
    public ApiResponse<AdminUserResponse> resetPassword(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable UUID userId,
                                                        @Valid @RequestBody AdminResetPasswordRequest request) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("Da reset mat khau.", adminUserService.resetPassword(CurrentUser.requireUserId(principal), userId, request.getNewPassword()));
    }

    @GetMapping("/export")
    @Operation(summary = "Export danh sach user CSV")
    public ResponseEntity<String> exportUsersCsv(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) SystemRole systemRole,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) AuthProvider authProvider,
            @RequestParam(required = false) Boolean emailVerified
    ) {
        AdminAccess.requireAdmin(principal);
        String csv = adminUserService.exportUsersCsv(q, status, systemRole, userType, authProvider, emailVerified);
        try {
            auditLogService.record(CurrentUser.requireUserId(principal), AuditAction.ADMIN_EXPORT_USERS, "USER", null, null, null, null, null);
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
            .contentType(MediaType.valueOf("text/csv"))
                .body(csv);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Xoa user (soft delete)")
    public ApiResponse<AdminUserResponse> deleteUser(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID userId) {
        AdminAccess.requireAdmin(principal);
        return ApiResponse.ok("Da xoa user.", adminUserService.deleteUser(CurrentUser.requireUserId(principal), userId));
    }
}
