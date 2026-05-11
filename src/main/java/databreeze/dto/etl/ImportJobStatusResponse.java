package databreeze.dto.etl;

import databreeze.entity.enums.ImportJobStatus;

import java.util.UUID;

/**
 *  Response polling trạng thái job import.
 */
public record ImportJobStatusResponse(
        UUID importJobId,
        UUID uploadId,
        UUID targetSchemaId,
        ImportJobStatus status,
        long totalRows,
        long successRows,
        long failedROws,
        long warningRows,
        String errorMessage
) {}
