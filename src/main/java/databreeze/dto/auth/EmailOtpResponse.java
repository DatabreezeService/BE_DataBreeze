package databreeze.dto.auth;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailOtpResponse {
    private String email;
    private OffsetDateTime expiresAt;
    private String message;
}
