package databreeze.service.workspace;

import databreeze.dto.workspace.*;

import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberService {
    InviteMemberResponse inviteMember(UUID workspaceId, UUID actorUserId, InviteMemberRequest request);

    AcceptInvitationResponse acceptInvitation(UUID actorUserId, AcceptInvitationRequest request);

    List<WorkspaceMemberResponse> listActiveMembers(UUID workspaceId, UUID actorUserId);
}
