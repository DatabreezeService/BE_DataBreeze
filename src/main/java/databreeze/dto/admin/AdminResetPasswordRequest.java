package databreeze.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPasswordRequest {
    @NotBlank(message = "Mat khau khong duoc de trong.")
    @Size(min = 6, max = 255, message = "Mat khau toi thieu 6 ky tu.")
    private String newPassword;
}
