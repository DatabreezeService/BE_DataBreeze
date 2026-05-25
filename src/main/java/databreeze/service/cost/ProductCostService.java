package databreeze.service.cost;

import databreeze.dto.cost.ApplyProductCostRequest;
import databreeze.dto.cost.ApplyProductCostResponse;
import databreeze.dto.cost.BulkProductCostRequest;
import databreeze.dto.cost.MissingCostSkuResponse;
import databreeze.dto.cost.ProductCostRequest;
import databreeze.dto.cost.ProductCostResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductCostService {
    List<ProductCostResponse> listCosts(UUID workspaceId, UUID actorUserId, String sku);

    ProductCostResponse createCost(UUID workspaceId, UUID actorUserId, ProductCostRequest request);

    List<ProductCostResponse> bulkCreateCosts(UUID workspaceId, UUID actorUserId, BulkProductCostRequest request);

    ProductCostResponse updateCost(UUID workspaceId, UUID actorUserId, UUID costId, ProductCostRequest request);

    void deleteCost(UUID workspaceId, UUID actorUserId, UUID costId);

    ApplyProductCostResponse applyCosts(UUID workspaceId, UUID actorUserId, ApplyProductCostRequest request);

    List<MissingCostSkuResponse> listMissingCostSkus(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate);
}
