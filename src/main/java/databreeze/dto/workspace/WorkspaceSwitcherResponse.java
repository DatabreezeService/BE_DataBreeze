package databreeze.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceSwitcherResponse {
    private UUID actorUserId;
    private UUID defaultWorkspaceId;
    private List<MyWorkspaceResponse> workspaces;
    private String message;
}
