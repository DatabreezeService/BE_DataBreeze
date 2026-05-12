package databreeze.entity;

import databreeze.enums.InvitationStatus;
import databreeze.enums.WorkspaceRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "workspace_invitations")
public class WorkspaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "invited_email", nullable = false, length = 255)
    private String invitedEmail;

    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "token", nullable = false, unique = true, length = 120)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
