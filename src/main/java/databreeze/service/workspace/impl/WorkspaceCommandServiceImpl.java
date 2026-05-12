package databreeze.service.workspace.impl;

import databreeze.dto.workspace.CreateOrganizationWorkspaceRequest;
import databreeze.dto.workspace.CreateOrganizationWorkspaceResponse;
import databreeze.dto.workspace.InitPersonalWorkspaceResponse;
import databreeze.entity.User;
import databreeze.entity.Workspace;
import databreeze.entity.WorkspaceMember;
import databreeze.enums.MemberStatus;
import databreeze.enums.WorkspaceRole;
import databreeze.enums.WorkspaceStatus;
import databreeze.enums.WorkspaceType;
import databreeze.repository.UserRepository;
import databreeze.repository.WorkspaceMemberRepository;
import databreeze.repository.WorkspaceRepository;
import databreeze.service.workspace.WorkspaceBootstrapService;
import databreeze.service.workspace.WorkspaceCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkspaceCommandServiceImpl implements WorkspaceCommandService {

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
    public InitPersonalWorkspaceResponse initPersonalWorkspace(UUID actorUserId) {
        User user = requireUser(actorUserId);
        Workspace personalWorkspace = workspaceBootstrapService.getOrCreatePersonalWorkspace(user);

        return InitPersonalWorkspaceResponse.builder()
                .workspaceId(personalWorkspace.getId())
                .workspaceName(personalWorkspace.getName())
                .workspaceType(personalWorkspace.getWorkspaceType())
                .ownerUserId(user.getId())
                .role(WorkspaceRole.OWNER)
                .message("Personal workspace đã sẵn sàng. FE có thể cho user chọn workspace này trong workspace switcher.")
                .build();
    }

    @Override
    @Transactional
    public CreateOrganizationWorkspaceResponse createOrganizationWorkspace(UUID actorUserId, CreateOrganizationWorkspaceRequest request) {
        User owner = requireUser(actorUserId);
        String workspaceName = request.getWorkspaceName().trim();
        String billingEmail = request.getBillingEmail() == null || request.getBillingEmail().isBlank()
                ? owner.getEmail()
                : request.getBillingEmail().trim().toLowerCase();

        Workspace workspace = Workspace.builder()
                .name(workspaceName)
                .workspaceType(WorkspaceType.ORGANIZATION)
                .ownerUserId(owner.getId())
                .countryCode("VN")
                .currencyCode("VND")
                .timezone("Asia/Ho_Chi_Minh")
                .businessName(request.getBusinessName())
                .taxCode(request.getTaxCode())
                .billingEmail(billingEmail)
                .status(WorkspaceStatus.ACTIVE)
                .build();
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(owner.getId())
                .role(WorkspaceRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(OffsetDateTime.now())
                .build();
        workspaceMemberRepository.save(ownerMember);

        return CreateOrganizationWorkspaceResponse.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getName())
                .workspaceType(workspace.getWorkspaceType())
                .ownerUserId(owner.getId())
                .ownerRole(WorkspaceRole.OWNER)
                .message("Tạo organization workspace thành công.")
                .nextStep("FE nên refresh GET /api/v1/me/workspaces để workspace mới xuất hiện trong workspace switcher.")
                .build();
    }

    private User requireUser(UUID actorUserId) {
        return userRepository.findById(actorUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy user. MVP chưa setup login nên cần truyền actorUserId hợp lệ."));
    }
}
