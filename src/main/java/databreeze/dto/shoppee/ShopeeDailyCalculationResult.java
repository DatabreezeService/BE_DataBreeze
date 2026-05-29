package databreeze.dto.shoppee;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.util.UUID;

/**
 *
 * Kết quả tính revenue_daily/profit_daily cho Shopee
 */
public record ShopeeDailyCalculationResult (
        UUID workspaceId,
        UUID storeId,
        LocalDate fromDate,
        LocalDate toDate,
        int revenueDailyRows,
        int profitDailyRows,
        String message
) {
}
