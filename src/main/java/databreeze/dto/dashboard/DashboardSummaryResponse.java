package databreeze.dto.dashboard;


import databreeze.enums.SourcePlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private UUID workspaceId;
    private UUID storeId;
    private SourcePlatform sourcePlatform;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long orderCount;
    private long itemQuantity;
    private BigDecimal grossRevenueAmount;
    private BigDecimal discountAmount;
    private BigDecimal refundAmount;
    private BigDecimal netRevenueAmount;
    private BigDecimal cogsAmount;
    private BigDecimal platformFeeAmount;
    private BigDecimal transactionFeeAmount;
    private BigDecimal shippingFeeAmount;
    private BigDecimal adSpendAmount;
    private BigDecimal operatingExpenseAmount;
    private BigDecimal netProfitAmount;
    private BigDecimal profitMargin;
    private BigDecimal averageOrderValue;
}
