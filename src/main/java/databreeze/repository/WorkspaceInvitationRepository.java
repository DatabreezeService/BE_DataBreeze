package databreeze.repository;

import databreeze.entity.WorkspaceInvitation;
import databreeze.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {
    Optional<WorkspaceInvitation> findByToken(String token);

    Optional<WorkspaceInvitation> findFirstByWorkspaceIdAndInvitedEmailAndStatusOrderByCreatedAtDesc(
            UUID workspaceId,
            String invitedEmail,
            InvitationStatus status
    );

    List<WorkspaceInvitation> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, InvitationStatus status);
}
