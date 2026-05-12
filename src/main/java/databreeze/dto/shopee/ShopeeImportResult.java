package databreeze.dto.shopee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopeeImportResult {
    private long totalRows;
    private long successRows;
    private long failedRows;
    private long warningRows;
    private long createdOrders;
    private long createdOrderItems;
    private long createdProducts;
    private LocalDate minBusinessDate;
    private LocalDate maxBusinessDate;
}
