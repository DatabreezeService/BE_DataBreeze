package databreeze.repository;

import databreeze.entity.Subscription;
import databreeze.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findFirstByWorkspaceIdAndStatusInOrderByCurrentPeriodEndDesc(UUID workspaceId, List<SubscriptionStatus> statuses);

    List<Subscription> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
