package databreeze.dto.etl;

import databreeze.entity.enums.ImportJobStatus;

import java.util.UUID;

public record RunImportResponse(
        UUID importJobId,
        ImportJobStatus status,
        long totalRows,
        long successRows,
        long failedRows,
        long warningRows,
        long createdOrders,
        long createdOrderItems,
        long createdProducts,
        String nextStep,
        String message
) {
}
