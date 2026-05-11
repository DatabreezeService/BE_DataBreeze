package databreeze.entity;

import databreeze.entity.enums.InsightGeneratedBy;
import databreeze.entity.enums.InsightSeverity;
import databreeze.entity.enums.InsightStatus;
import databreeze.entity.enums.InsightType;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "business_insights")
public class BusinessInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", nullable = false, length = 50)
    private InsightType insightType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    @Builder.Default
    private InsightSeverity severity = InsightSeverity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private InsightStatus status = InsightStatus.OPEN;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "summary", nullable = false)
    private String summary;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "metric_name", length = 100)
    private String metricName;

    @Column(name = "metric_value")
    private BigDecimal metricValue;

    @Column(name = "comparison_value")
    private BigDecimal comparisonValue;

    @Column(name = "change_percent")
    private BigDecimal changePercent;

    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 50)
    @Builder.Default
    private InsightGeneratedBy generatedBy = InsightGeneratedBy.SYSTEM_RULE;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
