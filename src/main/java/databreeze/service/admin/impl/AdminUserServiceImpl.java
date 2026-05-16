package databreeze.service.admin.impl;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import databreeze.dto.admin.AdminUserCreateRequest;
import databreeze.dto.admin.AdminUserPageResponse;
import databreeze.dto.admin.AdminUserResponse;
import databreeze.dto.admin.AdminUserUpdateRequest;
import databreeze.entity.User;
import databreeze.enums.AuditAction;
import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import databreeze.repository.UserRepository;
import databreeze.service.admin.AdminUserService;
import databreeze.service.audit.AuditLogService;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public AdminUserPageResponse listUsers(String q,
                                           UserStatus status,
                                           SystemRole systemRole,
                                           UserType userType,
                                           AuthProvider authProvider,
                                           Boolean emailVerified,
                                           int page,
                                           int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = Specification.where(hasStatus(status))
                .and(hasSystemRole(systemRole))
                .and(hasUserType(userType))
                .and(hasAuthProvider(authProvider))
                .and(hasEmailVerified(emailVerified))
                .and(matchesQuery(q));

        Page<User> result = userRepository.findAll(spec, pageable);

        return AdminUserPageResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponse createUser(UUID actorUserId, AdminUserCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email da ton tai.");
        }

        AuthProvider authProvider = request.getAuthProvider() == null ? AuthProvider.EMAIL_PASSWORD : request.getAuthProvider();
        String passwordHash = null;
        if (authProvider == AuthProvider.EMAIL_PASSWORD) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new IllegalArgumentException("Mat khau bat buoc voi tai khoan email.");
            }
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        UserStatus status = request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus();
        Boolean emailVerified = request.getEmailVerified();
        if (emailVerified == null) {
            emailVerified = authProvider == AuthProvider.GOOGLE || status == UserStatus.ACTIVE;
        }

        User user = User.builder()
                .email(email)
                .fullName(normalizeName(request.getFullName()))
                .avatarUrl(request.getAvatarUrl())
                .passwordHash(passwordHash)
                .authProvider(authProvider)
                .emailVerified(emailVerified)
                .userType(request.getUserType() == null ? UserType.PERSONAL : request.getUserType())
                .systemRole(request.getSystemRole() == null ? SystemRole.USER : request.getSystemRole())
                .status(status)
                .build();

        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(actorUserId, AuditAction.CREATE, "USER", saved.getId(), null, userSnapshot(saved), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(UUID actorUserId, UUID userId, AdminUserUpdateRequest request) {
        User user = requireUser(userId);
        var oldValue = userSnapshot(user);

        if (request.getFullName() != null) {
            user.setFullName(normalizeName(request.getFullName()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        if (request.getSystemRole() != null) {
            user.setSystemRole(request.getSystemRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(actorUserId, AuditAction.UPDATE, "USER", saved.getId(), oldValue, userSnapshot(saved), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse deactivateUser(UUID actorUserId, UUID userId) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new IllegalStateException("Khong the khoa chinh minh.");
        }
        User user = requireUser(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalStateException("Khong the khoa user da bi xoa.");
        }
        var oldValue = userSnapshot(user);
        user.setStatus(UserStatus.SUSPENDED);
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(actorUserId, AuditAction.ADMIN_SUSPEND_USER, "USER", saved.getId(), oldValue, userSnapshot(saved), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse activateUser(UUID actorUserId, UUID userId) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new IllegalStateException("Khong the tu mo khoa chinh minh.");
        }
        User user = requireUser(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalStateException("Khong the mo khoa user da bi xoa.");
        }
        var oldValue = userSnapshot(user);
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(actorUserId, AuditAction.ADMIN_ACTIVATE_USER, "USER", saved.getId(), oldValue, userSnapshot(saved), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse resetPassword(UUID actorUserId, UUID userId, String newPassword) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new IllegalStateException("Khong the tu reset mat khau chinh minh.");
        }
        User user = requireUser(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalStateException("Khong the reset mat khau cho user da bi xoa.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mat khau khong duoc de trong.");
        }
        var oldValue = userSnapshot(user);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        if (user.getAuthProvider() != AuthProvider.EMAIL_PASSWORD) {
            user.setAuthProvider(AuthProvider.EMAIL_PASSWORD);
        }
        user.setEmailVerified(true);
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(actorUserId, AuditAction.ADMIN_RESET_PASSWORD, "USER", saved.getId(), oldValue, userSnapshot(saved), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportUsersCsv(String q,
                                 UserStatus status,
                                 SystemRole systemRole,
                                 UserType userType,
                                 AuthProvider authProvider,
                                 Boolean emailVerified) {
        Specification<User> spec = Specification.where(hasStatus(status))
                .and(hasSystemRole(systemRole))
                .and(hasUserType(userType))
                .and(hasAuthProvider(authProvider))
                .and(hasEmailVerified(emailVerified))
                .and(matchesQuery(q));

        StringBuilder csv = new StringBuilder();
        csv.append("id,email,full_name,auth_provider,email_verified,user_type,system_role,status,last_login_at,created_at,updated_at\n");
        userRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .forEach(user -> csv.append(toCsvRow(user)));
        return csv.toString();
    }

    @Override
    @Transactional
    public AdminUserResponse deleteUser(UUID actorUserId, UUID userId) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new IllegalStateException("Khong the xoa chinh minh.");
        }
        User user = requireUser(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            return toResponse(user);
        }
        var oldValue = userSnapshot(user);
        user.setStatus(UserStatus.DELETED);
        user.setEmailVerified(false);
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(actorUserId, AuditAction.DELETE, "USER", saved.getId(), oldValue, userSnapshot(saved), null, null);
        return toResponse(saved);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay user."));
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .userType(user.getUserType())
                .systemRole(user.getSystemRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private java.util.Map<String, Object> userSnapshot(User user) {
        return java.util.Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "avatarUrl", user.getAvatarUrl(),
                "authProvider", user.getAuthProvider(),
                "emailVerified", user.getEmailVerified(),
                "userType", user.getUserType(),
                "systemRole", user.getSystemRole(),
                "status", user.getStatus()
        );
    }

    private String toCsvRow(User user) {
        return new StringBuilder()
                .append(escapeCsv(user.getId()))
                .append(',').append(escapeCsv(user.getEmail()))
                .append(',').append(escapeCsv(user.getFullName()))
                .append(',').append(escapeCsv(user.getAuthProvider()))
                .append(',').append(escapeCsv(user.getEmailVerified()))
                .append(',').append(escapeCsv(user.getUserType()))
                .append(',').append(escapeCsv(user.getSystemRole()))
                .append(',').append(escapeCsv(user.getStatus()))
                .append(',').append(escapeCsv(user.getLastLoginAt()))
                .append(',').append(escapeCsv(user.getCreatedAt()))
                .append(',').append(escapeCsv(user.getUpdatedAt()))
                .append("\n")
                .toString();
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        String escaped = raw.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private Specification<User> hasSystemRole(SystemRole role) {
        return (root, query, cb) -> role == null ? cb.conjunction() : cb.equal(root.get("systemRole"), role);
    }

    private Specification<User> hasUserType(UserType userType) {
        return (root, query, cb) -> userType == null ? cb.conjunction() : cb.equal(root.get("userType"), userType);
    }

    private Specification<User> hasAuthProvider(AuthProvider provider) {
        return (root, query, cb) -> provider == null ? cb.conjunction() : cb.equal(root.get("authProvider"), provider);
    }

    private Specification<User> hasEmailVerified(Boolean emailVerified) {
        return (root, query, cb) -> emailVerified == null ? cb.conjunction() : cb.equal(root.get("emailVerified"), emailVerified);
    }

    private Specification<User> matchesQuery(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("fullName")), like)
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
