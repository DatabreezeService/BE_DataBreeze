package databreeze.service.shopee.impl;

import databreeze.dto.shopee.ShopeeImportResult;
import databreeze.entity.ImportColumnMapping;
import databreeze.entity.Order;
import databreeze.entity.OrderItem;
import databreeze.entity.Product;
import databreeze.entity.RawImportRow;
import databreeze.entity.Upload;
import databreeze.enums.CommercePlatform;
import databreeze.enums.OrderStatus;
import databreeze.enums.ProductStatus;
import databreeze.enums.RawRowStatus;
import databreeze.repository.OrderItemRepository;
import databreeze.repository.OrderRepository;
import databreeze.repository.ProductRepository;
import databreeze.repository.RawImportRowRepository;
import databreeze.service.shopee.ShopeeOrderImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShopeeOrderImportServiceImpl implements ShopeeOrderImportService {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RawImportRowRepository rawImportRowRepository;

    @Override
    public ShopeeImportResult importOrders(
            Upload upload,
            List<RawImportRow> rawRows,
            List<ImportColumnMapping> mappings,
            boolean skipInvalidRows
    ) {
        ShopeeImportResult result = new ShopeeImportResult();
        if (upload == null || rawRows == null || rawRows.isEmpty()) {
            return result;
        }

        Map<String, String> sourceToTargetMap = buildSourceToTargetMap(mappings);
        Map<String, Order> orderCache = new LinkedHashMap<>();

        for (RawImportRow rawRow : rawRows) {
            result.setTotalRows(result.getTotalRows() + 1);
            try {
                Map<String, Object> normalizedRow = normalizeRow(rawRow.getRawData(), sourceToTargetMap);
                validateRequiredFields(normalizedRow);
                List<String> warnings = validateWarnings(normalizedRow);

                Order order = resolveOrder(upload, normalizedRow, orderCache, result);
                Product product = resolveProduct(upload.getWorkspaceId(), normalizedRow, result);
                createOrderItem(upload.getWorkspaceId(), order, product, normalizedRow, result);
                updateOrderAmounts(order, normalizedRow);
                orderRepository.save(order);
                updateBusinessDateRange(order, result);

                result.setSuccessRows(result.getSuccessRows() + 1);
                markRowImported(rawRow, normalizedRow, warnings);
                if (!warnings.isEmpty()) {
                    result.setWarningRows(result.getWarningRows() + 1);
                }
            } catch (Exception ex) {
                result.setFailedRows(result.getFailedRows() + 1);
                markRowInvalid(rawRow, ex);
                if (!skipInvalidRows) {
                    throw ex;
                }
            }
        }

        return result;
    }

    private Map<String, String> buildSourceToTargetMap(List<ImportColumnMapping> mappings) {
        Map<String, String> result = new LinkedHashMap<>();
        if (mappings == null) {
            return result;
        }
        for (ImportColumnMapping mapping : mappings) {
            if (mapping == null || !Boolean.TRUE.equals(mapping.getUserConfirmed())) {
                continue;
            }
            if (isBlank(mapping.getSourceColumnName()) || isBlank(mapping.getTargetFieldName())) {
                continue;
            }
            result.put(mapping.getSourceColumnName(), mapping.getTargetFieldName());
        }
        return result;
    }

    private Map<String, Object> normalizeRow(Map<String, Object> rawData, Map<String, String> sourceToTargetMap) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (rawData == null || sourceToTargetMap == null) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : sourceToTargetMap.entrySet()) {
            normalized.put(entry.getValue(), rawData.get(entry.getKey()));
        }
        return normalized;
    }

    private void validateRequiredFields(Map<String, Object> row) {
        if (isBlank(stringValue(row.get("external_order_id")))) {
            throw new IllegalArgumentException("Missing external_order_id.");
        }
        if (isBlank(stringValue(row.get("sku")))) {
            throw new IllegalArgumentException("Missing sku.");
        }
        if (isBlank(stringValue(row.get("product_name")))) {
            throw new IllegalArgumentException("Missing product_name.");
        }
        if (integerValue(row.get("quantity"), 1) <= 0) {
            throw new IllegalArgumentException("Invalid quantity.");
        }
    }

    private List<String> validateWarnings(Map<String, Object> row) {
        List<String> warnings = new java.util.ArrayList<>();
        BigDecimal netRevenue = decimalValue(row.get("net_revenue_amount"));
        BigDecimal unitPrice = decimalValue(row.get("unit_price"));
        int quantity = integerValue(row.get("quantity"), 1);
        BigDecimal computedRevenue = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (netRevenue.signum() == 0 && computedRevenue.signum() > 0) {
            netRevenue = computedRevenue;
        }
        if (netRevenue.signum() > 0 && decimalValue(row.get("cogs_amount")).signum() == 0) {
            warnings.add("Thiếu giá vốn/COGS nên lợi nhuận SKU có thể chưa chính xác.");
        }
        if (parseDateTime(row.get("order_date")) == null && parseDateTime(row.get("paid_at")) == null) {
            warnings.add("Thiếu ngày đơn hàng/ngày thanh toán nên dashboard theo ngày có thể bỏ qua dòng này.");
        }
        return warnings;
    }

    private void markRowImported(RawImportRow rawRow, Map<String, Object> normalizedRow, List<String> warnings) {
        rawRow.setNormalizedPreview(normalizedRow);
        rawRow.setStatus(warnings.isEmpty() ? RawRowStatus.IMPORTED : RawRowStatus.WARNING);
        rawRow.setErrorMessages(null);
        rawRow.setWarningMessages(warnings.isEmpty() ? null : Map.of("messages", warnings));
        rawImportRowRepository.save(rawRow);
    }

    private void markRowInvalid(RawImportRow rawRow, Exception ex) {
        rawRow.setStatus(RawRowStatus.INVALID);
        rawRow.setErrorMessages(Map.of(
                "message", ex.getMessage() == null ? "Import row failed." : ex.getMessage(),
                "exception", ex.getClass().getSimpleName(),
                "rowNumber", rawRow.getRowNumber()
        ));
        rawImportRowRepository.save(rawRow);
    }

    private Order resolveOrder(
            Upload upload,
            Map<String, Object> row,
            Map<String, Order> orderCache,
            ShopeeImportResult result
    ) {
        String externalOrderId = stringValue(row.get("external_order_id"));
        Order cached = orderCache.get(externalOrderId);
        if (cached != null) {
            return cached;
        }

        Optional<Order> existing = orderRepository.findByWorkspaceIdAndPlatformAndExternalOrderId(
                upload.getWorkspaceId(),
                CommercePlatform.SHOPEE,
                externalOrderId
        );

        Order order;
        if (existing.isPresent()) {
            order = existing.get();
            orderItemRepository.deleteByOrderId(order.getId());
        } else {
            order = new Order();
            order.setWorkspaceId(upload.getWorkspaceId());
            order.setExternalOrderId(externalOrderId);
            order.setPlatform(CommercePlatform.SHOPEE);
            result.setCreatedOrders(result.getCreatedOrders() + 1);
        }

        order.setStoreId(upload.getStoreId());
        order.setUploadId(upload.getId());
        order.setOrderStatus(normalizeOrderStatus(stringValue(row.get("order_status"))));
        order.setOrderDate(parseDateTime(row.get("order_date")));
        order.setPaidAt(parseDateTime(row.get("paid_at")));
        order.setBuyerUsername(stringValue(row.get("buyer_username")));
        order.setCurrencyCode("VND");
        resetOrderAmounts(order);

        Order saved = orderRepository.save(order);
        orderCache.put(externalOrderId, saved);
        return saved;
    }

    private Product resolveProduct(UUID workspaceId, Map<String, Object> row, ShopeeImportResult result) {
        String sku = stringValue(row.get("sku"));
        String productName = stringValue(row.get("product_name"));
        Optional<Product> existing = productRepository.findByWorkspaceIdAndSku(workspaceId, sku);
        if (existing.isPresent()) {
            Product product = existing.get();
            if (!isBlank(productName) && !Objects.equals(product.getName(), productName)) {
                product.setName(productName);
                return productRepository.save(product);
            }
            return product;
        }

        Product product = new Product();
        product.setWorkspaceId(workspaceId);
        product.setSku(sku);
        product.setName(productName);
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);
        result.setCreatedProducts(result.getCreatedProducts() + 1);
        return saved;
    }

    private void createOrderItem(
            UUID workspaceId,
            Order order,
            Product product,
            Map<String, Object> row,
            ShopeeImportResult result
    ) {
        int quantity = integerValue(row.get("quantity"), 1);
        BigDecimal unitPrice = decimalValue(row.get("unit_price"));
        BigDecimal gross = amountOrComputed(row.get("gross_revenue_amount"), unitPrice.multiply(BigDecimal.valueOf(quantity)));
        BigDecimal discount = decimalValue(row.get("discount_amount"));
        BigDecimal refund = decimalValue(row.get("refund_amount"));
        BigDecimal net = amountOrComputed(row.get("net_revenue_amount"), gross.subtract(discount).subtract(refund));
        BigDecimal cogs = decimalValue(row.get("cogs_amount"));
        BigDecimal adSpend = decimalValue(row.get("allocated_ad_spend_amount"));
        BigDecimal platformFee = decimalValue(row.get("platform_fee_amount"));
        BigDecimal grossProfit = net.subtract(cogs);
        BigDecimal netProfit = grossProfit.subtract(platformFee).subtract(adSpend);

        OrderItem item = new OrderItem();
        item.setWorkspaceId(workspaceId);
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setSku(stringValue(row.get("sku")));
        item.setProductName(stringValue(row.get("product_name")));
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setGrossRevenueAmount(gross);
        item.setDiscountAmount(discount);
        item.setRefundAmount(refund);
        item.setNetRevenueAmount(net);
        item.setAllocatedPlatformFeeAmount(platformFee);
        item.setAllocatedAdSpendAmount(adSpend);
        item.setCogsAmount(cogs);
        item.setGrossProfitAmount(grossProfit);
        item.setNetProfitAmount(netProfit);
        orderItemRepository.save(item);
        result.setCreatedOrderItems(result.getCreatedOrderItems() + 1);
    }

    private void resetOrderAmounts(Order order) {
        order.setGrossRevenueAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setNetRevenueAmount(BigDecimal.ZERO);
        order.setPlatformFeeAmount(BigDecimal.ZERO);
        order.setTransactionFeeAmount(BigDecimal.ZERO);
        order.setShippingFeeAmount(BigDecimal.ZERO);
    }

    private void updateOrderAmounts(Order order, Map<String, Object> row) {
        int quantity = integerValue(row.get("quantity"), 1);
        BigDecimal unitPrice = decimalValue(row.get("unit_price"));
        BigDecimal gross = amountOrComputed(row.get("gross_revenue_amount"), unitPrice.multiply(BigDecimal.valueOf(quantity)));
        BigDecimal discount = decimalValue(row.get("discount_amount"));
        BigDecimal refund = decimalValue(row.get("refund_amount"));
        BigDecimal net = amountOrComputed(row.get("net_revenue_amount"), gross.subtract(discount).subtract(refund));

        order.setGrossRevenueAmount(money(order.getGrossRevenueAmount()).add(gross));
        order.setDiscountAmount(money(order.getDiscountAmount()).add(discount));
        order.setRefundAmount(money(order.getRefundAmount()).add(refund));
        order.setNetRevenueAmount(money(order.getNetRevenueAmount()).add(net));
        order.setPlatformFeeAmount(money(order.getPlatformFeeAmount()).add(decimalValue(row.get("platform_fee_amount"))));
        order.setTransactionFeeAmount(money(order.getTransactionFeeAmount()).add(decimalValue(row.get("transaction_fee_amount"))));
        order.setShippingFeeAmount(money(order.getShippingFeeAmount()).add(decimalValue(row.get("shipping_fee_amount"))));
    }

    private void updateBusinessDateRange(Order order, ShopeeImportResult result) {
        LocalDate businessDate = null;
        if (order.getPaidAt() != null) {
            businessDate = order.getPaidAt().toLocalDate();
        } else if (order.getOrderDate() != null) {
            businessDate = order.getOrderDate().toLocalDate();
        }
        if (businessDate == null) {
            return;
        }
        if (result.getMinBusinessDate() == null || businessDate.isBefore(result.getMinBusinessDate())) {
            result.setMinBusinessDate(businessDate);
        }
        if (result.getMaxBusinessDate() == null || businessDate.isAfter(result.getMaxBusinessDate())) {
            result.setMaxBusinessDate(businessDate);
        }
    }

    private OrderStatus normalizeOrderStatus(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return OrderStatus.UNKNOWN;
        }
        if (normalized.contains("hoan thanh") || normalized.contains("completed")) {
            return OrderStatus.COMPLETED;
        }
        if (normalized.contains("huy") || normalized.contains("cancel")) {
            return OrderStatus.CANCELLED;
        }
        if (normalized.contains("hoan tien") || normalized.contains("refund")) {
            return OrderStatus.REFUNDED;
        }
        if (normalized.contains("tra hang") || normalized.contains("return")) {
            return OrderStatus.RETURNED;
        }
        if (normalized.contains("dang giao") || normalized.contains("shipped")) {
            return OrderStatus.SHIPPED;
        }
        if (normalized.contains("cho") || normalized.contains("pending")) {
            return OrderStatus.PENDING;
        }
        return OrderStatus.UNKNOWN;
    }

    private OffsetDateTime parseDateTime(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(VIETNAM_ZONE).toOffsetDateTime();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atTime(12, 0).atZone(VIETNAM_ZONE).toOffsetDateTime();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(VIETNAM_ZONE).toOffsetDateTime();
        }
        if (value instanceof Number number) {
            OffsetDateTime excelDate = parseExcelSerialDate(number);
            if (excelDate != null) {
                return excelDate;
            }
        }
        OffsetDateTime arrayDate = parseArrayDate(value);
        if (arrayDate != null) {
            return arrayDate;
        }

        String text = stringValue(value);
        if (isBlank(text)) {
            return null;
        }
        OffsetDateTime bracketArrayDate = parseBracketArrayDate(text);
        if (bracketArrayDate != null) {
            return bracketArrayDate;
        }

        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(text, formatter).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {
            }
        }

        List<DateTimeFormatter> dateFormatters = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );
        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                return LocalDate.parse(text, formatter).atTime(12, 0).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private OffsetDateTime parseArrayDate(Object value) {
        List<Integer> parts = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                Integer parsed = integerPart(item);
                if (parsed == null) {
                    return null;
                }
                parts.add(parsed);
            }
        } else if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Integer parsed = integerPart(Array.get(value, i));
                if (parsed == null) {
                    return null;
                }
                parts.add(parsed);
            }
        } else {
            return null;
        }
        return parseDateParts(parts);
    }

    private OffsetDateTime parseBracketArrayDate(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }
        String body = trimmed.substring(1, trimmed.length() - 1);
        if (body.isBlank()) {
            return null;
        }
        List<Integer> parts = new ArrayList<>();
        for (String token : body.split(",")) {
            Integer parsed = integerPart(token.trim());
            if (parsed == null) {
                return null;
            }
            parts.add(parsed);
        }
        return parseDateParts(parts);
    }

    private OffsetDateTime parseDateParts(List<Integer> parts) {
        if (parts == null || parts.size() < 3) {
            return null;
        }
        try {
            int year = parts.get(0);
            int month = parts.get(1);
            int day = parts.get(2);
            int hour = parts.size() > 3 ? parts.get(3) : 12;
            int minute = parts.size() > 4 ? parts.get(4) : 0;
            int second = parts.size() > 5 ? parts.get(5) : 0;
            return LocalDateTime.of(year, month, day, hour, minute, second)
                    .atZone(VIETNAM_ZONE)
                    .toOffsetDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private OffsetDateTime parseExcelSerialDate(Number number) {
        double serial = number.doubleValue();
        if (serial < 20000 || serial > 80000) {
            return null;
        }
        long wholeDays = (long) Math.floor(serial);
        double fraction = serial - wholeDays;
        LocalDate date = LocalDate.of(1899, 12, 30).plusDays(wholeDays);
        int seconds = (int) Math.round(fraction * 24 * 60 * 60);
        if (seconds == 0) {
            seconds = 12 * 60 * 60;
        }
        return date.atStartOfDay(VIETNAM_ZONE)
                .plusSeconds(seconds)
                .toOffsetDateTime();
    }

    private Integer integerPart(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer integerValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).replace(",", "").replace(".", "").trim();
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private BigDecimal amountOrComputed(Object rawValue, BigDecimal computed) {
        BigDecimal parsed = decimalValue(rawValue);
        return parsed.signum() == 0 ? money(computed) : parsed;
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        String text = String.valueOf(value)
                .replace("₫", "")
                .replace("đ", "")
                .replace("Đ", "")
                .replace("VND", "")
                .replace(",", "")
                .trim();
        if (text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
