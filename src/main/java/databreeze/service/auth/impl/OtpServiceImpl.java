package databreeze.service.auth.impl;

import databreeze.service.auth.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
public class OtpServiceImpl implements OtpService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final int length;
    private final int ttlMinutes;

    public OtpServiceImpl(
            @Value("${app.otp.length}") int length,
            @Value("${app.otp.ttl-minutes}") int ttlMinutes
    ) {
        this.length = Math.max(4, length);
        this.ttlMinutes = Math.max(5, ttlMinutes);
    }

    @Override
    public String generateOtp() {
        int max = (int) Math.pow(10, length);
        int otp = secureRandom.nextInt(max);
        return String.format("%0" + length + "d", otp);
    }

    @Override
    public String hashOtp(String otp) {
        if (otp == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể hash OTP.", ex);
        }
    }

    @Override
    public boolean matches(String rawOtp, String hashedOtp) {
        if (rawOtp == null || hashedOtp == null) {
            return false;
        }
        return hashOtp(rawOtp).equalsIgnoreCase(hashedOtp);
    }

    @Override
    public OffsetDateTime expiryFromNow() {
        return OffsetDateTime.now().plusMinutes(ttlMinutes);
    }
}
