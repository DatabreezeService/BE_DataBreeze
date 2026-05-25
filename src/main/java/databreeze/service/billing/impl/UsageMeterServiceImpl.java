package databreeze.service.billing.impl;

import databreeze.dto.billing.SubscriptionResponse;
import databreeze.dto.billing.UsageResponse;
import databreeze.entity.Plan;
import databreeze.entity.Subscription;
import databreeze.entity.UsageCounter;
import databreeze.enums.SubscriptionStatus;
import databreeze.repository.PlanRepository;
import databreeze.repository.SubscriptionRepository;
import databreeze.repository.UsageCounterRepository;
import databreeze.service.billing.UsageMeterService;
import databreeze.service.workspace.WorkspaceAccessService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageMeterServiceImpl implements UsageMeterService {
    private static final List<SubscriptionStatus> ACTIVE_STATUSES = List.of(
            SubscriptionStatus.TRIALING,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.PAST_DUE
    );

    @Autowired
    private BillingPlanCatalog planCatalog;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UsageCounterRepository usageCounterRepository;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Override
    @Transactional
    public SubscriptionResponse getSubscription(UUID workspaceId, UUID actorUserId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);
        return toSubscriptionResponse(subscription, plan, counter, "Subscription hiện tại của workspace.");
    }

    @Override
    @Transactional
    public UsageResponse getUsage(UUID workspaceId, UUID actorUserId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);
        return toUsageResponse(workspaceId, plan, counter);
    }

    @Override
    @Transactional
    public void recordUpload(UUID workspaceId, UUID actorUserId, long fileSizeBytes) {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);

        if (plan.getMaxFileSizeMb() != null && toMegabytes(fileSizeBytes) > plan.getMaxFileSizeMb()) {
            throw new IllegalStateException("File vượt giới hạn " + plan.getMaxFileSizeMb() + "MB của gói " + plan.getName() + ".");
        }
        if (plan.getMaxUploadsPerMonth() != null && counter.getUploadCount() + 1 > plan.getMaxUploadsPerMonth()) {
            throw new IllegalStateException("Workspace đã hết lượt upload trong kỳ hiện tại. Vui lòng nâng cấp gói hoặc đợi kỳ mới.");
        }
        counter.setUploadCount(counter.getUploadCount() + 1);
        usageCounterRepository.save(counter);
    }

    @Override
    @Transactional
    public void recordImportedRows(UUID workspaceId, UUID actorUserId, long importedRows) {
        if (importedRows <= 0) {
            return;
        }
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);
        if (plan.getMaxRowsPerMonth() != null && counter.getImportedRowCount() + importedRows > plan.getMaxRowsPerMonth()) {
            throw new IllegalStateException("Số dòng import vượt giới hạn tháng của gói " + plan.getName() + ".");
        }
        counter.setImportedRowCount(counter.getImportedRowCount() + importedRows);
        usageCounterRepository.save(counter);
    }

    @Override
    @Transactional
    public void ensureAiMappingAvailable(UUID workspaceId, UUID actorUserId) {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        if (!Boolean.TRUE.equals(plan.getAllowAiMapping())) {
            throw new IllegalStateException("Gói hiện tại chưa bật AI mapping.");
        }
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);
        if (plan.getAiTokenLimitPerMonth() != null && counter.getAiTotalTokens() >= plan.getAiTokenLimitPerMonth()) {
            throw new IllegalStateException("Workspace đã hết token AI trong kỳ hiện tại. Vui lòng nâng cấp gói hoặc giảm lượng dữ liệu gửi AI.");
        }
    }

    @Override
    @Transactional
    public void recordAiMapping(UUID workspaceId, UUID actorUserId, long actualInputTokens, long actualOutputTokens) {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        if (!Boolean.TRUE.equals(plan.getAllowAiMapping())) {
            throw new IllegalStateException("Gói hiện tại chưa bật AI mapping.");
        }
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);
        consumeAiTokens(plan, counter, Math.max(0, actualInputTokens), Math.max(0, actualOutputTokens));
        counter.setAiMappingCount(counter.getAiMappingCount() + 1);
        usageCounterRepository.save(counter);
    }

    @Override
    @Transactional
    public void recordInsightGeneration(UUID workspaceId, UUID actorUserId, long actualInputTokens, long actualOutputTokens) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        Subscription subscription = getOrCreateCurrentSubscription(workspaceId);
        Plan plan = requirePlan(subscription);
        if (!Boolean.TRUE.equals(plan.getAllowInsights())) {
            throw new IllegalStateException("Gói hiện tại chưa bật tạo insight.");
        }
        UsageCounter counter = getOrCreateCounter(workspaceId, subscription);
        if (plan.getInsightGenerationLimitPerMonth() != null
                && counter.getInsightGenerationCount() + 1 > plan.getInsightGenerationLimitPerMonth()) {
            throw new IllegalStateException("Workspace đã hết lượt tạo insight trong kỳ hiện tại.");
        }
        consumeAiTokens(plan, counter, Math.max(0, actualInputTokens), Math.max(0, actualOutputTokens));
        counter.setInsightGenerationCount(counter.getInsightGenerationCount() + 1);
        usageCounterRepository.save(counter);
    }

    private void consumeAiTokens(Plan plan, UsageCounter counter, long inputTokens, long outputTokens) {
        long totalTokens = inputTokens + outputTokens;
        if (plan.getAiTokenLimitPerMonth() != null && counter.getAiTotalTokens() + totalTokens > plan.getAiTokenLimitPerMonth()) {
            throw new IllegalStateException("Workspace đã hết token AI trong kỳ hiện tại. Vui lòng nâng cấp gói hoặc giảm lượng dữ liệu gửi AI.");
        }
        counter.setAiInputTokens(counter.getAiInputTokens() + inputTokens);
        counter.setAiOutputTokens(counter.getAiOutputTokens() + outputTokens);
        counter.setAiTotalTokens(counter.getAiTotalTokens() + totalTokens);
    }

    private Subscription getOrCreateCurrentSubscription(UUID workspaceId) {
        planCatalog.ensureDefaultPlans();
        OffsetDateTime now = OffsetDateTime.now();
        return subscriptionRepository.findFirstByWorkspaceIdAndStatusInOrderByCurrentPeriodEndDesc(workspaceId, ACTIVE_STATUSES)
                .filter(subscription -> !subscription.getCurrentPeriodEnd().isBefore(now))
                .orElseGet(() -> createTrialSubscription(workspaceId, now));
    }

    private Subscription createTrialSubscription(UUID workspaceId, OffsetDateTime now) {
        Plan trialPlan = planCatalog.requirePlan("FREE_TRIAL");
        return subscriptionRepository.save(Subscription.builder()
                .workspaceId(workspaceId)
                .planId(trialPlan.getId())
                .status(SubscriptionStatus.TRIALING)
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusDays(14))
                .trialStart(now)
                .trialEnd(now.plusDays(14))
                .cancelAtPeriodEnd(false)
                .build());
    }

    private Plan requirePlan(Subscription subscription) {
        return planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy gói của subscription hiện tại."));
    }

    private UsageCounter getOrCreateCounter(UUID workspaceId, Subscription subscription) {
        LocalDate periodStart = subscription.getCurrentPeriodStart().toLocalDate();
        LocalDate periodEnd = subscription.getCurrentPeriodEnd().toLocalDate();
        return usageCounterRepository.findByWorkspaceIdAndPeriodStartAndPeriodEnd(workspaceId, periodStart, periodEnd)
                .orElseGet(() -> usageCounterRepository.save(UsageCounter.builder()
                        .workspaceId(workspaceId)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .build()));
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription subscription, Plan plan, UsageCounter counter, String message) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .workspaceId(subscription.getWorkspaceId())
                .status(subscription.getStatus())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .trialStart(subscription.getTrialStart())
                .trialEnd(subscription.getTrialEnd())
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .plan(planCatalog.toResponse(plan))
                .usage(toUsageResponse(subscription.getWorkspaceId(), plan, counter))
                .message(message)
                .build();
    }

    private UsageResponse toUsageResponse(UUID workspaceId, Plan plan, UsageCounter counter) {
        Long tokenLimit = plan.getAiTokenLimitPerMonth();
        return UsageResponse.builder()
                .workspaceId(workspaceId)
                .periodStart(counter.getPeriodStart())
                .periodEnd(counter.getPeriodEnd())
                .uploadCount(counter.getUploadCount())
                .uploadLimit(plan.getMaxUploadsPerMonth() == null ? null : plan.getMaxUploadsPerMonth().longValue())
                .importedRowCount(counter.getImportedRowCount())
                .importedRowLimit(plan.getMaxRowsPerMonth())
                .aiMappingCount(counter.getAiMappingCount())
                .insightGenerationCount(counter.getInsightGenerationCount())
                .insightGenerationLimit(plan.getInsightGenerationLimitPerMonth() == null ? null : plan.getInsightGenerationLimitPerMonth().longValue())
                .aiInputTokens(counter.getAiInputTokens())
                .aiOutputTokens(counter.getAiOutputTokens())
                .aiTotalTokens(counter.getAiTotalTokens())
                .aiTokenLimit(tokenLimit)
                .aiTokenRemaining(tokenLimit == null ? null : Math.max(0, tokenLimit - counter.getAiTotalTokens()))
                .build();
    }

    private long toMegabytes(long bytes) {
        return (long) Math.ceil(bytes / 1024.0 / 1024.0);
    }
}
