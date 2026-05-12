package databreeze.service.workspace.impl;

import databreeze.dto.workspace.MyWorkspaceResponse;
import databreeze.dto.workspace.WorkspaceSwitcherResponse;
import databreeze.entity.User;
import databreeze.entity.Workspace;
import databreeze.entity.WorkspaceMember;
import databreeze.enums.MemberStatus;
import databreeze.enums.WorkspacePermission;
import databreeze.enums.WorkspaceRole;
import databreeze.enums.WorkspaceStatus;
import databreeze.enums.WorkspaceType;
import databreeze.repository.UserRepository;
import databreeze.repository.WorkspaceMemberRepository;
import databreeze.repository.WorkspaceRepository;
import databreeze.service.workspace.WorkspaceBootstrapService;
import databreeze.service.workspace.WorkspaceQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceQueryServiceImpl implements WorkspaceQueryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceBootstrapService workspaceBootstrapService;

    @Override
    @Transactional
    public WorkspaceSwitcherResponse listMyWorkspaces(UUID actorUserId) {
        User user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy user. MVP chưa setup login nên cần truyền actorUserId hợp lệ."));

        Workspace personalWorkspace = workspaceBootstrapService.getOrCreatePersonalWorkspace(user);
        List<WorkspaceMember> activeMemberships = workspaceMemberRepository.findByUserIdAndStatus(actorUserId, MemberStatus.ACTIVE);

        Map<UUID, MyWorkspaceResponse> uniqueWorkspaces = new LinkedHashMap<>();
        uniqueWorkspaces.put(personalWorkspace.getId(), toResponse(personalWorkspace, WorkspaceRole.OWNER, true));

        for (WorkspaceMember membership : activeMemberships) {
            workspaceRepository.findById(membership.getWorkspaceId())
                    .filter(workspace -> workspace.getStatus() == WorkspaceStatus.ACTIVE)
                    .ifPresent(workspace -> uniqueWorkspaces.put(workspace.getId(), toResponse(
                            workspace,
                            resolveRoleForResponse(workspace, membership),
                            workspace.getWorkspaceType() == WorkspaceType.PERSONAL
                    )));
        }

        List<MyWorkspaceResponse> workspaces = new ArrayList<>(uniqueWorkspaces.values());
        workspaces.sort(Comparator
                .comparing(MyWorkspaceResponse::getPersonalWorkspace).reversed()
                .thenComparing(MyWorkspaceResponse::getWorkspaceName, String.CASE_INSENSITIVE_ORDER));

        return WorkspaceSwitcherResponse.builder()
                .actorUserId(actorUserId)
                .defaultWorkspaceId(personalWorkspace.getId())
                .workspaces(workspaces)
                .message("FE dùng danh sách này để làm workspace switcher. Sau khi user chọn workspace, mọi API nghiệp vụ dùng workspaceId đã chọn.")
                .build();
    }

    private WorkspaceRole resolveRoleForResponse(Workspace workspace, WorkspaceMember membership) {
        if (workspace.getWorkspaceType() == WorkspaceType.PERSONAL) {
            return WorkspaceRole.OWNER;
        }
        return membership.getRole();
    }

    private MyWorkspaceResponse toResponse(Workspace workspace, WorkspaceRole role, boolean defaultWorkspace) {
        Set<WorkspacePermission> permissions = permissionsOf(role, workspace.getWorkspaceType());
        return MyWorkspaceResponse.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getName())
                .workspaceType(workspace.getWorkspaceType())
                .role(role)
                .status(workspace.getStatus())
                .defaultWorkspace(defaultWorkspace)
                .personalWorkspace(workspace.getWorkspaceType() == WorkspaceType.PERSONAL)
                .organizationWorkspace(workspace.getWorkspaceType() == WorkspaceType.ORGANIZATION)
                .countryCode(workspace.getCountryCode())
                .currencyCode(workspace.getCurrencyCode())
                .timezone(workspace.getTimezone())
                .permissions(permissions)
                .message(workspace.getWorkspaceType() == WorkspaceType.PERSONAL
                        ? "Không gian cá nhân của user. Không hiển thị invite member."
                        : "Organization workspace. Owner/Admin có thể mời thêm thành viên.")
                .build();
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
