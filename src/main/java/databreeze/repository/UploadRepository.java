package databreeze.repository;

import databreeze.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {
    List<Upload> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<Upload> findByWorkspaceIdAndStoreIdOrderByCreatedAtDesc(UUID workspaceId, UUID storeId);
}
