package databreeze.service.billing;

import databreeze.dto.billing.PlanResponse;
import databreeze.dto.billing.SubscriptionResponse;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    List<PlanResponse> listPlans();

    SubscriptionResponse subscribe(UUID workspaceId, UUID actorUserId, String planCode);
}
