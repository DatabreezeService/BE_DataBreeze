package databreeze.service.workspace;

import databreeze.dto.workspace.WorkspaceContextDto;
import databreeze.entity.Workspace;
import databreeze.enums.WorkspacePermission;

import java.util.UUID;

/**
 * Service kiểm tra quyền truy cập workspace theo permission/action.
 * Controller/service nghiệp vụ không cần biết role cụ thể; chỉ cần yêu cầu permission.
 */
public interface WorkspaceAccessService {

    Workspace requirePermission(UUID workspaceId, UUID actorUserId, WorkspacePermission permission);

    Workspace requireReadAccess(UUID workspaceId, UUID actorUserId);

    Workspace requireImportAccess(UUID workspaceId, UUID actorUserId);

    Workspace requireManageMembersAccess(UUID workspaceId, UUID actorUserId);

    void requireStoreBelongsToWorkspace(UUID workspaceId, UUID storeId);

    WorkspaceContextDto describeContext(UUID workspaceId, UUID actorUserId);
}
