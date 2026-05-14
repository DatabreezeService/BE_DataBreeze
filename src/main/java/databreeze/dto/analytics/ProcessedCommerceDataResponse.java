package databreeze.dto.analytics;

import databreeze.enums.CommercePlatform;
import databreeze.enums.OrderStatus;
import databreeze.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedCommerceDataResponse {
    private UUID workspaceId;
    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private SummaryDto summary;
    private List<OrderDto> orders;
    private List<OrderItemDto> orderItems;
    private List<ProductDto> products;
    private List<ProductStatisticDto> productStatistics;
    private List<DailyStatisticDto> dailyStatistics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryDto {
        private long orderCount;
        private long orderItemCount;
        private long productCount;
        private long totalQuantity;
        private BigDecimal grossRevenueAmount;
        private BigDecimal discountAmount;
        private BigDecimal refundAmount;
        private BigDecimal netRevenueAmount;
        private BigDecimal platformFeeAmount;
        private BigDecimal transactionFeeAmount;
        private BigDecimal shippingFeeAmount;
        private BigDecimal cogsAmount;
        private BigDecimal adSpendAmount;
        private BigDecimal grossProfitAmount;
        private BigDecimal netProfitAmount;
        private BigDecimal profitMargin;
        private BigDecimal averageOrderValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderDto {
        private UUID id;
        private UUID storeId;
        private UUID uploadId;
        private CommercePlatform platform;
        private String externalOrderId;
        private OrderStatus orderStatus;
        private OffsetDateTime orderDate;
        private OffsetDateTime paidAt;
        private OffsetDateTime completedAt;
        private OffsetDateTime cancelledAt;
        private String buyerUsername;
        private BigDecimal grossRevenueAmount;
        private BigDecimal discountAmount;
        private BigDecimal refundAmount;
        private BigDecimal netRevenueAmount;
        private BigDecimal platformFeeAmount;
        private BigDecimal transactionFeeAmount;
        private BigDecimal shippingFeeAmount;
        private String currencyCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        private UUID id;
        private UUID orderId;
        private UUID productId;
        private String externalItemId;
        private String sku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal grossRevenueAmount;
        private BigDecimal discountAmount;
        private BigDecimal refundAmount;
        private BigDecimal netRevenueAmount;
        private BigDecimal cogsAmount;
        private BigDecimal allocatedPlatformFeeAmount;
        private BigDecimal allocatedAdSpendAmount;
        private BigDecimal grossProfitAmount;
        private BigDecimal netProfitAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductDto {
        private UUID id;
        private String sku;
        private String name;
        private String categoryName;
        private String brandName;
        private ProductStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductStatisticDto {
        private UUID productId;
        private String sku;
        private String productName;
        private long orderItemCount;
        private long quantity;
        private BigDecimal grossRevenueAmount;
        private BigDecimal netRevenueAmount;
        private BigDecimal cogsAmount;
        private BigDecimal adSpendAmount;
        private BigDecimal netProfitAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyStatisticDto {
        private LocalDate date;
        private long orderCount;
        private long itemQuantity;
        private BigDecimal grossRevenueAmount;
        private BigDecimal netRevenueAmount;
        private BigDecimal netProfitAmount;
    }
}
