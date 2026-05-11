package databreeze.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
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
@Table(name = "usage_counters")
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "upload_count", nullable = false)
    @Builder.Default
    private Long uploadCount = 0L;

    @Column(name = "imported_row_count", nullable = false)
    @Builder.Default
    private Long importedRowCount = 0L;

    @Column(name = "ai_mapping_count", nullable = false)
    @Builder.Default
    private Long aiMappingCount = 0L;

    @Column(name = "insight_generation_count", nullable = false)
    @Builder.Default
    private Long insightGenerationCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
