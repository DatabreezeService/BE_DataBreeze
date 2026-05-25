package databreeze.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardProductResponse {
    private UUID productId;
    private String sku;
    private String productName;
    private long orderItemCount;
    private long itemQuantity;
    private BigDecimal grossRevenueAmount;
    private BigDecimal netRevenueAmount;
    private BigDecimal cogsAmount;
    private BigDecimal netProfitAmount;
    private BigDecimal profitMargin;
}
