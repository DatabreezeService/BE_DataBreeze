package databreeze.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Thiếu idToken từ Google.")
    private String idToken;
}
