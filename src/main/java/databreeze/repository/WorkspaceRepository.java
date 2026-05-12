package databreeze.repository;

import databreeze.entity.Workspace;
import databreeze.enums.WorkspaceStatus;
import databreeze.enums.WorkspaceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    Optional<Workspace> findFirstByOwnerUserIdAndWorkspaceTypeAndStatus(UUID ownerUserId, WorkspaceType workspaceType, WorkspaceStatus status);

    List<Workspace> findByOwnerUserIdAndStatus(UUID ownerUserId, WorkspaceStatus status);
}
