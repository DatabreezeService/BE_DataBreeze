package databreeze.service.auth.impl;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import databreeze.dto.auth.AuthResponse;
import databreeze.dto.auth.EmailOtpResponse;
import databreeze.dto.auth.GoogleLoginRequest;
import databreeze.dto.auth.LoginRequest;
import databreeze.dto.auth.RegisterRequest;
import databreeze.dto.auth.ResendOtpRequest;
import databreeze.dto.auth.VerifyOtpRequest;
import databreeze.entity.ExternalIdentity;
import databreeze.entity.User;
import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import databreeze.repository.ExternalIdentityRepository;
import databreeze.repository.UserRepository;
import databreeze.service.auth.AuthService;
import databreeze.service.auth.EmailService;
import databreeze.service.auth.GoogleTokenVerifier;
import databreeze.service.auth.JwtService;
import databreeze.service.auth.OtpService;
import databreeze.service.auth.model.GoogleTokenInfo;
import databreeze.service.workspace.WorkspaceBootstrapService;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalIdentityRepository externalIdentityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private WorkspaceBootstrapService workspaceBootstrapService;

    @Value("${app.auth.require-email-verification:true}")
    private boolean requireEmailVerification;

    @Override
    @Transactional
    public EmailOtpResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email da duoc dang ky. Vui long dang nhap.");
        }

        User user = User.builder()
                .email(email)
                .fullName(normalizeName(request.getFullName()))
                .authProvider(AuthProvider.EMAIL_PASSWORD)
                .emailVerified(false)
                .userType(UserType.PERSONAL)
                .systemRole(SystemRole.USER)
                .status(UserStatus.PENDING_ONBOARDING)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user = userRepository.save(user);
        workspaceBootstrapService.getOrCreatePersonalWorkspace(user);

        if (requireEmailVerification) {
            sendOtpEmail(user);
        } else {
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerificationOtp(null);
            user.setEmailVerificationOtpExpiresAt(null);
            user = userRepository.save(user);
        }

        return EmailOtpResponse.builder()
                .email(user.getEmail())
                .expiresAt(user.getEmailVerificationOtpExpiresAt())
                .message(requireEmailVerification
                        ? "Da gui ma OTP xac thuc email. Vui long kiem tra hop thu."
                        : "Da tao tai khoan. Xac thuc email dang tat tren moi truong hien tai.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay tai khoan."));

        if (user.getAuthProvider() != AuthProvider.EMAIL_PASSWORD) {
            throw new IllegalStateException("Email nay da dang ky bang Google. Vui long dang nhap Google.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalStateException("Mat khau khong dung.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.DELETED) {
            throw new IllegalStateException("Tai khoan dang bi khoa hoac da bi xoa.");
        }

        if (requireEmailVerification && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Email chua duoc xac thuc. Vui long nhap OTP.");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        workspaceBootstrapService.getOrCreatePersonalWorkspace(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenInfo tokenInfo = googleTokenVerifier.verify(request.getIdToken());
        String email = normalizeEmail(tokenInfo.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && user.getAuthProvider() == AuthProvider.EMAIL_PASSWORD) {
            throw new IllegalStateException("Email nay da dang ky bang mat khau. Vui long dang nhap bang mat khau.");
        }

        if (user == null) {
            user = User.builder()
                    .email(email)
                    .fullName(normalizeName(tokenInfo.getName()))
                    .avatarUrl(tokenInfo.getPicture())
                    .authProvider(AuthProvider.GOOGLE)
                    .emailVerified(true)
                    .userType(UserType.PERSONAL)
                    .systemRole(SystemRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            user = userRepository.save(user);
        }

        ensureExternalIdentity(user, tokenInfo);

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        workspaceBootstrapService.getOrCreatePersonalWorkspace(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmailOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay tai khoan."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Tai khoan da duoc xac thuc.");
        }

        if (!requireEmailVerification) {
            throw new IllegalStateException("Xac thuc email dang tat tren moi truong hien tai.");
        }

        if (user.getEmailVerificationOtpExpiresAt() == null || OffsetDateTime.now().isAfter(user.getEmailVerificationOtpExpiresAt())) {
            throw new IllegalStateException("OTP da het han. Vui long gui lai.");
        }

        if (!otpService.matches(request.getOtp(), user.getEmailVerificationOtp())) {
            throw new IllegalStateException("OTP khong dung.");
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerificationOtp(null);
        user.setEmailVerificationOtpExpiresAt(null);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        workspaceBootstrapService.getOrCreatePersonalWorkspace(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public EmailOtpResponse resendEmailOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay tai khoan."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Tai khoan da duoc xac thuc.");
        }

        sendOtpEmail(user);

        return EmailOtpResponse.builder()
                .email(user.getEmail())
                .expiresAt(user.getEmailVerificationOtpExpiresAt())
                .message("Da gui lai ma OTP. Vui long kiem tra hop thu.")
                .build();
    }

    private void sendOtpEmail(User user) {
        String otp = otpService.generateOtp();
        user.setEmailVerificationOtp(otpService.hashOtp(otp));
        user.setEmailVerificationOtpExpiresAt(otpService.expiryFromNow());
        user.setEmailVerified(false);
        user.setStatus(UserStatus.PENDING_ONBOARDING);
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        emailService.sendVerificationOtp(user.getEmail(), user.getFullName(), otp, user.getEmailVerificationOtpExpiresAt());
    }

    private void ensureExternalIdentity(User user, GoogleTokenInfo tokenInfo) {
        ExternalIdentity existing = externalIdentityRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, tokenInfo.getSubject())
                .orElse(null);

        if (existing != null && !existing.getUserId().equals(user.getId())) {
            throw new IllegalStateException("Tai khoan Google nay da lien ket voi user khac.");
        }

        if (existing == null) {
            externalIdentityRepository.save(ExternalIdentity.builder()
                    .userId(user.getId())
                    .provider(AuthProvider.GOOGLE)
                    .providerUserId(tokenInfo.getSubject())
                    .providerEmail(user.getEmail())
                    .build());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateAccessToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .systemRole(user.getSystemRole())
                .status(user.getStatus())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
