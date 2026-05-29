package databreeze.dto.billing;

import databreeze.enums.BillingCycle;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private BillingCycle billingCycle;
    private BigDecimal priceAmount;
    private String currencyCode;
    private Integer maxMembers;
    private Integer maxStores;
    private Integer maxUploadsPerMonth;
    private Long maxRowsPerMonth;
    private Integer maxFileSizeMb;
    private Long aiTokenLimitPerMonth;
    private Integer insightGenerationLimitPerMonth;
    private String recommendedAiModel;
    private Boolean allowAiMapping;
    private Boolean allowProfitDashboard;
    private Boolean allowInsights;
    private Boolean allowPrioritySupport;
    private BigDecimal estimatedModelCostUsd;
    private BigDecimal targetGrossMarginPercent;
}
