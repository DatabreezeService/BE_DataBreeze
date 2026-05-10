package com.databreeze.entity;

import com.databreeze.enums.DataSourceType;
import com.databreeze.enums.SourcePlatform;
import com.databreeze.enums.UploadStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "uploads")
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "data_source_id")
    private UUID dataSourceId;

    @Column(name = "target_schema_id")
    private UUID targetSchemaId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source_type", nullable = false, length = 50)
    private DataSourceType dataSourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private SourcePlatform platform;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "file_mime_type", length = 255)
    private String fileMimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "detected_encoding", length = 100)
    private String detectedEncoding;

    @Column(name = "detected_delimiter", length = 20)
    private String detectedDelimiter;

    @Column(name = "total_rows")
    private Long totalRows;

    @Column(name = "total_columns")
    private Integer totalColumns;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private UploadStatus status = UploadStatus.UPLOADED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
