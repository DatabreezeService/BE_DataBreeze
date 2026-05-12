package databreeze.service.workspace;

import databreeze.entity.User;
import databreeze.entity.Workspace;

import java.util.UUID;

/**
 * Service tạo workspace mặc định cho user.
 * Khi auth/JWT hoàn thiện, gọi service này sau đăng ký/đăng nhập lần đầu.
 */
public interface WorkspaceBootstrapService {

    /**
     * Tìm hoặc tạo personal workspace mặc định cho user.
     */
    Workspace getOrCreatePersonalWorkspace(User user);

    /**
     * Tìm hoặc tạo organization workspace demo/test cho user.
     */
    Workspace getOrCreateOrganizationWorkspace(User owner, String organizationName);

    /**
     * Đảm bảo user là member active trong workspace với role phù hợp.
     */
    void ensureOwnerMembership(UUID workspaceId, UUID ownerUserId);
}
