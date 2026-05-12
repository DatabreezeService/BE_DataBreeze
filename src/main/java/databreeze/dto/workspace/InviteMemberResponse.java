package databreeze.dto.workspace;

import databreeze.enums.InvitationStatus;
import databreeze.enums.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteMemberResponse {
    private UUID invitationId;
    private UUID workspaceId;
    private String invitedEmail;
    private UUID invitedByUserId;
    private WorkspaceRole role;
    private InvitationStatus status;
    private String inviteToken;
    private OffsetDateTime expiresAt;
    private String nextStep;
    private String message;
}
