package databreeze.service.workspace;

import databreeze.dto.workspace.CreateOrganizationWorkspaceRequest;
import databreeze.dto.workspace.CreateOrganizationWorkspaceResponse;
import databreeze.dto.workspace.InitPersonalWorkspaceResponse;

import java.util.UUID;

public interface WorkspaceCommandService {
    /**
     * Tạo hoặc lấy personal workspace mặc định cho user.
     * Sau này auth service gọi sau khi user đăng ký/login lần đầu.
     */
    InitPersonalWorkspaceResponse initPersonalWorkspace(UUID actorUserId);

    /**
     * Tạo organization workspace mới. Owner mặc định là actorUserId.
     */
    CreateOrganizationWorkspaceResponse createOrganizationWorkspace(UUID actorUserId, CreateOrganizationWorkspaceRequest request);
}
