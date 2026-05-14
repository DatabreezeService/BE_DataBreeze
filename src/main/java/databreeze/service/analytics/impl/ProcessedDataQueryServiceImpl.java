package databreeze.service.analytics.impl;

import databreeze.dto.analytics.ProcessedCommerceDataResponse;
import databreeze.entity.Order;
import databreeze.entity.OrderItem;
import databreeze.entity.Product;
import databreeze.enums.CommercePlatform;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.repository.ProductRepository;
import databreeze.service.analytics.ProcessedDataQueryService;
import databreeze.service.workspace.WorkspaceAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProcessedDataQueryServiceImpl implements ProcessedDataQueryService {
    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public ProcessedCommerceDataResponse getShopeeProcessedData(
            UUID workspaceId,
            UUID actorUserId,
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);

        List<Order> orders = loadOrders(workspaceId, storeId).stream()
                .filter(order -> order.getPlatform() == CommercePlatform.SHOPEE)
                .filter(order -> inDateRange(businessDate(order), fromDate, toDate))
                .sorted(Comparator.comparing(this::businessDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Set<UUID> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        Map<UUID, Order> ordersById = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));

        List<OrderItem> orderItems = orderItemRepository.findByWorkspaceId(workspaceId).stream()
                .filter(item -> orderIds.contains(item.getOrderId()))
                .toList();
        Set<UUID> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<Product> products = productRepository.findByWorkspaceId(workspaceId).stream()
                .filter(product -> productIds.contains(product.getId()))
                .sorted(Comparator.comparing(Product::getSku, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        Map<UUID, Product> productsById = products.stream().collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));

        return ProcessedCommerceDataResponse.builder()
                .workspaceId(workspaceId)
                .storeId(storeId)
                .fromDate(fromDate)
                .toDate(toDate)
                .summary(toSummary(orders, orderItems, productIds.size()))
                .orders(orders.stream().map(this::toOrderDto).toList())
                .orderItems(orderItems.stream().map(this::toOrderItemDto).toList())
                .products(products.stream().map(this::toProductDto).toList())
                .productStatistics(toProductStatistics(orderItems, productsById))
                .dailyStatistics(toDailyStatistics(orders, orderItems, ordersById))
                .build();
    }

    private List<Order> loadOrders(UUID workspaceId, UUID storeId) {
        if (storeId == null) {
            return orderRepository.findByWorkspaceId(workspaceId);
        }
        return orderRepository.findByWorkspaceIdAndStoreId(workspaceId, storeId);
    }

    private ProcessedCommerceDataResponse.SummaryDto toSummary(List<Order> orders, List<OrderItem> items, long productCount) {
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
        BigDecimal grossProfit = net.subtract(cogs);
        BigDecimal netProfit = grossProfit.subtract(platformFee).subtract(transactionFee).subtract(shippingFee).subtract(adSpend);

        return ProcessedCommerceDataResponse.SummaryDto.builder()
                .orderCount(orders.size())
                .orderItemCount(items.size())
                .productCount(productCount)
                .totalQuantity(quantity)
                .grossRevenueAmount(gross)
                .discountAmount(discount)
                .refundAmount(refund)
                .netRevenueAmount(net)
                .platformFeeAmount(platformFee)
                .transactionFeeAmount(transactionFee)
                .shippingFeeAmount(shippingFee)
                .cogsAmount(cogs)
                .adSpendAmount(adSpend)
                .grossProfitAmount(grossProfit)
                .netProfitAmount(netProfit)
                .profitMargin(ratio(netProfit, net))
                .averageOrderValue(orders.isEmpty() ? BigDecimal.ZERO : net.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP))
                .build();
    }

    private List<ProcessedCommerceDataResponse.ProductStatisticDto> toProductStatistics(List<OrderItem> items, Map<UUID, Product> productsById) {
        Map<String, ProductAgg> aggByKey = new LinkedHashMap<>();
        for (OrderItem item : items) {
            String key = item.getProductId() == null ? item.getSku() : item.getProductId().toString();
            ProductAgg agg = aggByKey.computeIfAbsent(key, ignored -> new ProductAgg(item, productsById.get(item.getProductId())));
            agg.orderItemCount++;
            agg.quantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.gross = agg.gross.add(money(item.getGrossRevenueAmount()));
            agg.net = agg.net.add(money(item.getNetRevenueAmount()));
            agg.cogs = agg.cogs.add(money(item.getCogsAmount()));
            agg.adSpend = agg.adSpend.add(money(item.getAllocatedAdSpendAmount()));
            agg.netProfit = agg.netProfit.add(money(item.getNetProfitAmount()));
        }
        return aggByKey.values().stream()
                .sorted(Comparator.comparing((ProductAgg agg) -> agg.net).reversed())
                .map(ProductAgg::toDto)
                .toList();
    }

    private List<ProcessedCommerceDataResponse.DailyStatisticDto> toDailyStatistics(
            List<Order> orders,
            List<OrderItem> items,
            Map<UUID, Order> ordersById
    ) {
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
            agg.quantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.netProfit = agg.netProfit.subtract(money(item.getCogsAmount())).subtract(money(item.getAllocatedAdSpendAmount()));
        }

        return daily.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, DailyAgg>comparingByKey().reversed())
                .map(entry -> ProcessedCommerceDataResponse.DailyStatisticDto.builder()
                        .date(entry.getKey())
                        .orderCount(entry.getValue().orderIds.size())
                        .itemQuantity(entry.getValue().quantity)
                        .grossRevenueAmount(entry.getValue().gross)
                        .netRevenueAmount(entry.getValue().net)
                        .netProfitAmount(entry.getValue().netProfit)
                        .build())
                .toList();
    }

    private ProcessedCommerceDataResponse.OrderDto toOrderDto(Order order) {
        return ProcessedCommerceDataResponse.OrderDto.builder()
                .id(order.getId())
                .storeId(order.getStoreId())
                .uploadId(order.getUploadId())
                .platform(order.getPlatform())
                .externalOrderId(order.getExternalOrderId())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .paidAt(order.getPaidAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .buyerUsername(order.getBuyerUsername())
                .grossRevenueAmount(money(order.getGrossRevenueAmount()))
                .discountAmount(money(order.getDiscountAmount()))
                .refundAmount(money(order.getRefundAmount()))
                .netRevenueAmount(money(order.getNetRevenueAmount()))
                .platformFeeAmount(money(order.getPlatformFeeAmount()))
                .transactionFeeAmount(money(order.getTransactionFeeAmount()))
                .shippingFeeAmount(money(order.getShippingFeeAmount()))
                .currencyCode(order.getCurrencyCode())
                .build();
    }

    private ProcessedCommerceDataResponse.OrderItemDto toOrderItemDto(OrderItem item) {
        return ProcessedCommerceDataResponse.OrderItemDto.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .productId(item.getProductId())
                .externalItemId(item.getExternalItemId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(money(item.getUnitPrice()))
                .grossRevenueAmount(money(item.getGrossRevenueAmount()))
                .discountAmount(money(item.getDiscountAmount()))
                .refundAmount(money(item.getRefundAmount()))
                .netRevenueAmount(money(item.getNetRevenueAmount()))
                .cogsAmount(money(item.getCogsAmount()))
                .allocatedPlatformFeeAmount(money(item.getAllocatedPlatformFeeAmount()))
                .allocatedAdSpendAmount(money(item.getAllocatedAdSpendAmount()))
                .grossProfitAmount(money(item.getGrossProfitAmount()))
                .netProfitAmount(money(item.getNetProfitAmount()))
                .build();
    }

    private ProcessedCommerceDataResponse.ProductDto toProductDto(Product product) {
        return ProcessedCommerceDataResponse.ProductDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .categoryName(product.getCategoryName())
                .brandName(product.getBrandName())
                .status(product.getStatus())
                .build();
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

    private BigDecimal sumOrders(List<Order> orders, java.util.function.Function<Order, BigDecimal> getter) {
        return orders.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(List<OrderItem> items, java.util.function.Function<OrderItem, BigDecimal> getter) {
        return items.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private static class ProductAgg {
        private final UUID productId;
        private final String sku;
        private final String productName;
        private long orderItemCount;
        private long quantity;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal cogs = BigDecimal.ZERO;
        private BigDecimal adSpend = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;

        private ProductAgg(OrderItem item, Product product) {
            this.productId = item.getProductId();
            this.sku = product == null ? item.getSku() : product.getSku();
            this.productName = product == null ? item.getProductName() : product.getName();
        }

        private ProcessedCommerceDataResponse.ProductStatisticDto toDto() {
            return ProcessedCommerceDataResponse.ProductStatisticDto.builder()
                    .productId(productId)
                    .sku(sku)
                    .productName(productName)
                    .orderItemCount(orderItemCount)
                    .quantity(quantity)
                    .grossRevenueAmount(gross)
                    .netRevenueAmount(net)
                    .cogsAmount(cogs)
                    .adSpendAmount(adSpend)
                    .netProfitAmount(netProfit)
                    .build();
        }
    }

    private static class DailyAgg {
        private final Set<UUID> orderIds = new HashSet<>();
        private long quantity;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;
    }
}
