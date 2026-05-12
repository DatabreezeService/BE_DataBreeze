package databreeze.dto.workspace;

import databreeze.enums.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptInvitationResponse {
    private UUID workspaceId;
    private UUID userId;
    private WorkspaceRole role;
    private String message;
}
