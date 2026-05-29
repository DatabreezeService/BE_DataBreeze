package databreeze.service.dashboard.impl;

import databreeze.dto.dashboard.DashboardDailyResponse;
import databreeze.dto.dashboard.DashboardDataQualityResponse;
import databreeze.dto.dashboard.DashboardOverviewResponse;
import databreeze.dto.dashboard.DashboardProductResponse;
import databreeze.dto.dashboard.DashboardSummaryResponse;
import databreeze.dto.shopee.ShopeeDailyCalculationResult;
import databreeze.entity.OperatingExpense;
import databreeze.entity.Order;
import databreeze.entity.OrderItem;
import databreeze.enums.CommercePlatform;
import databreeze.enums.SourcePlatform;
import databreeze.enums.WorkspacePermission;
import databreeze.repository.OperatingExpenseRepository;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.service.analytics.ShopeeAnalyticsService;
import databreeze.service.dashboard.DashboardQueryService;
import databreeze.service.workspace.WorkspaceAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardQueryServiceImpl implements DashboardQueryService {
    private static final int DEFAULT_TOP_PRODUCT_LIMIT = 10;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OperatingExpenseRepository operatingExpenseRepository;

    @Autowired
    private ShopeeAnalyticsService shopeeAnalyticsService;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getShopeeDashboard(
            UUID workspaceId,
            UUID actorUserId,
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate,
            int topProductLimit
    ) {
        DashboardData data = loadDashboardData(workspaceId, actorUserId, storeId, fromDate, toDate);
        int limit = topProductLimit <= 0 ? DEFAULT_TOP_PRODUCT_LIMIT : topProductLimit;
        return DashboardOverviewResponse.builder()
                .workspaceId(workspaceId)
                .storeId(storeId)
                .sourcePlatform(SourcePlatform.SHOPEE)
                .fromDate(fromDate)
                .toDate(toDate)
                .summary(toSummary(workspaceId, storeId, fromDate, toDate, data.selectedOrders, data.selectedItems, data.selectedExpenses))
                .daily(toDaily(data.selectedOrders, data.selectedItems, data.selectedExpenses))
                .topProducts(toTopProducts(data.selectedItems, limit))
                .dataQuality(toDataQuality(data))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getShopeeSummary(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        DashboardData data = loadDashboardData(workspaceId, actorUserId, storeId, fromDate, toDate);
        return toSummary(workspaceId, storeId, fromDate, toDate, data.selectedOrders, data.selectedItems, data.selectedExpenses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardDailyResponse> getShopeeDaily(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        DashboardData data = loadDashboardData(workspaceId, actorUserId, storeId, fromDate, toDate);
        return toDaily(data.selectedOrders, data.selectedItems, data.selectedExpenses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardProductResponse> getShopeeTopProducts(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate, int limit) {
        DashboardData data = loadDashboardData(workspaceId, actorUserId, storeId, fromDate, toDate);
        return toTopProducts(data.selectedItems, limit <= 0 ? DEFAULT_TOP_PRODUCT_LIMIT : limit);
    }

    @Override
    @Transactional
    public ShopeeDailyCalculationResult recalculateShopee(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);
        DateRange range = inferDateRange(workspaceId, storeId, fromDate, toDate);
        return shopeeAnalyticsService.recalculateDaily(workspaceId, storeId, range.fromDate(), range.toDate());
    }

    private DashboardData loadDashboardData(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);

        List<Order> allOrders = loadOrders(workspaceId, storeId).stream()
                .filter(order -> order.getPlatform() == CommercePlatform.SHOPEE)
                .toList();
        List<Order> selectedOrders = allOrders.stream()
                .filter(order -> inDateRange(businessDate(order), fromDate, toDate))
                .toList();
        Set<UUID> allOrderIds = allOrders.stream().map(Order::getId).collect(Collectors.toSet());
        Set<UUID> selectedOrderIds = selectedOrders.stream().map(Order::getId).collect(Collectors.toSet());
        List<OrderItem> allItems = orderItemRepository.findByWorkspaceId(workspaceId).stream()
                .filter(item -> allOrderIds.contains(item.getOrderId()))
                .toList();
        List<OrderItem> selectedItems = allItems.stream()
                .filter(item -> selectedOrderIds.contains(item.getOrderId()))
                .toList();
        LocalDate expenseFrom = fromDate == null ? LocalDate.of(1900, 1, 1) : fromDate;
        LocalDate expenseTo = toDate == null ? LocalDate.of(2999, 12, 31) : toDate;
        List<OperatingExpense> selectedExpenses = operatingExpenseRepository
                .findByWorkspaceIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(workspaceId, expenseFrom, expenseTo)
                .stream()
                .filter(expense -> storeId == null || expense.getStoreId() == null || storeId.equals(expense.getStoreId()))
                .toList();
        LocalDate firstDate = allOrders.stream()
                .map(this::businessDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate lastDate = allOrders.stream()
                .map(this::businessDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(null);

        return new DashboardData(workspaceId, storeId, fromDate, toDate, allOrders, selectedOrders, allItems, selectedItems, selectedExpenses, firstDate, lastDate);
    }

    private List<Order> loadOrders(UUID workspaceId, UUID storeId) {
        if (storeId == null) {
            return orderRepository.findByWorkspaceId(workspaceId);
        }
        return orderRepository.findByWorkspaceIdAndStoreId(workspaceId, storeId);
    }

    private DashboardSummaryResponse toSummary(
            UUID workspaceId,
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<Order> orders,
            List<OrderItem> items,
            List<OperatingExpense> expenses
    ) {
        long quantity = items.stream().mapToLong(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
        BigDecimal gross = sumOrders(orders, Order::getGrossRevenueAmount);
        BigDecimal discount = sumOrders(orders, Order::getDiscountAmount);
        BigDecimal refund = sumOrders(orders, Order::getRefundAmount);
        BigDecimal net = sumOrders(orders, Order::getNetRevenueAmount);
        BigDecimal platformFee = sumOrders(orders, Order::getPlatformFeeAmount);
        BigDecimal transactionFee = sumOrders(orders, Order::getTransactionFeeAmount);
        BigDecimal shippingFee = sumOrders(orders, Order::getShippingFeeAmount);
        BigDecimal cogs = sumItems(items, OrderItem::getCogsAmount);
        BigDecimal adSpend = sumItems(items, OrderItem::getAllocatedAdSpendAmount);
        BigDecimal operatingExpense = sumExpenses(expenses);
        BigDecimal netProfit = net.subtract(cogs).subtract(platformFee).subtract(transactionFee).subtract(shippingFee).subtract(adSpend).subtract(operatingExpense);

        return DashboardSummaryResponse.builder()
                .workspaceId(workspaceId)
                .storeId(storeId)
                .sourcePlatform(SourcePlatform.SHOPEE)
                .fromDate(fromDate)
                .toDate(toDate)
                .orderCount(orders.size())
                .itemQuantity(quantity)
                .grossRevenueAmount(gross)
                .discountAmount(discount)
                .refundAmount(refund)
                .netRevenueAmount(net)
                .cogsAmount(cogs)
                .platformFeeAmount(platformFee)
                .transactionFeeAmount(transactionFee)
                .shippingFeeAmount(shippingFee)
                .adSpendAmount(adSpend)
                .operatingExpenseAmount(operatingExpense)
                .netProfitAmount(netProfit)
                .profitMargin(ratio(netProfit, net))
                .averageOrderValue(orders.isEmpty() ? BigDecimal.ZERO : net.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP))
                .build();
    }

    private List<DashboardDailyResponse> toDaily(List<Order> orders, List<OrderItem> items, List<OperatingExpense> expenses) {
        Map<UUID, Order> ordersById = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));
        Map<LocalDate, DailyAgg> daily = new LinkedHashMap<>();

        for (Order order : orders) {
            LocalDate date = businessDate(order);
            if (date == null) {
                continue;
            }
            DailyAgg agg = daily.computeIfAbsent(date, ignored -> new DailyAgg());
            agg.orderIds.add(order.getId());
            agg.gross = agg.gross.add(money(order.getGrossRevenueAmount()));
            agg.net = agg.net.add(money(order.getNetRevenueAmount()));
            agg.refund = agg.refund.add(money(order.getRefundAmount()));
            agg.netProfit = agg.netProfit
                    .add(money(order.getNetRevenueAmount()))
                    .subtract(money(order.getPlatformFeeAmount()))
                    .subtract(money(order.getTransactionFeeAmount()))
                    .subtract(money(order.getShippingFeeAmount()));
        }

        for (OrderItem item : items) {
            Order order = ordersById.get(item.getOrderId());
            LocalDate date = order == null ? null : businessDate(order);
            if (date == null) {
                continue;
            }
            DailyAgg agg = daily.computeIfAbsent(date, ignored -> new DailyAgg());
            agg.itemQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.netProfit = agg.netProfit.subtract(money(item.getCogsAmount())).subtract(money(item.getAllocatedAdSpendAmount()));
        }

        for (OperatingExpense expense : expenses) {
            DailyAgg agg = daily.computeIfAbsent(expense.getExpenseDate(), ignored -> new DailyAgg());
            BigDecimal amount = money(expense.getAmount());
            agg.operatingExpense = agg.operatingExpense.add(amount);
            agg.netProfit = agg.netProfit.subtract(amount);
        }

        return daily.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, DailyAgg>comparingByKey().reversed())
                .map(entry -> DashboardDailyResponse.builder()
                        .date(entry.getKey())
                        .orderCount(entry.getValue().orderIds.size())
                        .itemQuantity(entry.getValue().itemQuantity)
                        .grossRevenueAmount(entry.getValue().gross)
                        .netRevenueAmount(entry.getValue().net)
                        .refundAmount(entry.getValue().refund)
                        .operatingExpenseAmount(entry.getValue().operatingExpense)
                        .netProfitAmount(entry.getValue().netProfit)
                        .profitMargin(ratio(entry.getValue().netProfit, entry.getValue().net))
                        .build())
                .toList();
    }

    private List<DashboardProductResponse> toTopProducts(List<OrderItem> items, int limit) {
        Map<String, ProductAgg> byProduct = new HashMap<>();
        for (OrderItem item : items) {
            String key = item.getProductId() == null ? item.getSku() : item.getProductId().toString();
            ProductAgg agg = byProduct.computeIfAbsent(key == null ? item.getId().toString() : key, ignored -> new ProductAgg(item));
            agg.orderItemCount++;
            agg.itemQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.gross = agg.gross.add(money(item.getGrossRevenueAmount()));
            agg.net = agg.net.add(money(item.getNetRevenueAmount()));
            agg.cogs = agg.cogs.add(money(item.getCogsAmount()));
            agg.netProfit = agg.netProfit.add(money(item.getNetProfitAmount()));
        }

        return byProduct.values().stream()
                .sorted(Comparator.comparing((ProductAgg agg) -> agg.net).reversed())
                .limit(limit)
                .map(ProductAgg::toResponse)
                .toList();
    }

    private DashboardDataQualityResponse toDataQuality(DashboardData data) {
        long missingDate = data.allOrders.stream().filter(order -> businessDate(order) == null).count();
        long missingCogs = data.selectedItems.stream()
                .filter(item -> money(item.getNetRevenueAmount()).signum() > 0)
                .filter(item -> item.getCogsAmount() == null || item.getCogsAmount().signum() == 0)
                .count();
        boolean hasData = !data.selectedOrders.isEmpty();

        return DashboardDataQualityResponse.builder()
                .totalProcessedOrders(data.allOrders.size())
                .ordersInSelectedRange(data.selectedOrders.size())
                .ordersMissingBusinessDate(missingDate)
                .orderItemsMissingCogs(missingCogs)
                .firstBusinessDate(data.firstBusinessDate)
                .lastBusinessDate(data.lastBusinessDate)
                .selectedRangeHasData(hasData)
                .message(dataQualityMessage(data, missingDate, missingCogs, hasData))
                .build();
    }

    private String dataQualityMessage(DashboardData data, long missingDate, long missingCogs, boolean hasData) {
        if (data.allOrders.isEmpty()) {
            return "Chua co don Shopee da import cho workspace/store nay.";
        }
        if (!hasData) {
            return "Khong co don trong khoang ngay dang chon. Range co du lieu: " + data.firstBusinessDate + " -> " + data.lastBusinessDate + ".";
        }
        if (missingDate > 0) {
            return "Co " + missingDate + " don thieu ngay kinh doanh, daily chart va insight theo ngay co the bi thieu.";
        }
        if (missingCogs > 0) {
            return "Co " + missingCogs + " dong thieu COGS, doanh thu dung nhung loi nhuan chi la tam tinh.";
        }
        return "Du lieu dashboard san sang.";
    }

    private LocalDate businessDate(Order order) {
        if (order.getPaidAt() != null) {
            return order.getPaidAt().toLocalDate();
        }
        if (order.getOrderDate() != null) {
            return order.getOrderDate().toLocalDate();
        }
        return null;
    }

    private boolean inDateRange(LocalDate date, LocalDate fromDate, LocalDate toDate) {
        if (date == null) {
            return fromDate == null && toDate == null;
        }
        if (fromDate != null && date.isBefore(fromDate)) {
            return false;
        }
        return toDate == null || !date.isAfter(toDate);
    }

    private BigDecimal sumOrders(List<Order> orders, Function<Order, BigDecimal> getter) {
        return orders.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(List<OrderItem> items, Function<OrderItem, BigDecimal> getter) {
        return items.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumExpenses(List<OperatingExpense> expenses) {
        return expenses.stream().map(OperatingExpense::getAmount).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DateRange inferDateRange(UUID workspaceId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return new DateRange(fromDate, toDate);
        }
        List<Order> orders = loadOrders(workspaceId, storeId).stream()
                .filter(order -> order.getPlatform() == CommercePlatform.SHOPEE)
                .toList();
        LocalDate min = fromDate != null ? fromDate : orders.stream().map(this::businessDate).filter(date -> date != null).min(LocalDate::compareTo).orElse(null);
        LocalDate max = toDate != null ? toDate : orders.stream().map(this::businessDate).filter(date -> date != null).max(LocalDate::compareTo).orElse(null);
        return new DateRange(min, max);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record DashboardData(
            UUID workspaceId,
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<Order> allOrders,
            List<Order> selectedOrders,
            List<OrderItem> allItems,
            List<OrderItem> selectedItems,
            List<OperatingExpense> selectedExpenses,
            LocalDate firstBusinessDate,
            LocalDate lastBusinessDate
    ) {
    }

    private static class DailyAgg {
        private final Set<UUID> orderIds = new HashSet<>();
        private long itemQuantity;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal refund = BigDecimal.ZERO;
        private BigDecimal operatingExpense = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private class ProductAgg {
        private final UUID productId;
        private final String sku;
        private final String productName;
        private long orderItemCount;
        private long itemQuantity;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal cogs = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;

        private ProductAgg(OrderItem item) {
            this.productId = item.getProductId();
            this.sku = item.getSku();
            this.productName = item.getProductName();
        }

        private DashboardProductResponse toResponse() {
            return DashboardProductResponse.builder()
                    .productId(productId)
                    .sku(sku)
                    .productName(productName)
                    .orderItemCount(orderItemCount)
                    .itemQuantity(itemQuantity)
                    .grossRevenueAmount(gross)
                    .netRevenueAmount(net)
                    .cogsAmount(cogs)
                    .netProfitAmount(netProfit)
                    .profitMargin(ratio(netProfit, net))
                    .build();
        }
    }
}
