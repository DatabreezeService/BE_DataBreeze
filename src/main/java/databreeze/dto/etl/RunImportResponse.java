package databreeze.dto.etl;

import databreeze.enums.ImportJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunImportResponse {
    private UUID importJobId;
    private ImportJobStatus status;
    private long totalRows;
    private long successRows;
    private long failedRows;
    private long warningRows;
    private long createdOrders;
    private long createdOrderItems;
    private long createdProducts;
    private String nextStep;
    private String message;
}
