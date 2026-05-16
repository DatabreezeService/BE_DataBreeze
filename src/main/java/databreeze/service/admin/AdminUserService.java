package databreeze.service.admin;

import java.util.UUID;

import databreeze.dto.admin.AdminUserCreateRequest;
import databreeze.dto.admin.AdminUserPageResponse;
import databreeze.dto.admin.AdminUserResponse;
import databreeze.dto.admin.AdminUserUpdateRequest;
import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;

public interface AdminUserService {
    AdminUserPageResponse listUsers(String q,
                                    UserStatus status,
                                    SystemRole systemRole,
                                    UserType userType,
                                    AuthProvider authProvider,
                                    Boolean emailVerified,
                                    int page,
                                    int size);

    AdminUserResponse getUser(UUID userId);

    AdminUserResponse createUser(UUID actorUserId, AdminUserCreateRequest request);

    AdminUserResponse updateUser(UUID actorUserId, UUID userId, AdminUserUpdateRequest request);

    AdminUserResponse deactivateUser(UUID actorUserId, UUID userId);

    AdminUserResponse activateUser(UUID actorUserId, UUID userId);

    AdminUserResponse resetPassword(UUID actorUserId, UUID userId, String newPassword);

    String exportUsersCsv(String q,
                          UserStatus status,
                          SystemRole systemRole,
                          UserType userType,
                          AuthProvider authProvider,
                          Boolean emailVerified);

    AdminUserResponse deleteUser(UUID actorUserId, UUID userId);
}
