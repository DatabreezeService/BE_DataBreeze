package databreeze.dto.shopee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopeeDailyCalculationResult {
    private UUID workspaceId;
    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int revenueDailyRows;
    private int profitDailyRows;
    private String message;
}
