package databreeze.repository;

import databreeze.entity.Order;
import databreeze.enums.CommercePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByWorkspaceIdAndPlatformAndExternalOrderId(UUID workspaceId, CommercePlatform platform, String externalOrderId);
}
