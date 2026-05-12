package databreeze.service.auth;

import java.time.OffsetDateTime;

public interface OtpService {
    String generateOtp();

    String hashOtp(String otp);

    boolean matches(String rawOtp, String hashedOtp);

    OffsetDateTime expiryFromNow();
}
