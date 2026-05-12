package databreeze.service.auth;

import databreeze.dto.auth.AuthResponse;
import databreeze.dto.auth.EmailOtpResponse;
import databreeze.dto.auth.GoogleLoginRequest;
import databreeze.dto.auth.LoginRequest;
import databreeze.dto.auth.RegisterRequest;
import databreeze.dto.auth.ResendOtpRequest;
import databreeze.dto.auth.VerifyOtpRequest;

public interface AuthService {
    EmailOtpResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse verifyEmailOtp(VerifyOtpRequest request);

    EmailOtpResponse resendEmailOtp(ResendOtpRequest request);
}
