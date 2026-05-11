package databreeze.dto.shoppee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ShopeeNormalizedOrderRow(
        String externanlOrderId,
        String orderStatus,
        LocalDate orderDate,
        LocalDate paidAt,
        String buyerUsername,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal grossRevenueAmount,
        BigDecimal discountAmount,
        BigDecimal refundAmount,
        BigDecimal shippingFeeAmount,
        BigDecimal transactionFeeAmount,
        BigDecimal netRevenueAmount,
        BigDecimal cogsAmount,
        BigDecimal allocatedAdSpendAmount,
        Map<String, Object> normalizedPreview
) {
}
