package databreeze.dto.cost;

import databreeze.dto.shopee.ShopeeDailyCalculationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyProductCostResponse {
    private UUID workspaceId;
    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long matchedOrderItems;
    private long updatedOrderItems;
    private long missingCostOrderItems;
    private List<String> missingSkus;
    private ShopeeDailyCalculationResult dailyCalculation;
    private String message;
}
