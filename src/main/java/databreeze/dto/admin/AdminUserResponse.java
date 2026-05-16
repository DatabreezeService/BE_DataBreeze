package databreeze.dto.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private AuthProvider authProvider;
    private Boolean emailVerified;
    private UserType userType;
    private SystemRole systemRole;
    private UserStatus status;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
