package databreeze.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc.")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự.")
    private String password;

    private String fullName;

}
