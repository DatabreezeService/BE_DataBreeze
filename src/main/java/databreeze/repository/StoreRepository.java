package databreeze.repository;

import databreeze.entity.Store;
import databreeze.enums.WorkspaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    boolean existsByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<Store> findFirstByWorkspaceIdAndStatus(UUID workspaceId, WorkspaceStatus status);
}
