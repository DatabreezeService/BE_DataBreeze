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
public class ImportJobStatusResponse {
    private UUID importJobId;
    private UUID uploadId;
    private UUID targetSchemaId;
    private ImportJobStatus status;
    private long totalRows;
    private long successRows;
    private long failedRows;
    private long warningRows;
    private String errorMessage;
    private String errorReportDownloadUrl;
}
