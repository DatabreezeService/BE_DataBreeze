package databreeze.service.workspace.impl;

import databreeze.dto.workspace.*;
import databreeze.entity.User;
import databreeze.entity.Workspace;
import databreeze.entity.WorkspaceInvitation;
import databreeze.entity.WorkspaceMember;
import databreeze.enums.*;
import databreeze.repository.UserRepository;
import databreeze.repository.WorkspaceInvitationRepository;
import databreeze.repository.WorkspaceMemberRepository;
import databreeze.service.workspace.WorkspaceAccessService;
import databreeze.service.workspace.WorkspaceMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private WorkspaceInvitationRepository invitationRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public InviteMemberResponse inviteMember(UUID workspaceId, UUID actorUserId, InviteMemberRequest request) {
        Workspace workspace = workspaceAccessService.requireManageMembersAccess(workspaceId, actorUserId);
        if (workspace.getWorkspaceType() != WorkspaceType.ORGANIZATION) {
            throw new IllegalArgumentException("Chỉ organization workspace mới được mời thêm thành viên. Workspace cá nhân dùng cho chính chủ tài khoản.");
        }
        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Không mời trực tiếp role OWNER. Owner chỉ nên được chuyển quyền bằng flow riêng ở phase sau.");
        }

        String invitedEmail = request.getEmail().trim().toLowerCase();
        WorkspaceInvitation invitation = invitationRepository
                .findFirstByWorkspaceIdAndInvitedEmailAndStatusOrderByCreatedAtDesc(workspaceId, invitedEmail, InvitationStatus.PENDING)
                .orElseGet(WorkspaceInvitation::new);

        invitation.setWorkspaceId(workspaceId);
        invitation.setInvitedEmail(invitedEmail);
        invitation.setInvitedByUserId(actorUserId);
        invitation.setRole(request.getRole());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        invitation.setExpiresAt(OffsetDateTime.now().plusDays(7));
        invitation = invitationRepository.save(invitation);

        return InviteMemberResponse.builder()
                .invitationId(invitation.getId())
                .workspaceId(workspaceId)
                .invitedEmail(invitedEmail)
                .invitedByUserId(actorUserId)
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .inviteToken(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .nextStep("Gửi inviteToken cho người được mời. Sau này thay bằng link email.")
                .message("Tạo lời mời thành công. Người được mời cần đăng nhập/đăng ký rồi gọi API accept invitation.")
                .build();
    }

    @Override
    @Transactional
    public AcceptInvitationResponse acceptInvitation(UUID actorUserId, AcceptInvitationRequest request) {
        User user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy user nhận lời mời."));
        WorkspaceInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new NoSuchElementException("Token lời mời không tồn tại."));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Lời mời không còn ở trạng thái PENDING.");
        }
        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalStateException("Lời mời đã hết hạn.");
        }
        if (!user.getEmail().equalsIgnoreCase(invitation.getInvitedEmail())) {
            throw new SecurityException("Email tài khoản hiện tại không khớp email được mời vào workspace.");
        }

        WorkspaceMember member = memberRepository.findFirstByWorkspaceIdAndUserId(invitation.getWorkspaceId(), actorUserId)
                .orElseGet(WorkspaceMember::new);
        member.setWorkspaceId(invitation.getWorkspaceId());
        member.setUserId(actorUserId);
        member.setRole(invitation.getRole());
        member.setStatus(MemberStatus.ACTIVE);
        member.setInvitedBy(invitation.getInvitedByUserId());
        member.setInvitedAt(invitation.getCreatedAt());
        member.setJoinedAt(OffsetDateTime.now());
        memberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        return AcceptInvitationResponse.builder()
                .workspaceId(invitation.getWorkspaceId())
                .userId(actorUserId)
                .role(invitation.getRole())
                .message("Bạn đã tham gia organization workspace thành công.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listActiveMembers(UUID workspaceId, UUID actorUserId) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.READ_WORKSPACE);
        return memberRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, MemberStatus.ACTIVE)
                .stream()
                .map(member -> WorkspaceMemberResponse.builder()
                        .memberId(member.getId())
                        .workspaceId(member.getWorkspaceId())
                        .userId(member.getUserId())
                        .role(member.getRole())
                        .status(member.getStatus())
                        .build())
                .toList();
    }
}
