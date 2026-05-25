package databreeze.dto.cost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyProductCostRequest {
    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private boolean recalculateDashboard = true;
}
