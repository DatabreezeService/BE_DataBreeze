package databreeze.entity;

import databreeze.entity.enums.RawRowStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "raw_import_rows")
public class RawImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Column(name = "import_job_id", nullable = false)
    private UUID importJobId;

    @Column(name = "row_number", nullable = false)
    private Long rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_preview", columnDefinition = "jsonb")
    private Map<String, Object> normalizedPreview;

    @Column(name = "row_hash", length = 128)
    private String rowHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RawRowStatus status = RawRowStatus.VALID;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_messages", columnDefinition = "jsonb")
    private Map<String, Object> errorMessages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warning_messages", columnDefinition = "jsonb")
    private Map<String, Object> warningMessages;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}
