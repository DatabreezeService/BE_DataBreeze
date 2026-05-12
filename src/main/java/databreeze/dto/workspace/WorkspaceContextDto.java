package databreeze.dto.workspace;

import databreeze.enums.WorkspacePermission;
import databreeze.enums.WorkspaceRole;
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
public class WorkspaceContextDto {
    private UUID workspaceId;
    private String workspaceName;
    private WorkspaceType workspaceType;
    private UUID actorUserId;
    private WorkspaceRole role;
    private Set<WorkspacePermission> permissions;
    private Boolean canRead;
    private Boolean canImport;
    private Boolean canManageMembers;
    private String message;
}
