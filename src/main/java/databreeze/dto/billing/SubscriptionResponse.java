package databreeze.dto.billing;

import databreeze.enums.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {
    private UUID id;
    private UUID workspaceId;
    private SubscriptionStatus status;
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private OffsetDateTime trialStart;
    private OffsetDateTime trialEnd;
    private Boolean cancelAtPeriodEnd;
    private PlanResponse plan;
    private UsageResponse usage;
    private String message;
}
