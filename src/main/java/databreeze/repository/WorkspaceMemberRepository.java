package databreeze.repository;

import databreeze.entity.WorkspaceMember;
import databreeze.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndStatus(UUID workspaceId, UUID userId, MemberStatus status);

    Optional<WorkspaceMember> findFirstByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserIdAndStatus(UUID workspaceId, UUID userId, MemberStatus status);

    List<WorkspaceMember> findByUserIdAndStatus(UUID userId, MemberStatus status);

    List<WorkspaceMember> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, MemberStatus status);
}
