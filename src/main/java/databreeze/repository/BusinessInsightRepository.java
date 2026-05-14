package databreeze.repository;

import databreeze.entity.BusinessInsight;
import databreeze.enums.InsightStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessInsightRepository extends JpaRepository<BusinessInsight, UUID> {
    List<BusinessInsight> findByWorkspaceIdOrderByGeneratedAtDesc(UUID workspaceId);

    List<BusinessInsight> findByWorkspaceIdAndStatusOrderByGeneratedAtDesc(UUID workspaceId, InsightStatus status);
}
