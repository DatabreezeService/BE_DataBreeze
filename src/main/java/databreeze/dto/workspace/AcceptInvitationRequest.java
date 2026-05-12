package databreeze.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcceptInvitationRequest {
    @NotBlank(message = "Token lời mời không được để trống.")
    private String token;
}
