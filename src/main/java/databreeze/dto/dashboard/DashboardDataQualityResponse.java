package databreeze.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDataQualityResponse {
    private long totalProcessedOrders;
    private long ordersInSelectedRange;
    private long ordersMissingBusinessDate;
    private long orderItemsMissingCogs;
    private LocalDate firstBusinessDate;
    private LocalDate lastBusinessDate;
    private boolean selectedRangeHasData;
    private String message;
}
