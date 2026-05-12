package databreeze.service.workspace;

import databreeze.dto.workspace.WorkspaceSwitcherResponse;

import java.util.UUID;

public interface WorkspaceQueryService {
    /**
     * Trả danh sách workspace mà user có thể chọn sau khi đăng nhập.
     * MVP chưa setup JWT nên actorUserId được truyền từ Swagger/FE.
     */
    WorkspaceSwitcherResponse listMyWorkspaces(UUID actorUserId);
}
