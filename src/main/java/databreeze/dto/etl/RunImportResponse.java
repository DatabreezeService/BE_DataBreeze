package databreeze.dto.etl;

import databreeze.enums.ImportJobStatus;
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
public class RunImportResponse {
    private UUID importJobId;
    private UUID storeId;
    private ImportJobStatus status;
    private long totalRows;
    private long successRows;
    private long failedRows;
    private long warningRows;
    private long createdOrders;
    private long createdOrderItems;
    private long createdProducts;
    private LocalDate minBusinessDate;
    private LocalDate maxBusinessDate;
    private int revenueDailyRows;
    private int profitDailyRows;
    private String errorReportDownloadUrl;
    private String dashboardUrl;
    private String processedDataUrl;
    private String insightGenerateUrl;
    private String nextStep;
    private String message;

    public RunImportResponse(
            UUID importJobId,
            ImportJobStatus status,
            long totalRows,
            long successRows,
            long failedRows,
            long warningRows,
            long createdOrders,
            long createdOrderItems,
            long createdProducts,
            String errorReportDownloadUrl,
            String nextStep,
            String message
    ) {
        this.importJobId = importJobId;
        this.status = status;
        this.totalRows = totalRows;
        this.successRows = successRows;
        this.failedRows = failedRows;
        this.warningRows = warningRows;
        this.createdOrders = createdOrders;
        this.createdOrderItems = createdOrderItems;
        this.createdProducts = createdProducts;
        this.errorReportDownloadUrl = errorReportDownloadUrl;
        this.nextStep = nextStep;
        this.message = message;
    }
}
