package databreeze.api;

import databreeze.dto.auth.*;
import databreeze.dto.common.ApiResponse;
import databreeze.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Dang ky, dang nhap, JWT, OTP email va Google login")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Dang ky tai khoan (email + mat khau)")
    public ApiResponse<EmailOtpResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Da tao tai khoan. Vui long xac thuc OTP.", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Dang nhap bang email va mat khau")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Dang nhap thanh cong.", authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Dang nhap bang Google")
    public ApiResponse<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.ok("Dang nhap Google thanh cong.", authService.loginWithGoogle(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xac thuc email bang OTP")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok("Xac thuc email thanh cong.", authService.verifyEmailOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Gui lai OTP xac thuc email")
    public ApiResponse<EmailOtpResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.ok("Da gui lai OTP.", authService.resendEmailOtp(request));
    }
}
