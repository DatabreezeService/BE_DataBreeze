package databreeze.dto.etl;

import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import databreeze.enums.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UploadListItemResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID storeId;
    private UUID uploadedBy;
    private DataSourceType type;
    private SourcePlatform platform;
    private String originalFilename;
    private Long fileSizeBytes;
    private Long totalRows;
    private Long totalColumns;
    private UploadStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
