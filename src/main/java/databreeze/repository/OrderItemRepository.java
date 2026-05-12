package databreeze.repository;

import databreeze.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    long deleteByOrderId(UUID orderId);
    List<OrderItem> findByWorkspaceId(UUID workspaceId);
}
