package databreeze.dto.workspace;

import databreeze.enums.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank(message = "Email người được mời không được để trống.")
    @Email(message = "Email người được mời không hợp lệ.")
    private String email;

    @NotNull(message = "Role của người được mời là bắt buộc.")
    private WorkspaceRole role = WorkspaceRole.MEMBER;
}
