package databreeze.service.auth;

import java.time.OffsetDateTime;

public interface EmailService {
    void sendVerificationOtp(String email, String fullName, String otp, OffsetDateTime expiresAt);

    void sendResetPasswordOtp(String email, String fullName, String otp, OffsetDateTime expiresAt);
}
