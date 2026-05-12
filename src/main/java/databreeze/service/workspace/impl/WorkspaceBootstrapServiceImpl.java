package databreeze.service.workspace.impl;

import databreeze.entity.User;
import databreeze.entity.Workspace;
import databreeze.entity.WorkspaceMember;
import databreeze.enums.MemberStatus;
import databreeze.enums.WorkspaceRole;
import databreeze.enums.WorkspaceStatus;
import databreeze.enums.WorkspaceType;
import databreeze.repository.WorkspaceMemberRepository;
import databreeze.repository.WorkspaceRepository;
import databreeze.service.workspace.WorkspaceBootstrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WorkspaceBootstrapServiceImpl implements WorkspaceBootstrapService {
    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public Workspace getOrCreatePersonalWorkspace(User user) {
        Workspace workspace = workspaceRepository
                .findFirstByOwnerUserIdAndWorkspaceTypeAndStatus(user.getId(), WorkspaceType.PERSONAL, WorkspaceStatus.ACTIVE)
                .orElseGet(() -> workspaceRepository.save(Workspace.builder()
                        .name(defaultPersonalWorkspaceName(user))
                        .workspaceType(WorkspaceType.PERSONAL)
                        .ownerUserId(user.getId())
                        .countryCode("VN")
                        .currencyCode("VND")
                        .timezone("Asia/Ho_Chi_Minh")
                        .billingEmail(user.getEmail())
                        .status(WorkspaceStatus.ACTIVE)
                        .build()));

        ensureOwnerMembership(workspace.getId(), user.getId());
        return workspace;
    }

    @Override
    @Transactional
    public Workspace getOrCreateOrganizationWorkspace(User owner, String organizationName) {
        Workspace workspace = workspaceRepository.findByOwnerUserIdAndStatus(owner.getId(), WorkspaceStatus.ACTIVE)
                .stream()
                .filter(item -> item.getWorkspaceType() == WorkspaceType.ORGANIZATION)
                .findFirst()
                .orElseGet(() -> workspaceRepository.save(Workspace.builder()
                        .name(organizationName == null || organizationName.isBlank() ? "Workspace bán hàng chung" : organizationName)
                        .workspaceType(WorkspaceType.ORGANIZATION)
                        .ownerUserId(owner.getId())
                        .countryCode("VN")
                        .currencyCode("VND")
                        .timezone("Asia/Ho_Chi_Minh")
                        .businessName(organizationName)
                        .billingEmail(owner.getEmail())
                        .status(WorkspaceStatus.ACTIVE)
                        .build()));

        ensureOwnerMembership(workspace.getId(), owner.getId());
        return workspace;
    }

    @Override
    @Transactional
    public void ensureOwnerMembership(UUID workspaceId, UUID ownerUserId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserIdAndStatus(workspaceId, ownerUserId, MemberStatus.ACTIVE)
                .orElseGet(() -> workspaceMemberRepository.save(WorkspaceMember.builder()
                        .workspaceId(workspaceId)
                        .userId(ownerUserId)
                        .role(WorkspaceRole.OWNER)
                        .status(MemberStatus.ACTIVE)
                        .joinedAt(OffsetDateTime.now())
                        .build()));
    }

    private String defaultPersonalWorkspaceName(User user) {
        String name = user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
        return "Tài khoản cá nhân - " + name;
    }
}
