package databreeze.dto.etl;


import databreeze.enums.ImportJobStatus;
import databreeze.enums.ImportJobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobListItemResponse {
    public UUID id;
    private UUID workspaceId;
    private UUID uploadId;
    private UUID targetSchemaId;
    private ImportJobStatus status;
    private ImportJobType jobType;
    private Long totalRows;
    private Long successRows;
    private Long failedRows;
    private Long warningRows;
    private String errorMessage;
    private String errorReportDownloadUrl;
    private OffsetDateTime startedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime failedAt;
}
