package databreeze.dto.workspace;

import databreeze.enums.WorkspaceRole;
import databreeze.enums.WorkspaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrganizationWorkspaceResponse {
    private UUID workspaceId;
    private String workspaceName;
    private WorkspaceType workspaceType;
    private UUID ownerUserId;
    private WorkspaceRole ownerRole;
    private String message;
    private String nextStep;
}
