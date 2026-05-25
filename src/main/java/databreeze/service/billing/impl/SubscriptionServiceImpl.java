package databreeze.service.billing.impl;

import databreeze.dto.billing.PlanResponse;
import databreeze.dto.billing.SubscriptionResponse;
import databreeze.entity.Plan;
import databreeze.entity.Subscription;
import databreeze.enums.SubscriptionStatus;
import databreeze.enums.WorkspacePermission;
import databreeze.repository.SubscriptionRepository;
import databreeze.service.billing.SubscriptionService;
import databreeze.service.billing.UsageMeterService;
import databreeze.service.workspace.WorkspaceAccessService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    @Autowired
    private BillingPlanCatalog planCatalog;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UsageMeterService usageMeterService;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Override
    @Transactional
    public List<PlanResponse> listPlans() {
        planCatalog.ensureDefaultPlans();
        return planCatalog.activePlans().stream()
                .map(planCatalog::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionResponse subscribe(UUID workspaceId, UUID actorUserId, String planCode) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_BILLING);
        planCatalog.ensureDefaultPlans();
        Plan plan = planCatalog.requirePlan(planCode);
        OffsetDateTime now = OffsetDateTime.now();

        subscriptionRepository.findFirstByWorkspaceIdAndStatusInOrderByCurrentPeriodEndDesc(
                workspaceId,
                List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE)
        ).ifPresent(active -> {
            active.setStatus(SubscriptionStatus.CANCELLED);
            active.setCancelledAt(now);
            active.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(active);
        });

        Subscription subscription = subscriptionRepository.save(Subscription.builder()
                .workspaceId(workspaceId)
                .planId(plan.getId())
                .status("FREE_TRIAL".equals(plan.getCode()) ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusMonths(1))
                .trialStart("FREE_TRIAL".equals(plan.getCode()) ? now : null)
                .trialEnd("FREE_TRIAL".equals(plan.getCode()) ? now.plusDays(14) : null)
                .cancelAtPeriodEnd(false)
                .build());

        SubscriptionResponse response = usageMeterService.getSubscription(workspaceId, actorUserId);
        response.setMessage("Đã kích hoạt gói " + plan.getName() + " cho workspace.");
        response.setId(subscription.getId());
        return response;
    }
}
