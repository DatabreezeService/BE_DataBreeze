package databreeze.service.analytics.impl;

import databreeze.dto.shopee.ShopeeDailyCalculationResult;
import databreeze.entity.OperatingExpense;
import databreeze.entity.Order;
import databreeze.entity.OrderItem;
import databreeze.entity.ProfitDaily;
import databreeze.entity.RevenueDaily;
import databreeze.enums.SourcePlatform;
import databreeze.repository.OperatingExpenseRepository;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.repository.ProfitDailyRepository;
import databreeze.repository.RevenueDailyRepository;
import databreeze.service.analytics.ShopeeAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tính lại bảng tổng hợp Shopee sau import. Dùng Java aggregation để chạy được cả MySQL local và PostgreSQL deploy.
 */
@Service
public class ShopeeAnalyticsServiceImpl implements ShopeeAnalyticsService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RevenueDailyRepository revenueDailyRepository;

    @Autowired
    private ProfitDailyRepository profitDailyRepository;

    @Autowired
    private OperatingExpenseRepository operatingExpenseRepository;

    @Override
    @Transactional
    public ShopeeDailyCalculationResult recalculateDaily(UUID workspaceId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return new ShopeeDailyCalculationResult(workspaceId, storeId, fromDate, toDate, 0, 0, "Không có ngày phát sinh để tính dashboard.");
        }

        revenueDailyRepository.deleteRange(workspaceId, SourcePlatform.SHOPEE, storeId, fromDate, toDate);
        profitDailyRepository.deleteRange(workspaceId, SourcePlatform.SHOPEE, storeId, fromDate, toDate);

        Map<LocalDate, DailyAgg> daily = new TreeMap<>();
        List<Order> workspaceOrders = orderRepository.findAll().stream()
                .filter(order -> workspaceId.equals(order.getWorkspaceId()))
                .filter(order -> storeId == null || storeId.equals(order.getStoreId()))
                .toList();

        for (Order order : workspaceOrders) {
            LocalDate date = businessDate(order);
            if (date == null || date.isBefore(fromDate) || date.isAfter(toDate)) continue;
            DailyAgg agg = daily.computeIfAbsent(date, d -> new DailyAgg());
            agg.orderCount++;
            agg.gross = agg.gross.add(money(order.getGrossRevenueAmount()));
            agg.net = agg.net.add(money(order.getNetRevenueAmount()));
            agg.discount = agg.discount.add(money(order.getDiscountAmount()));
            agg.refund = agg.refund.add(money(order.getRefundAmount()));
            agg.platformFee = agg.platformFee.add(money(order.getPlatformFeeAmount()));
            agg.transactionFee = agg.transactionFee.add(money(order.getTransactionFeeAmount()));
            agg.shippingFee = agg.shippingFee.add(money(order.getShippingFeeAmount()));
        }

        Map<UUID, Order> ordersById = workspaceOrders.stream().collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));
        for (OrderItem item : orderItemRepository.findByWorkspaceId(workspaceId)) {
            Order order = ordersById.get(item.getOrderId());
            if (order == null) continue;
            LocalDate date = businessDate(order);
            if (date == null || date.isBefore(fromDate) || date.isAfter(toDate)) continue;
            DailyAgg agg = daily.computeIfAbsent(date, d -> new DailyAgg());
            agg.itemQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.cogs = agg.cogs.add(money(item.getCogsAmount()));
            agg.adSpend = agg.adSpend.add(money(item.getAllocatedAdSpendAmount()));
        }

        for (OperatingExpense expense : operatingExpenseRepository.findByWorkspaceIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(workspaceId, fromDate, toDate)) {
            if (storeId != null && expense.getStoreId() != null && !storeId.equals(expense.getStoreId())) {
                continue;
            }
            DailyAgg agg = daily.computeIfAbsent(expense.getExpenseDate(), d -> new DailyAgg());
            agg.operatingExpense = agg.operatingExpense.add(money(expense.getAmount()));
        }

        OffsetDateTime now = OffsetDateTime.now();
        int revenueRows = 0;
        int profitRows = 0;
        for (Map.Entry<LocalDate, DailyAgg> entry : daily.entrySet()) {
            DailyAgg agg = entry.getValue();
            revenueDailyRepository.save(RevenueDaily.builder()
                    .workspaceId(workspaceId)
                    .storeId(storeId)
                    .platform(SourcePlatform.SHOPEE)
                    .revenueDate(entry.getKey())
                    .grossRevenueAmount(agg.gross)
                    .discountAmount(agg.discount)
                    .refundAmount(agg.refund)
                    .netRevenueAmount(agg.net)
                    .orderCount(agg.orderCount)
                    .itemQuantity(agg.itemQuantity)
                    .calculatedAt(now)
                    .build());
            revenueRows++;

            BigDecimal grossProfit = agg.net.subtract(agg.cogs);
            BigDecimal netProfit = grossProfit.subtract(agg.platformFee).subtract(agg.transactionFee).subtract(agg.shippingFee).subtract(agg.adSpend).subtract(agg.operatingExpense);
            BigDecimal margin = agg.net.signum() == 0 ? BigDecimal.ZERO : netProfit.divide(agg.net, 4, RoundingMode.HALF_UP);
            profitDailyRepository.save(ProfitDaily.builder()
                    .workspaceId(workspaceId)
                    .storeId(storeId)
                    .platform(SourcePlatform.SHOPEE)
                    .profitDate(entry.getKey())
                    .grossRevenueAmount(agg.gross)
                    .netRevenueAmount(agg.net)
                    .cogsAmount(agg.cogs)
                    .platformFeeAmount(agg.platformFee)
                    .transactionFeeAmount(agg.transactionFee)
                    .shippingFeeAmount(agg.shippingFee)
                    .adSpendAmount(agg.adSpend)
                    .operatingExpenseAmount(agg.operatingExpense)
                    .refundAmount(agg.refund)
                    .grossProfitAmount(grossProfit)
                    .netProfitAmount(netProfit)
                    .profitMargin(margin)
                    .orderCount(agg.orderCount)
                    .itemQuantity(agg.itemQuantity)
                    .calculatedAt(now)
                    .build());
            profitRows++;
        }

        return new ShopeeDailyCalculationResult(workspaceId, storeId, fromDate, toDate, revenueRows, profitRows, "Đã tính lại doanh thu/lợi nhuận Shopee theo ngày.");
    }

    private LocalDate businessDate(Order order) {
        if (order.getPaidAt() != null) return order.getPaidAt().toLocalDate();
        if (order.getOrderDate() != null) return order.getOrderDate().toLocalDate();
        return null;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class DailyAgg {
        long orderCount;
        long itemQuantity;
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal platformFee = BigDecimal.ZERO;
        BigDecimal transactionFee = BigDecimal.ZERO;
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;
        BigDecimal adSpend = BigDecimal.ZERO;
        BigDecimal operatingExpense = BigDecimal.ZERO;
    }
}
