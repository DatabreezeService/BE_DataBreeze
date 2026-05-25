package databreeze.service.insight.impl;

import databreeze.dto.insight.BusinessInsightResponse;
import databreeze.dto.insight.GenerateInsightRequest;
import databreeze.dto.insight.GenerateInsightResponse;
import databreeze.entity.BusinessInsight;
import databreeze.entity.Order;
import databreeze.entity.OrderItem;
import databreeze.enums.CommercePlatform;
import databreeze.enums.InsightGeneratedBy;
import databreeze.enums.InsightSeverity;
import databreeze.enums.InsightStatus;
import databreeze.enums.InsightType;
import databreeze.repository.BusinessInsightRepository;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.service.billing.UsageMeterService;
import databreeze.service.insight.BusinessInsightService;
import databreeze.service.workspace.WorkspaceAccessService;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessInsightServiceImpl implements BusinessInsightService {
    private static final BigDecimal LOW_MARGIN = BigDecimal.valueOf(0.10);
    private static final BigDecimal HIGH_REFUND_RATE = BigDecimal.valueOf(0.10);
    private static final BigDecimal SIGNIFICANT_DROP = BigDecimal.valueOf(-25);

    @Autowired
    private BusinessInsightRepository businessInsightRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private UsageMeterService usageMeterService;

    @Override
    @Transactional
    public GenerateInsightResponse generate(UUID workspaceId, UUID actorUserId, GenerateInsightRequest request) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        UUID storeId = request == null ? null : request.getStoreId();
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);

        LocalDate toDate = request == null || request.getToDate() == null ? LocalDate.now() : request.getToDate();
        LocalDate fromDate = request == null || request.getFromDate() == null ? toDate.minusDays(29) : request.getFromDate();

        List<Order> orders = loadOrders(workspaceId, storeId).stream()
                .filter(order -> order.getPlatform() == CommercePlatform.SHOPEE)
                .filter(order -> inDateRange(businessDate(order), fromDate, toDate))
                .toList();
        Set<UUID> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        Map<UUID, Order> orderById = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));
        List<OrderItem> items = orderItemRepository.findByWorkspaceId(workspaceId).stream()
                .filter(item -> orderIds.contains(item.getOrderId()))
                .toList();

        usageMeterService.recordInsightGeneration(
                workspaceId,
                actorUserId,
                0L,
                0L
        );

        List<BusinessInsight> generated = buildInsights(workspaceId, storeId, fromDate, toDate, orders, items, orderById);
        businessInsightRepository.deleteGeneratedForPeriod(workspaceId, storeId, fromDate, toDate, InsightGeneratedBy.SYSTEM_RULE);
        if (!generated.isEmpty()) {
            generated = businessInsightRepository.saveAll(generated);
        }

        return GenerateInsightResponse.builder()
                .generatedCount(generated.size())
                .message(generated.isEmpty()
                        ? "Chưa đủ dữ liệu để tạo insight. Hãy import đơn hàng Shopee trước."
                        : "Đã tạo insight từ dữ liệu Shopee đã xử lý.")
                .insights(generated.stream().map(this::toResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessInsightResponse> list(UUID workspaceId, UUID actorUserId, InsightStatus status, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);
        List<BusinessInsight> insights = status == null
                ? businessInsightRepository.findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId)
                : businessInsightRepository.findByWorkspaceIdAndStatusOrderByGeneratedAtDesc(workspaceId, status);
        return insights.stream()
                .filter(insight -> storeId == null || storeId.equals(insight.getStoreId()))
                .filter(insight -> fromDate == null || (insight.getPeriodEnd() != null && !insight.getPeriodEnd().isBefore(fromDate)))
                .filter(insight -> toDate == null || (insight.getPeriodStart() != null && !insight.getPeriodStart().isAfter(toDate)))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BusinessInsightResponse updateStatus(UUID workspaceId, UUID actorUserId, UUID insightId, InsightStatus status) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        BusinessInsight insight = businessInsightRepository.findById(insightId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy insight."));
        if (!workspaceId.equals(insight.getWorkspaceId())) {
            throw new SecurityException("Insight không thuộc workspace hiện tại.");
        }
        insight.setStatus(status);
        return toResponse(businessInsightRepository.save(insight));
    }

    private List<BusinessInsight> buildInsights(
            UUID workspaceId,
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<Order> orders,
            List<OrderItem> items,
            Map<UUID, Order> orderById
    ) {
        InsightBuilder builder = new InsightBuilder(workspaceId, storeId, fromDate, toDate);
        if (orders.isEmpty()) {
            builder.add(InsightType.DATA_QUALITY_WARNING, InsightSeverity.INFO,
                    "Chưa có dữ liệu Shopee trong kỳ",
                    "Không tìm thấy đơn hàng đã xử lý trong khoảng ngày được chọn.",
                    "Import file Shopee hoặc chọn lại khoảng ngày để hệ thống có dữ liệu phân tích.",
                    "order_count", BigDecimal.ZERO, null, null, null, null);
            return builder.insights();
        }

        BigDecimal netRevenue = sumOrders(orders, Order::getNetRevenueAmount);
        BigDecimal refund = sumOrders(orders, Order::getRefundAmount);
        BigDecimal platformFee = sumOrders(orders, Order::getPlatformFeeAmount);
        BigDecimal transactionFee = sumOrders(orders, Order::getTransactionFeeAmount);
        BigDecimal shippingFee = sumOrders(orders, Order::getShippingFeeAmount);
        BigDecimal cogs = sumItems(items, OrderItem::getCogsAmount);
        BigDecimal adSpend = sumItems(items, OrderItem::getAllocatedAdSpendAmount);
        BigDecimal netProfit = netRevenue.subtract(cogs).subtract(platformFee).subtract(transactionFee).subtract(shippingFee).subtract(adSpend);
        BigDecimal margin = ratio(netProfit, netRevenue);

        if (netProfit.signum() < 0) {
            builder.add(InsightType.PROFIT_DROP, InsightSeverity.CRITICAL,
                    "Lợi nhuận ròng đang âm",
                    "Kỳ này doanh thu không đủ bù giá vốn, phí sàn, phí vận chuyển và quảng cáo.",
                    "Ưu tiên kiểm tra SKU lỗ, phí vận chuyển và giá vốn trước khi tăng ngân sách bán hàng.",
                    "net_profit", netProfit, BigDecimal.ZERO, null, null, null);
        } else if (margin.compareTo(LOW_MARGIN) < 0) {
            builder.add(InsightType.PROFIT_DROP, InsightSeverity.WARNING,
                    "Biên lợi nhuận thấp",
                    "Biên lợi nhuận ròng thấp hơn 10%, dễ bị lỗ khi có hoàn hàng hoặc tăng phí.",
                    "Nên rà lại giá bán, voucher và các SKU có giá vốn cao.",
                    "profit_margin", margin, LOW_MARGIN, null, null, null);
        }

        BigDecimal refundRate = ratio(refund, netRevenue);
        if (refundRate.compareTo(HIGH_REFUND_RATE) > 0) {
            builder.add(InsightType.HIGH_REFUND_RATE, InsightSeverity.WARNING,
                    "Tỷ lệ hoàn tiền cao",
                    "Hoàn tiền chiếm hơn 10% doanh thu thuần trong kỳ.",
                    "Kiểm tra các đơn bị hủy/hoàn và nhóm sản phẩm có tỷ lệ refund bất thường.",
                    "refund_rate", refundRate, HIGH_REFUND_RATE, null, null, null);
        }

        addRevenueTrendInsight(builder, orders, fromDate, toDate);
        addSkuInsights(builder, items);
        addMissingCostInsight(builder, items, orderById);
        return builder.insights();
    }

    private void addRevenueTrendInsight(InsightBuilder builder, List<Order> orders, LocalDate fromDate, LocalDate toDate) {
        long days = Math.max(1, fromDate.datesUntil(toDate.plusDays(1)).count());
        LocalDate splitDate = toDate.minusDays(Math.max(1, days / 2) - 1);
        BigDecimal recent = orders.stream()
                .filter(order -> !businessDate(order).isBefore(splitDate))
                .map(Order::getNetRevenueAmount)
                .map(this::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previous = orders.stream()
                .filter(order -> businessDate(order).isBefore(splitDate))
                .map(Order::getNetRevenueAmount)
                .map(this::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal changePercent = percentChange(recent, previous);
        if (previous.signum() > 0 && changePercent.compareTo(SIGNIFICANT_DROP) <= 0) {
            builder.add(InsightType.REVENUE_DROP, InsightSeverity.WARNING,
                    "Doanh thu đang giảm mạnh",
                    "Nửa sau của kỳ giảm hơn 25% so với nửa trước.",
                    "Kiểm tra sản phẩm chủ lực, tồn kho và chiến dịch khuyến mãi trong các ngày gần nhất.",
                    "net_revenue", recent, previous, changePercent, null, null);
        }
    }

    private void addSkuInsights(InsightBuilder builder, List<OrderItem> items) {
        Map<String, ProductAgg> bySku = new LinkedHashMap<>();
        for (OrderItem item : items) {
            String key = item.getSku() == null || item.getSku().isBlank() ? "NO_SKU:" + item.getId() : item.getSku();
            ProductAgg agg = bySku.computeIfAbsent(key, ignored -> new ProductAgg(item));
            agg.quantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.netRevenue = agg.netRevenue.add(money(item.getNetRevenueAmount()));
            agg.netProfit = agg.netProfit.add(money(item.getNetProfitAmount()));
        }

        bySku.values().stream()
                .filter(agg -> agg.netRevenue.signum() > 0 && agg.netProfit.signum() < 0)
                .max(Comparator.comparing(agg -> agg.netRevenue))
                .ifPresent(agg -> builder.add(InsightType.NEGATIVE_PROFIT_SKU, InsightSeverity.CRITICAL,
                        "SKU có doanh thu nhưng đang lỗ",
                        agg.productName + " bán được " + agg.quantity + " sản phẩm nhưng lợi nhuận ròng âm.",
                        "Nên kiểm tra giá vốn, voucher, phí sàn hoặc tạm dừng đẩy SKU này.",
                        "sku_net_profit", agg.netProfit, BigDecimal.ZERO, null, "SKU", null));
    }

    private void addMissingCostInsight(InsightBuilder builder, List<OrderItem> items, Map<UUID, Order> orderById) {
        long missingCostRows = items.stream()
                .filter(item -> money(item.getNetRevenueAmount()).signum() > 0)
                .filter(item -> item.getCogsAmount() == null || item.getCogsAmount().signum() == 0)
                .count();
        if (missingCostRows > 0) {
            builder.add(InsightType.MISSING_PRODUCT_COST, InsightSeverity.WARNING,
                    "Một số dòng bán hàng thiếu giá vốn",
                    missingCostRows + " dòng order item chưa có COGS nên lợi nhuận có thể đang bị sai.",
                    "Bổ sung bảng giá vốn theo SKU rồi chạy lại import/tính profit để insight chính xác hơn.",
                    "missing_cogs_rows", BigDecimal.valueOf(missingCostRows), BigDecimal.valueOf(items.size()), null, null, null);
        }
    }

    private List<Order> loadOrders(UUID workspaceId, UUID storeId) {
        return storeId == null
                ? orderRepository.findByWorkspaceId(workspaceId)
                : orderRepository.findByWorkspaceIdAndStoreId(workspaceId, storeId);
    }

    private BusinessInsightResponse toResponse(BusinessInsight insight) {
        return BusinessInsightResponse.builder()
                .id(insight.getId())
                .workspaceId(insight.getWorkspaceId())
                .storeId(insight.getStoreId())
                .insightType(insight.getInsightType())
                .severity(insight.getSeverity())
                .status(insight.getStatus())
                .title(displayTitle(insight))
                .summary(displaySummary(insight))
                .explanation(displayExplanation(insight))
                .metricName(insight.getMetricName())
                .metricValue(insight.getMetricValue())
                .comparisonValue(insight.getComparisonValue())
                .changePercent(insight.getChangePercent())
                .relatedEntityType(insight.getRelatedEntityType())
                .relatedEntityId(insight.getRelatedEntityId())
                .periodStart(insight.getPeriodStart())
                .periodEnd(insight.getPeriodEnd())
                .generatedAt(insight.getGeneratedAt())
                .build();
    }

    private String displayTitle(BusinessInsight insight) {
        if (insight.getInsightType() == InsightType.DATA_QUALITY_WARNING) {
            return "Chưa có dữ liệu Shopee trong kỳ";
        }
        if (insight.getInsightType() == InsightType.PROFIT_DROP && "net_profit".equals(insight.getMetricName())) {
            return "Lợi nhuận ròng đang âm";
        }
        if (insight.getInsightType() == InsightType.PROFIT_DROP && "profit_margin".equals(insight.getMetricName())) {
            return "Biên lợi nhuận thấp";
        }
        if (insight.getInsightType() == InsightType.HIGH_REFUND_RATE) {
            return "Tỷ lệ hoàn tiền cao";
        }
        if (insight.getInsightType() == InsightType.REVENUE_DROP) {
            return "Doanh thu đang giảm mạnh";
        }
        if (insight.getInsightType() == InsightType.NEGATIVE_PROFIT_SKU) {
            return "SKU có doanh thu nhưng đang lỗ";
        }
        if (insight.getInsightType() == InsightType.MISSING_PRODUCT_COST) {
            return "Một số dòng bán hàng thiếu giá vốn";
        }
        return readableText(insight.getTitle());
    }

    private String displaySummary(BusinessInsight insight) {
        if (insight.getInsightType() == InsightType.DATA_QUALITY_WARNING) {
            return "Không tìm thấy đơn hàng đã xử lý trong khoảng ngày được chọn.";
        }
        if (insight.getInsightType() == InsightType.PROFIT_DROP && "net_profit".equals(insight.getMetricName())) {
            return "Kỳ này doanh thu không đủ bù giá vốn, phí sàn, phí vận chuyển và quảng cáo.";
        }
        if (insight.getInsightType() == InsightType.PROFIT_DROP && "profit_margin".equals(insight.getMetricName())) {
            return "Biên lợi nhuận ròng thấp hơn 10%, dễ bị lỗ khi có hoàn hàng hoặc tăng phí.";
        }
        if (insight.getInsightType() == InsightType.HIGH_REFUND_RATE) {
            return "Hoàn tiền chiếm hơn 10% doanh thu thuần trong kỳ.";
        }
        if (insight.getInsightType() == InsightType.REVENUE_DROP) {
            return "Nửa sau của kỳ giảm hơn 25% so với nửa trước.";
        }
        if (insight.getInsightType() == InsightType.NEGATIVE_PROFIT_SKU) {
            return "Một SKU có doanh thu nhưng lợi nhuận ròng đang âm.";
        }
        if (insight.getInsightType() == InsightType.MISSING_PRODUCT_COST) {
            return metricAsLong(insight.getMetricValue()) + " dòng order item chưa có COGS nên lợi nhuận có thể đang sai.";
        }
        return readableText(insight.getSummary());
    }

    private String displayExplanation(BusinessInsight insight) {
        if (insight.getInsightType() == InsightType.DATA_QUALITY_WARNING) {
            return "Import file Shopee hoặc chọn lại khoảng ngày để hệ thống có dữ liệu phân tích.";
        }
        if (insight.getInsightType() == InsightType.PROFIT_DROP && "net_profit".equals(insight.getMetricName())) {
            return "Ưu tiên kiểm tra SKU lỗ, phí vận chuyển và giá vốn trước khi tăng ngân sách bán hàng.";
        }
        if (insight.getInsightType() == InsightType.PROFIT_DROP && "profit_margin".equals(insight.getMetricName())) {
            return "Nên rà lại giá bán, voucher và các SKU có giá vốn cao.";
        }
        if (insight.getInsightType() == InsightType.HIGH_REFUND_RATE) {
            return "Kiểm tra các đơn bị hủy/hoàn và nhóm sản phẩm có tỷ lệ refund bất thường.";
        }
        if (insight.getInsightType() == InsightType.REVENUE_DROP) {
            return "Kiểm tra sản phẩm chủ lực, tồn kho và chiến dịch khuyến mãi trong các ngày gần nhất.";
        }
        if (insight.getInsightType() == InsightType.NEGATIVE_PROFIT_SKU) {
            return "Nên kiểm tra giá vốn, voucher, phí sàn hoặc tạm dừng đẩy SKU này.";
        }
        if (insight.getInsightType() == InsightType.MISSING_PRODUCT_COST) {
            return "Bổ sung bảng giá vốn theo SKU rồi chạy lại import/tính profit để insight chính xác hơn.";
        }
        return readableText(insight.getExplanation());
    }

    private long metricAsLong(BigDecimal value) {
        return value == null ? 0L : value.longValue();
    }

    private static String readableText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!(value.contains("Ã") || value.contains("Ä") || value.contains("áº") || value.contains("á»"))) {
            return value;
        }
        try {
            return new String(value.getBytes(Charset.forName("windows-1252")), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }

    private LocalDate businessDate(Order order) {
        if (order.getPaidAt() != null) {
            return order.getPaidAt().toLocalDate();
        }
        if (order.getOrderDate() != null) {
            return order.getOrderDate().toLocalDate();
        }
        return LocalDate.MIN;
    }

    private boolean inDateRange(LocalDate date, LocalDate fromDate, LocalDate toDate) {
        return date != null && !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    private BigDecimal sumOrders(List<Order> orders, Function<Order, BigDecimal> getter) {
        return orders.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(List<OrderItem> items, Function<OrderItem, BigDecimal> getter) {
        return items.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private static class ProductAgg {
        private final String productName;
        private long quantity;
        private BigDecimal netRevenue = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;

        private ProductAgg(OrderItem item) {
            this.productName = item.getProductName() == null ? item.getSku() : item.getProductName();
        }
    }

    private static class InsightBuilder {
        private final UUID workspaceId;
        private final UUID storeId;
        private final LocalDate fromDate;
        private final LocalDate toDate;
        private final List<BusinessInsight> insights = new java.util.ArrayList<>();

        private InsightBuilder(UUID workspaceId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
            this.workspaceId = workspaceId;
            this.storeId = storeId;
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        private void add(
                InsightType type,
                InsightSeverity severity,
                String title,
                String summary,
                String explanation,
                String metricName,
                BigDecimal metricValue,
                BigDecimal comparisonValue,
                BigDecimal changePercent,
                String relatedEntityType,
                UUID relatedEntityId
        ) {
            insights.add(BusinessInsight.builder()
                    .workspaceId(workspaceId)
                    .storeId(storeId)
                    .insightType(type)
                    .severity(severity)
                    .status(InsightStatus.OPEN)
                    .title(title)
                    .summary(summary)
                    .explanation(explanation)
                    .metricName(metricName)
                    .metricValue(metricValue)
                    .comparisonValue(comparisonValue)
                    .changePercent(changePercent)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .periodStart(fromDate)
                    .periodEnd(toDate)
                    .generatedBy(InsightGeneratedBy.SYSTEM_RULE)
                    .generatedAt(OffsetDateTime.now())
                    .build());
        }

        private List<BusinessInsight> insights() {
            return insights;
        }
    }
}
