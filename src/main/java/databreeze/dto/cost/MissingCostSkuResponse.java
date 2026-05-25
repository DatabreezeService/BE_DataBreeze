package databreeze.dto.cost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingCostSkuResponse {
    private String sku;
    private String productName;
    private long orderItemCount;
    private long totalQuantity;
    private BigDecimal netRevenueAmount;
    private LocalDate firstBusinessDate;
    private LocalDate lastBusinessDate;
}
