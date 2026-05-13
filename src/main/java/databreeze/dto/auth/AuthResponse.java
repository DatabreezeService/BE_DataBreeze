package databreeze.dto.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;

    private UUID userId;
    private String email;
    private String fullName;
    private UserType userType;
    private SystemRole systemRole;
    private UserStatus status;
    private boolean emailVerified;
    private OffsetDateTime lastLoginAt;
}
