package databreeze.repository;

import databreeze.entity.ProductCost;
import databreeze.enums.CostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCostRepository extends JpaRepository<ProductCost, UUID> {
    List<ProductCost> findByWorkspaceIdOrderBySkuAscEffectiveFromDesc(UUID workspaceId);

    List<ProductCost> findByWorkspaceIdAndCostTypeOrderBySkuAscEffectiveFromDesc(UUID workspaceId, CostType costType);

    List<ProductCost> findByWorkspaceIdAndSkuIgnoreCaseOrderByEffectiveFromDesc(UUID workspaceId, String sku);

    Optional<ProductCost> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
