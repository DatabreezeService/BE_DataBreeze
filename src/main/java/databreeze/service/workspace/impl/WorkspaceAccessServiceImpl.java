package databreeze.service.workspace.impl;

import databreeze.dto.workspace.WorkspaceContextDto;
import databreeze.entity.Workspace;
import databreeze.entity.WorkspaceMember;
import databreeze.enums.*;
import databreeze.repository.StoreRepository;
import databreeze.repository.UserRepository;
import databreeze.repository.WorkspaceMemberRepository;
import databreeze.repository.WorkspaceRepository;
import databreeze.service.workspace.WorkspaceAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceAccessServiceImpl implements WorkspaceAccessService {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public Workspace requirePermission(UUID workspaceId, UUID actorUserId, WorkspacePermission permission) {
        Workspace workspace = requireActiveWorkspace(workspaceId);
        requireExistingUser(actorUserId);

        WorkspaceRole role = resolveRole(workspace, actorUserId);
        Set<WorkspacePermission> permissions = permissionsOf(role, workspace.getWorkspaceType());
        if (!permissions.contains(permission)) {
            throw new SecurityException("Bạn không có quyền " + permission.name() + " trong workspace này.");
        }
        return workspace;
    }

    @Override
    @Transactional(readOnly = true)
    public Workspace requireReadAccess(UUID workspaceId, UUID actorUserId) {
        return requirePermission(workspaceId, actorUserId, WorkspacePermission.READ_WORKSPACE);
    }

    @Override
    @Transactional(readOnly = true)
    public Workspace requireImportAccess(UUID workspaceId, UUID actorUserId) {
        return requirePermission(workspaceId, actorUserId, WorkspacePermission.IMPORT_DATA);
    }

    @Override
    @Transactional(readOnly = true)
    public Workspace requireManageMembersAccess(UUID workspaceId, UUID actorUserId) {
        return requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_MEMBERS);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireStoreBelongsToWorkspace(UUID workspaceId, UUID storeId) {
        if (storeId == null) return;
        if (!storeRepository.existsByIdAndWorkspaceId(storeId, workspaceId)) {
            throw new SecurityException("Shop/store không thuộc workspace hiện tại. Vui lòng kiểm tra storeId.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceContextDto describeContext(UUID workspaceId, UUID actorUserId) {
        Workspace workspace = requireActiveWorkspace(workspaceId);
        requireExistingUser(actorUserId);
        WorkspaceRole role = resolveRole(workspace, actorUserId);
        Set<WorkspacePermission> permissions = permissionsOf(role, workspace.getWorkspaceType());

        return WorkspaceContextDto.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getName())
                .workspaceType(workspace.getWorkspaceType())
                .actorUserId(actorUserId)
                .role(role)
                .permissions(permissions)
                .canRead(permissions.contains(WorkspacePermission.READ_WORKSPACE))
                .canImport(permissions.contains(WorkspacePermission.IMPORT_DATA))
                .canManageMembers(permissions.contains(WorkspacePermission.MANAGE_MEMBERS))
                .message(workspace.getWorkspaceType() == WorkspaceType.PERSONAL
                        ? "Đang dùng workspace cá nhân. Core feature giống workspace chung, nhưng chỉ owner được thao tác."
                        : "Đang dùng organization workspace. Owner/Admin có thể mời thêm thành viên.")
                .build();
    }

    private Workspace requireActiveWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy workspace. Vui lòng kiểm tra workspaceId."));
        if (workspace.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new SecurityException("Workspace không còn hoạt động hoặc đã bị khóa/lưu trữ.");
        }
        return workspace;
    }

    private void requireExistingUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("Không tìm thấy user thao tác. Hiện MVP local cần actorUserId hợp lệ từ Swagger test data.");
        }
    }

    private WorkspaceMember requireActiveMember(UUID workspaceId, UUID actorUserId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserIdAndStatus(workspaceId, actorUserId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new SecurityException("Bạn chưa là thành viên active của workspace này."));
    }

    private WorkspaceRole resolveRole(Workspace workspace, UUID actorUserId) {
        if (workspace.getWorkspaceType() == WorkspaceType.PERSONAL) {
            if (!workspace.getOwnerUserId().equals(actorUserId)) {
                throw new SecurityException("Bạn không có quyền truy cập workspace cá nhân này.");
            }
            return WorkspaceRole.OWNER;
        }
        return requireActiveMember(workspace.getId(), actorUserId).getRole();
    }

    private Set<WorkspacePermission> permissionsOf(WorkspaceRole role, WorkspaceType type) {
        if (type == WorkspaceType.PERSONAL) {
            return EnumSet.of(
                    WorkspacePermission.READ_WORKSPACE,
                    WorkspacePermission.IMPORT_DATA,
                    WorkspacePermission.MANAGE_STORES,
                    WorkspacePermission.MANAGE_FINANCIAL_DATA
            );
        }

        return switch (role) {
            case OWNER -> EnumSet.allOf(WorkspacePermission.class);
            case ADMIN -> EnumSet.of(
                    WorkspacePermission.READ_WORKSPACE,
                    WorkspacePermission.IMPORT_DATA,
                    WorkspacePermission.MANAGE_STORES,
                    WorkspacePermission.MANAGE_MEMBERS,
                    WorkspacePermission.MANAGE_FINANCIAL_DATA
            );
            case MEMBER -> EnumSet.of(
                    WorkspacePermission.READ_WORKSPACE,
                    WorkspacePermission.IMPORT_DATA
            );
            case ACCOUNTANT -> EnumSet.of(
                    WorkspacePermission.READ_WORKSPACE,
                    WorkspacePermission.IMPORT_DATA,
                    WorkspacePermission.MANAGE_FINANCIAL_DATA
            );
            case VIEWER -> EnumSet.of(WorkspacePermission.READ_WORKSPACE);
        };
    }
}
