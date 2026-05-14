package databreeze.repository;

import databreeze.entity.InsightAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsightActionRepository extends JpaRepository<InsightAction, UUID> {
    List<InsightAction> findByInsightIdOrderByPriorityDescCreatedAtAsc(UUID insightId);
}
