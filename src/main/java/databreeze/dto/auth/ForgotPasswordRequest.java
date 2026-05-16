package databreeze.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Email khong duoc de trong.")
    @Email(message = "Email khong hop le.")
    @Size(max = 255, message = "Email toi da 255 ky tu.")
    private String email;
}
