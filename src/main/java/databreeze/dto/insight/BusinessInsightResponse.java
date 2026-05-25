package databreeze.dto.insight;


import databreeze.enums.InsightSeverity;
import databreeze.enums.InsightStatus;
import databreeze.enums.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessInsightResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID storeId;
    private InsightType insightType;
    private InsightSeverity severity;
    private InsightStatus status;
    private String title;
    private String summary;
    private String explanation;
    private String metricName;
    private BigDecimal metricValue;
    private BigDecimal comparisonValue;
    private BigDecimal changePercent;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private OffsetDateTime generatedAt;
}
