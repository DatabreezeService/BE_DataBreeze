package databreeze.dto.shopee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopeeNormalizedOrderRow {
    private String externalOrderId;
    private String orderStatus;
    private LocalDate orderDate;
    private LocalDate paidAt;
    private String buyerUsername;
    private String sku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal grossRevenueAmount;
    private BigDecimal discountAmount;
    private BigDecimal refundAmount;
    private BigDecimal shippingFeeAmount;
    private BigDecimal platformFeeAmount;
    private BigDecimal transactionFeeAmount;
    private BigDecimal netRevenueAmount;
    private BigDecimal cogsAmount;
    private BigDecimal allocatedAdSpendAmount;
    private Map<String, Object> normalizedPreview;
}
