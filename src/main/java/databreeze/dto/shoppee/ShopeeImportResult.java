package databreeze.dto.shoppee;

import java.time.LocalDate;

public record ShopeeImportResult(
        long totalRows,
        long successRows,
        long failedRows,
        long warningRows,
        long createdOrders,
        long createdOrderItems,
        long createdProducts,
        LocalDate minBusinessDate,
        LocalDate maxBusinessDate
) {
}
