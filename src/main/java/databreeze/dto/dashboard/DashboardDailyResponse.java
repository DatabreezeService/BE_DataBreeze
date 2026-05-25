package databreeze.dto.dashboard;

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
public class DashboardDailyResponse {
    private LocalDate date;
    private long orderCount;
    private long itemQuantity;
    private BigDecimal grossRevenueAmount;
    private BigDecimal netRevenueAmount;
    private BigDecimal refundAmount;
    private BigDecimal operatingExpenseAmount;
    private BigDecimal netProfitAmount;
    private BigDecimal profitMargin;
}
