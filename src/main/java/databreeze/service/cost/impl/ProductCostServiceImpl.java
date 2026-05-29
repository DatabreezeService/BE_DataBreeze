package databreeze.service.cost.impl;

import databreeze.dto.cost.ApplyProductCostRequest;
import databreeze.dto.cost.ApplyProductCostResponse;
import databreeze.dto.cost.BulkProductCostRequest;
import databreeze.dto.cost.MissingCostSkuResponse;
import databreeze.dto.cost.ProductCostRequest;
import databreeze.dto.cost.ProductCostResponse;
import databreeze.dto.shopee.ShopeeDailyCalculationResult;
import databreeze.entity.Order;
import databreeze.entity.OrderItem;
import databreeze.entity.Product;
import databreeze.entity.ProductCost;
import databreeze.enums.CommercePlatform;
import databreeze.enums.CostType;
import databreeze.enums.WorkspacePermission;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.repository.ProductCostRepository;
import databreeze.repository.ProductRepository;
import databreeze.service.analytics.ShopeeAnalyticsService;
import databreeze.service.cost.ProductCostService;
import databreeze.service.workspace.WorkspaceAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductCostServiceImpl implements ProductCostService {

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private ProductCostRepository productCostRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ShopeeAnalyticsService shopeeAnalyticsService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductCostResponse> listCosts(UUID workspaceId, UUID actorUserId, String sku) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.READ_FINANCIAL_DATA);
        List<ProductCost> costs = sku == null || sku.isBlank()
                ? productCostRepository.findByWorkspaceIdOrderBySkuAscEffectiveFromDesc(workspaceId)
                : productCostRepository.findByWorkspaceIdAndSkuIgnoreCaseOrderByEffectiveFromDesc(workspaceId, sku.trim());
        return costs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ProductCostResponse createCost(UUID workspaceId, UUID actorUserId, ProductCostRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        ProductCost cost = new ProductCost();
        applyRequest(workspaceId, cost, request);
        return toResponse(productCostRepository.save(cost));
    }

    @Override
    @Transactional
    public List<ProductCostResponse> bulkCreateCosts(UUID workspaceId, UUID actorUserId, BulkProductCostRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        if (request == null || request.getCosts() == null || request.getCosts().isEmpty()) {
            throw new IllegalArgumentException("Danh sach gia von khong duoc de trong.");
        }
        List<ProductCostResponse> responses = new ArrayList<>();
        for (ProductCostRequest item : request.getCosts()) {
            ProductCost cost = new ProductCost();
            applyRequest(workspaceId, cost, item);
            responses.add(toResponse(productCostRepository.save(cost)));
        }
        if (request.isApplyAfterSave()) {
            applyCosts(workspaceId, actorUserId, new ApplyProductCostRequest(request.getStoreId(), request.getFromDate(), request.getToDate(), true));
        }
        return responses;
    }

    @Override
    @Transactional
    public ProductCostResponse updateCost(UUID workspaceId, UUID actorUserId, UUID costId, ProductCostRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        ProductCost cost = requireCost(workspaceId, costId);
        applyRequest(workspaceId, cost, request);
        return toResponse(productCostRepository.save(cost));
    }

    @Override
    @Transactional
    public void deleteCost(UUID workspaceId, UUID actorUserId, UUID costId) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        ProductCost cost = requireCost(workspaceId, costId);
        productCostRepository.delete(cost);
    }

    @Override
    @Transactional
    public ApplyProductCostResponse applyCosts(UUID workspaceId, UUID actorUserId, ApplyProductCostRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_FINANCIAL_DATA);
        ApplyProductCostRequest safeRequest = request == null ? new ApplyProductCostRequest() : request;
        UUID storeId = safeRequest.getStoreId();
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);

        List<Order> orders = loadOrders(workspaceId, storeId).stream()
                .filter(order -> order.getPlatform() == CommercePlatform.SHOPEE)
                .toList();
        DateRange range = inferDateRange(orders, safeRequest.getFromDate(), safeRequest.getToDate());
        Map<UUID, Order> orderById = orders.stream()
                .filter(order -> inDateRange(businessDate(order), range.fromDate(), range.toDate()))
                .collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));
        List<OrderItem> items = orderItemRepository.findByWorkspaceId(workspaceId).stream()
                .filter(item -> orderById.containsKey(item.getOrderId()))
                .toList();
        Map<String, List<ProductCost>> costsBySku = productCostRepository
                .findByWorkspaceIdAndCostTypeOrderBySkuAscEffectiveFromDesc(workspaceId, CostType.COGS)
                .stream()
                .collect(Collectors.groupingBy(cost -> normalizeSku(cost.getSku())));

        long matched = 0;
        long updated = 0;
        long missingCount = 0;
        Map<String, String> missingSkus = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Order order = orderById.get(item.getOrderId());
            LocalDate date = businessDate(order);
            ProductCost cost = matchCost(costsBySku.get(normalizeSku(item.getSku())), date);
            if (cost == null) {
                missingCount++;
                missingSkus.putIfAbsent(item.getSku(), item.getProductName());
                continue;
            }
            matched++;
            BigDecimal cogs = money(cost.getUnitCost()).multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));
            BigDecimal grossProfit = money(item.getNetRevenueAmount()).subtract(cogs);
            BigDecimal netProfit = grossProfit.subtract(money(item.getAllocatedPlatformFeeAmount())).subtract(money(item.getAllocatedAdSpendAmount()));
            if (!sameMoney(item.getCogsAmount(), cogs) || !sameMoney(item.getNetProfitAmount(), netProfit)) {
                item.setCogsAmount(cogs);
                item.setGrossProfitAmount(grossProfit);
                item.setNetProfitAmount(netProfit);
                orderItemRepository.save(item);
                updated++;
            }
        }

        ShopeeDailyCalculationResult dailyResult = null;
        if (safeRequest.isRecalculateDashboard()) {
            dailyResult = shopeeAnalyticsService.recalculateDaily(workspaceId, storeId, range.fromDate(), range.toDate());
        }

        return ApplyProductCostResponse.builder()
                .workspaceId(workspaceId)
                .storeId(storeId)
                .fromDate(range.fromDate())
                .toDate(range.toDate())
                .matchedOrderItems(matched)
                .updatedOrderItems(updated)
                .missingCostOrderItems(missingCount)
                .missingSkus(new ArrayList<>(missingSkus.keySet()))
                .dailyCalculation(dailyResult)
                .message("Da ap gia von vao order_items va tinh lai dashboard neu duoc yeu cau.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissingCostSkuResponse> listMissingCostSkus(UUID workspaceId, UUID actorUserId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.READ_FINANCIAL_DATA);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);
        List<Order> orders = loadOrders(workspaceId, storeId).stream()
                .filter(order -> order.getPlatform() == CommercePlatform.SHOPEE)
                .filter(order -> inDateRange(businessDate(order), fromDate, toDate))
                .toList();
        Set<UUID> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        Map<UUID, Order> orderById = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));
        Map<String, MissingAgg> bySku = new LinkedHashMap<>();
        for (OrderItem item : orderItemRepository.findByWorkspaceId(workspaceId)) {
            if (!orderIds.contains(item.getOrderId())) {
                continue;
            }
            if (money(item.getNetRevenueAmount()).signum() <= 0 || money(item.getCogsAmount()).signum() > 0) {
                continue;
            }
            String key = normalizeSku(item.getSku());
            MissingAgg agg = bySku.computeIfAbsent(key, ignored -> new MissingAgg(item));
            agg.orderItemCount++;
            agg.totalQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            agg.netRevenue = agg.netRevenue.add(money(item.getNetRevenueAmount()));
            LocalDate date = businessDate(orderById.get(item.getOrderId()));
            if (date != null && (agg.firstBusinessDate == null || date.isBefore(agg.firstBusinessDate))) {
                agg.firstBusinessDate = date;
            }
            if (date != null && (agg.lastBusinessDate == null || date.isAfter(agg.lastBusinessDate))) {
                agg.lastBusinessDate = date;
            }
        }
        return bySku.values().stream()
                .sorted(Comparator.comparing((MissingAgg agg) -> agg.netRevenue).reversed())
                .map(MissingAgg::toResponse)
                .toList();
    }

    private void applyRequest(UUID workspaceId, ProductCost cost, ProductCostRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu gia von khong duoc de trong.");
        }
        String sku = normalizeDisplaySku(request.getSku());
        cost.setWorkspaceId(workspaceId);
        cost.setSku(sku);
        cost.setCostType(request.getCostType() == null ? CostType.COGS : request.getCostType());
        cost.setUnitCost(money(request.getUnitCost()));
        cost.setCurrencyCode(request.getCurrencyCode() == null || request.getCurrencyCode().isBlank() ? "VND" : request.getCurrencyCode().trim().toUpperCase(Locale.ROOT));
        cost.setEffectiveFrom(request.getEffectiveFrom() == null ? LocalDate.of(1900, 1, 1) : request.getEffectiveFrom());
        cost.setEffectiveTo(request.getEffectiveTo());
        productRepository.findByWorkspaceIdAndSku(workspaceId, sku).map(Product::getId).ifPresent(cost::setProductId);
    }

    private ProductCost requireCost(UUID workspaceId, UUID costId) {
        return productCostRepository.findByIdAndWorkspaceId(costId, workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay gia von SKU."));
    }

    private List<Order> loadOrders(UUID workspaceId, UUID storeId) {
        return storeId == null ? orderRepository.findByWorkspaceId(workspaceId) : orderRepository.findByWorkspaceIdAndStoreId(workspaceId, storeId);
    }

    private ProductCost matchCost(List<ProductCost> costs, LocalDate businessDate) {
        if (costs == null || costs.isEmpty() || businessDate == null) {
            return null;
        }
        return costs.stream()
                .filter(cost -> !cost.getEffectiveFrom().isAfter(businessDate))
                .filter(cost -> cost.getEffectiveTo() == null || !cost.getEffectiveTo().isBefore(businessDate))
                .max(Comparator.comparing(ProductCost::getEffectiveFrom))
                .orElse(null);
    }

    private DateRange inferDateRange(List<Order> orders, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : orders.stream().map(this::businessDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
        LocalDate to = toDate != null ? toDate : orders.stream().map(this::businessDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        return new DateRange(from, to);
    }

    private LocalDate businessDate(Order order) {
        if (order == null) {
            return null;
        }
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

    private String normalizeDisplaySku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU khong duoc de trong.");
        }
        return sku.trim();
    }

    private String normalizeSku(String sku) {
        return sku == null ? "" : sku.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean sameMoney(BigDecimal left, BigDecimal right) {
        return money(left).compareTo(money(right)) == 0;
    }

    private ProductCostResponse toResponse(ProductCost cost) {
        return ProductCostResponse.builder()
                .id(cost.getId())
                .workspaceId(cost.getWorkspaceId())
                .productId(cost.getProductId())
                .sku(cost.getSku())
                .costType(cost.getCostType())
                .unitCost(cost.getUnitCost())
                .currencyCode(cost.getCurrencyCode())
                .effectiveFrom(cost.getEffectiveFrom())
                .effectiveTo(cost.getEffectiveTo())
                .createdAt(cost.getCreatedAt())
                .updatedAt(cost.getUpdatedAt())
                .build();
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private static class MissingAgg {
        private final String sku;
        private final String productName;
        private long orderItemCount;
        private long totalQuantity;
        private BigDecimal netRevenue = BigDecimal.ZERO;
        private LocalDate firstBusinessDate;
        private LocalDate lastBusinessDate;

        private MissingAgg(OrderItem item) {
            this.sku = item.getSku();
            this.productName = item.getProductName();
        }

        private MissingCostSkuResponse toResponse() {
            return MissingCostSkuResponse.builder()
                    .sku(sku)
                    .productName(productName)
                    .orderItemCount(orderItemCount)
                    .totalQuantity(totalQuantity)
                    .netRevenueAmount(netRevenue)
                    .firstBusinessDate(firstBusinessDate)
                    .lastBusinessDate(lastBusinessDate)
                    .build();
        }
    }
}
