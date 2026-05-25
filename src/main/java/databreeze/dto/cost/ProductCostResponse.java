package databreeze.dto.cost;

import databreeze.enums.CostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCostResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID productId;
    private String sku;
    private CostType costType;
    private BigDecimal unitCost;
    private String currencyCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
