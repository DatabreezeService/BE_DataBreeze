package databreeze.dto.workspace;

import databreeze.enums.WorkspacePermission;
import databreeze.enums.WorkspaceRole;
import databreeze.enums.WorkspaceStatus;
import databreeze.enums.WorkspaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyWorkspaceResponse {
    private UUID workspaceId;
    private String workspaceName;
    private WorkspaceType workspaceType;
    private WorkspaceRole role;
    private WorkspaceStatus status;
    private Boolean defaultWorkspace;
    private Boolean personalWorkspace;
    private Boolean organizationWorkspace;
    private String countryCode;
    private String currencyCode;
    private String timezone;
    private Set<WorkspacePermission> permissions;
    private String message;
}
