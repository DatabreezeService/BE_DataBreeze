package databreeze.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email khong duoc de trong.")
    @Email(message = "Email khong hop le.")
    @Size(max = 255, message = "Email toi da 255 ky tu.")
    private String email;

    @NotBlank(message = "OTP khong duoc de trong.")
    @Size(min = 4, max = 10, message = "OTP khong hop le.")
    private String otp;

    @NotBlank(message = "Mat khau khong duoc de trong.")
    @Size(min = 6, max = 255, message = "Mat khau toi thieu 6 ky tu.")
    private String newPassword;
}
