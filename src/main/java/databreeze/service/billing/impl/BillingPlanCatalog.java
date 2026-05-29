package databreeze.service.billing.impl;

import databreeze.dto.billing.PlanResponse;
import databreeze.entity.Plan;
import databreeze.enums.BillingCycle;
import databreeze.enums.PlanStatus;
import databreeze.repository.PlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingPlanCatalog {
    private static final BigDecimal INPUT_SHARE = BigDecimal.valueOf(0.70);
    private static final BigDecimal OUTPUT_SHARE = BigDecimal.valueOf(0.30);
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    @Autowired
    private PlanRepository planRepository;

    @Value("${app.billing.ai.input-price-usd-per-1m:0.40}")
    private BigDecimal inputPriceUsdPerMillion;

    @Value("${app.billing.ai.output-price-usd-per-1m:1.60}")
    private BigDecimal outputPriceUsdPerMillion;

    @Value("${app.billing.usd-to-vnd:25000}")
    private BigDecimal usdToVnd;

    @Transactional
    public void ensureDefaultPlans() {
        saveIfMissing(defaultPlan("FREE_TRIAL", "Free Trial", "5 lượt dùng thử để kiểm tra ETL, AI mapping và insight cơ bản.",
                BigDecimal.ZERO, 1, 1, 5, 10_000L, 10, 50_000L, 5, true, true, true, false));
        saveIfMissing(defaultPlan("STARTER", "Starter", "Cho shop nhỏ bắt đầu gom dữ liệu Shopee và xem insight lợi nhuận.",
                BigDecimal.valueOf(99_000), 1, 1, 50, 200_000L, 20, 1_000_000L, 30, true, true, true, false));
        saveIfMissing(defaultPlan("GROWTH", "Growth", "Cho team bán hàng cần nhiều import, dashboard lợi nhuận và phân tích thường xuyên.",
                BigDecimal.valueOf(299_000), 5, 3, 200, 1_000_000L, 50, 4_000_000L, 120, true, true, true, true));
        saveIfMissing(defaultPlan("BUSINESS", "Business", "Cho doanh nghiệp cần quota lớn, nhiều store và hỗ trợ ưu tiên.",
                BigDecimal.valueOf(799_000), 20, 10, 1_000, 5_000_000L, 100, 12_000_000L, 400, true, true, true, true));
    }

    @Transactional(readOnly = true)
    public List<Plan> activePlans() {
        return planRepository.findByStatusOrderByPriceAmountAsc(PlanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Plan requirePlan(String planCode) {
        return planRepository.findByCode(normalizePlanCode(planCode))
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy gói subscription: " + planCode));
    }

    public PlanResponse toResponse(Plan plan) {
        BigDecimal estimatedModelCostUsd = estimateModelCost(plan.getAiTokenLimitPerMonth());
        return PlanResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .billingCycle(plan.getBillingCycle())
                .priceAmount(plan.getPriceAmount())
                .currencyCode(plan.getCurrencyCode())
                .maxMembers(plan.getMaxMembers())
                .maxStores(plan.getMaxStores())
                .maxUploadsPerMonth(plan.getMaxUploadsPerMonth())
                .maxRowsPerMonth(plan.getMaxRowsPerMonth())
                .maxFileSizeMb(plan.getMaxFileSizeMb())
                .aiTokenLimitPerMonth(plan.getAiTokenLimitPerMonth())
                .insightGenerationLimitPerMonth(plan.getInsightGenerationLimitPerMonth())
                .recommendedAiModel(plan.getRecommendedAiModel())
                .allowAiMapping(plan.getAllowAiMapping())
                .allowProfitDashboard(plan.getAllowProfitDashboard())
                .allowInsights(plan.getAllowInsights())
                .allowPrioritySupport(plan.getAllowPrioritySupport())
                .estimatedModelCostUsd(estimatedModelCostUsd)
                .targetGrossMarginPercent(estimateGrossMargin(plan.getPriceAmount(), estimatedModelCostUsd))
                .build();
    }

    private void saveIfMissing(Plan plan) {
        if (planRepository.findByCode(plan.getCode()).isEmpty()) {
            planRepository.save(plan);
        }
    }

    private Plan defaultPlan(
            String code,
            String name,
            String description,
            BigDecimal price,
            Integer maxMembers,
            Integer maxStores,
            Integer maxUploads,
            Long maxRows,
            Integer maxFileSizeMb,
            Long aiTokenLimit,
            Integer insightLimit,
            boolean allowAiMapping,
            boolean allowProfitDashboard,
            boolean allowInsights,
            boolean allowPrioritySupport
    ) {
        return Plan.builder()
                .code(code)
                .name(name)
                .description(description)
                .billingCycle(BillingCycle.MONTHLY)
                .priceAmount(price)
                .currencyCode("VND")
                .maxMembers(maxMembers)
                .maxStores(maxStores)
                .maxUploadsPerMonth(maxUploads)
                .maxRowsPerMonth(maxRows)
                .maxFileSizeMb(maxFileSizeMb)
                .aiTokenLimitPerMonth(aiTokenLimit)
                .insightGenerationLimitPerMonth(insightLimit)
                .recommendedAiModel("gpt-4.1-mini")
                .allowAiMapping(allowAiMapping)
                .allowProfitDashboard(allowProfitDashboard)
                .allowInsights(allowInsights)
                .allowPrioritySupport(allowPrioritySupport)
                .status(PlanStatus.ACTIVE)
                .build();
    }

    private BigDecimal estimateModelCost(Long tokenLimit) {
        if (tokenLimit == null || tokenLimit <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal blendedPrice = inputPriceUsdPerMillion.multiply(INPUT_SHARE)
                .add(outputPriceUsdPerMillion.multiply(OUTPUT_SHARE));
        return BigDecimal.valueOf(tokenLimit)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(blendedPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateGrossMargin(BigDecimal priceVnd, BigDecimal modelCostUsd) {
        if (priceVnd == null || priceVnd.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal modelCostVnd = modelCostUsd.multiply(usdToVnd);
        return BigDecimal.ONE.subtract(modelCostVnd.divide(priceVnd, 4, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePlanCode(String planCode) {
        return planCode == null ? "" : planCode.trim().toUpperCase();
    }
}
