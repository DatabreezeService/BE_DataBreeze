package databreeze.service.shopee.impl;

import databreeze.dto.shopee.ShopeeImportResult;
import databreeze.dto.shopee.ShopeeNormalizedOrderRow;
import databreeze.entity.*;
import databreeze.enums.*;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.repository.ProductRepository;
import databreeze.repository.RawImportRowRepository;
import databreeze.service.shopee.ShopeeOrderImportService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Import nghiệp vụ Shopee VN.
 * Lớp này chịu trách nhiệm normalize từng dòng, validate field bắt buộc, upsert order/product/item.
 */
@Service
public class ShopeeOrderImportServiceImpl implements ShopeeOrderImportService {
    @Autowired
    private RawImportRowRepository rawRowRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public ShopeeImportResult importOrders(Upload upload, List<RawImportRow> rawRows, List<ImportColumnMapping> mappings, boolean skipInvalidRows) {
        Map<String, ImportColumnMapping> mappingBySourceColumn = mappings.stream()
                .collect(Collectors.toMap(ImportColumnMapping::getSourceColumnName, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        long successRows = 0;
        long failedRows = 0;
        long warningRows = 0;
        long createdOrders = 0;
        long createdOrderItems = 0;
        long createdProducts = 0;
        LocalDate minDate = null;
        LocalDate maxDate = null;

        for (RawImportRow rawRow : rawRows) {
            try {
                ShopeeNormalizedOrderRow normalized = normalize(rawRow.getRawData(), mappingBySourceColumn);
                List<String> errors = validate(normalized);
                rawRow.setNormalizedPreview(normalized.getNormalizedPreview());

                if (!errors.isEmpty()) {
                    failedRows++;
                    rawRow.setStatus(RawRowStatus.INVALID);
                    rawRow.setErrorMessages(Map.of("errors", errors));
                    rawRowRepository.save(rawRow);
                    if (!skipInvalidRows) break;
                    continue;
                }

                boolean orderExists = orderRepository
                        .findByWorkspaceIdAndPlatformAndExternalOrderId(upload.getWorkspaceId(), CommercePlatform.SHOPEE, normalized.getExternalOrderId())
                        .isPresent();
                Order order = upsertOrder(upload, normalized);
                if (!orderExists) createdOrders++;

                ProductUpsertResult productResult = upsertProduct(upload.getWorkspaceId(), normalized);
                if (productResult.isCreated()) createdProducts++;

                replaceOrderItem(upload, order, productResult.getProduct(), normalized);
                createdOrderItems++;

                rawRow.setStatus(RawRowStatus.IMPORTED);
                rawRow.setErrorMessages(null);
                rawRowRepository.save(rawRow);

                LocalDate businessDate = normalized.getPaidAt() != null ? normalized.getPaidAt() : normalized.getOrderDate();
                if (businessDate != null) {
                    minDate = minDate == null || businessDate.isBefore(minDate) ? businessDate : minDate;
                    maxDate = maxDate == null || businessDate.isAfter(maxDate) ? businessDate : maxDate;
                }
                successRows++;
            } catch (Exception ex) {
                failedRows++;
                rawRow.setStatus(RawRowStatus.INVALID);
                rawRow.setErrorMessages(Map.of("errors", List.of("Không thể import dòng " + rawRow.getRowNumber() + ": " + ex.getMessage())));
                rawRowRepository.save(rawRow);
                if (!skipInvalidRows) break;
            }
        }

        return new ShopeeImportResult(rawRows.size(), successRows, failedRows, warningRows, createdOrders, createdOrderItems, createdProducts, minDate, maxDate);
    }

    private ShopeeNormalizedOrderRow normalize(Map<String, Object> rawData, Map<String, ImportColumnMapping> mappingBySourceColumn) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            ImportColumnMapping mapping = mappingBySourceColumn.get(entry.getKey());
            if (mapping == null) continue;
            normalized.put(mapping.getTargetFieldName(), cast(entry.getValue(), mapping.getTargetDataType()));
        }

        BigDecimal gross = money(normalized.get("gross_revenue_amount"));
        BigDecimal discount = money(normalized.get("discount_amount"));
        BigDecimal refund = money(normalized.get("refund_amount"));
        BigDecimal platformFee = money(normalized.get("platform_fee_amount"));
        BigDecimal transactionFee = money(normalized.get("transaction_fee_amount"));
        BigDecimal shippingFee = money(normalized.get("shipping_fee_amount"));
        if (!normalized.containsKey("quantity") || normalized.get("quantity") == null) normalized.put("quantity", 1);
        if (!normalized.containsKey("net_revenue_amount") || normalized.get("net_revenue_amount") == null) {
            normalized.put("net_revenue_amount", gross.subtract(discount).subtract(refund).subtract(platformFee).subtract(transactionFee).subtract(shippingFee));
        }

        return new ShopeeNormalizedOrderRow(
                string(normalized.get("external_order_id")),
                string(normalized.get("order_status")),
                date(normalized.get("order_date")),
                date(normalized.get("paid_at")),
                string(normalized.get("buyer_username")),
                string(normalized.get("sku")),
                string(normalized.get("product_name")),
                integer(normalized.get("quantity")),
                money(normalized.get("unit_price")),
                money(normalized.get("gross_revenue_amount")),
                money(normalized.get("discount_amount")),
                money(normalized.get("refund_amount")),
                money(normalized.get("shipping_fee_amount")),
                money(normalized.get("platform_fee_amount")),
                money(normalized.get("transaction_fee_amount")),
                money(normalized.get("net_revenue_amount")),
                money(normalized.get("cogs_amount")),
                money(normalized.get("allocated_ad_spend_amount")),
                normalized
        );
    }

    private List<String> validate(ShopeeNormalizedOrderRow row) {
        List<String> errors = new ArrayList<>();
        if (isBlank(row.getExternalOrderId())) errors.add("Thiếu mã đơn hàng. Hãy mapping cột Shopee sang external_order_id.");
        if (isBlank(row.getSku())) errors.add("Thiếu SKU phân loại. Hãy mapping cột Shopee sang sku.");
        return errors;
    }

    private Order upsertOrder(Upload upload, ShopeeNormalizedOrderRow row) {
        Order order = orderRepository
                .findByWorkspaceIdAndPlatformAndExternalOrderId(upload.getWorkspaceId(), CommercePlatform.SHOPEE, row.getExternalOrderId())
                .orElseGet(Order::new);
        order.setWorkspaceId(upload.getWorkspaceId());
        order.setStoreId(upload.getStoreId());
        order.setUploadId(upload.getId());
        order.setPlatform(CommercePlatform.SHOPEE);
        order.setExternalOrderId(row.getExternalOrderId());
        order.setOrderStatus(parseOrderStatus(row.getOrderStatus()));
        order.setOrderDate(toOffset(row.getOrderDate()));
        order.setPaidAt(toOffset(row.getPaidAt()));
        order.setBuyerUsername(row.getBuyerUsername());
        order.setGrossRevenueAmount(row.getGrossRevenueAmount());
        order.setDiscountAmount(row.getDiscountAmount());
        order.setRefundAmount(row.getRefundAmount());
        order.setNetRevenueAmount(row.getNetRevenueAmount());
        order.setPlatformFeeAmount(row.getPlatformFeeAmount());
        order.setTransactionFeeAmount(row.getTransactionFeeAmount());
        order.setShippingFeeAmount(row.getShippingFeeAmount());
        order.setCurrencyCode("VND");
        return orderRepository.save(order);
    }

    private ProductUpsertResult upsertProduct(UUID workspaceId, ShopeeNormalizedOrderRow row) {
        Optional<Product> existing = productRepository.findByWorkspaceIdAndSku(workspaceId, row.getSku());
        if (existing.isPresent()) return new ProductUpsertResult(existing.get(), false);
        Product product = productRepository.save(Product.builder()
                .workspaceId(workspaceId)
                .sku(row.getSku())
                .name(isBlank(row.getProductName()) ? row.getSku() : row.getProductName())
                .status(ProductStatus.ACTIVE)
                .build());
        return new ProductUpsertResult(product, true);
    }

    private void replaceOrderItem(Upload upload, Order order, Product product, ShopeeNormalizedOrderRow row) {
        orderItemRepository.deleteByOrderId(order.getId());
        BigDecimal grossProfit = row.getNetRevenueAmount().subtract(row.getCogsAmount());
        BigDecimal netProfit = grossProfit.subtract(row.getPlatformFeeAmount()).subtract(row.getAllocatedAdSpendAmount());
        orderItemRepository.save(OrderItem.builder()
                .workspaceId(upload.getWorkspaceId())
                .orderId(order.getId())
                .productId(product.getId())
                .sku(row.getSku())
                .productName(row.getProductName())
                .quantity(row.getQuantity() == null ? 1 : row.getQuantity())
                .unitPrice(row.getUnitPrice())
                .grossRevenueAmount(row.getGrossRevenueAmount())
                .discountAmount(row.getDiscountAmount())
                .refundAmount(row.getRefundAmount())
                .netRevenueAmount(row.getNetRevenueAmount())
                .cogsAmount(row.getCogsAmount())
                .allocatedPlatformFeeAmount(row.getPlatformFeeAmount())
                .allocatedAdSpendAmount(row.getAllocatedAdSpendAmount())
                .grossProfitAmount(grossProfit)
                .netProfitAmount(netProfit)
                .build());
    }

    private Object cast(Object value, TargetDataType type) {
        if (value == null) return null;
        String text = value.toString().trim();
        if (text.isBlank()) return null;
        return switch (type) {
            case INTEGER -> integer(text);
            case DECIMAL, CURRENCY -> money(text);
            case DATE, DATETIME -> date(text);
            case BOOLEAN -> text.equalsIgnoreCase("true") || text.equals("1") || text.equalsIgnoreCase("có");
            case JSON, STRING -> text;
        };
    }

    private OrderStatus parseOrderStatus(String text) {
        if (text == null) return OrderStatus.UNKNOWN;
        String value = text.toLowerCase(Locale.ROOT);
        if (value.contains("hoàn thành") || value.contains("completed")) return OrderStatus.COMPLETED;
        if (value.contains("hủy") || value.contains("cancel")) return OrderStatus.CANCELLED;
        if (value.contains("hoàn tiền") || value.contains("refund")) return OrderStatus.REFUNDED;
        if (value.contains("trả hàng") || value.contains("return")) return OrderStatus.RETURNED;
        if (value.contains("đang giao") || value.contains("shipped")) return OrderStatus.SHIPPED;
        if (value.contains("đã thanh toán") || value.contains("paid")) return OrderStatus.PAID;
        if (value.contains("xử lý") || value.contains("processing")) return OrderStatus.PROCESSING;
        return OrderStatus.UNKNOWN;
    }

    private OffsetDateTime toOffset(LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.ofHours(7));
    }

    private LocalDate date(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate d) return d;
        if (value instanceof LocalDateTime dt) return dt.toLocalDate();
        if (value instanceof OffsetDateTime odt) return odt.toLocalDate();
        String text = value.toString().trim();
        if (text.isBlank()) return null;
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("M/d/yyyy")
        );
        for (DateTimeFormatter formatter : formats) {
            try { return LocalDate.parse(text, formatter); } catch (Exception ignored) {}
        }
        try { return LocalDate.of(1899, 12, 30).plusDays((long) Double.parseDouble(text)); } catch (Exception ignored) {}
        return null;
    }

    private BigDecimal money(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        String text = value.toString().trim();
        if (text.isBlank()) return BigDecimal.ZERO;
        String cleaned = text.replaceAll("[^0-9,.-]", "");
        if (cleaned.contains(",") && cleaned.contains(".")) cleaned = cleaned.replace(".", "").replace(",", ".");
        else cleaned = cleaned.replace(",", ".");
        if (cleaned.isBlank() || cleaned.equals("-")) return BigDecimal.ZERO;
        return new BigDecimal(cleaned);
    }

    private Integer integer(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        String text = value.toString().trim();
        if (text.isBlank()) return null;
        return (int) Math.round(Double.parseDouble(text.replaceAll("[^0-9,.-]", "").replace(",", ".")));
    }

    private String string(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    @Data
    @AllArgsConstructor
    private static class ProductUpsertResult {
        private Product product;
        private boolean created;
    }
}
