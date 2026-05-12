package databreeze.entity;

import databreeze.enums.ImportJobStatus;
import databreeze.enums.ImportJobType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_jobs")
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Column(name = "target_schema_id")
    private UUID targetSchemaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private ImportJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ImportJobStatus status = ImportJobStatus.PENDING;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Long totalRows = 0L;

    @Column(name = "success_rows", nullable = false)
    @Builder.Default
    private Long successRows = 0L;

    @Column(name = "failed_rows", nullable = false)
    @Builder.Default
    private Long failedRows = 0L;

    @Column(name = "warning_rows", nullable = false)
    @Builder.Default
    private Long warningRows = 0L;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
