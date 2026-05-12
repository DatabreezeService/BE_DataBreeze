package databreeze.service.etl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MappingRuleBook {

    @Autowired
    private FieldNormalizer fieldNormalizer;

    private static final Map<String, List<String>> SHOPEE_ORDER_ALIASES = new LinkedHashMap<>();

    static {
        SHOPEE_ORDER_ALIASES.put("external_order_id", List.of(
                "ma don hang",
                "ma don",
                "ma don shopee",
                "order id",
                "order code",
                "external order id",
                "ma giao dich"
        ));

        SHOPEE_ORDER_ALIASES.put("order_date", List.of(
                "ngay dat hang",
                "thoi gian dat hang",
                "ngay tao don",
                "ngay phat sinh don",
                "order date",
                "created time",
                "created at"
        ));

        SHOPEE_ORDER_ALIASES.put("order_status", List.of(
                "trang thai don hang",
                "trang thai",
                "tinh trang don hang",
                "order status",
                "status"
        ));

        SHOPEE_ORDER_ALIASES.put("buyer_username", List.of(
                "ten dang nhap nguoi mua",
                "nguoi mua",
                "ten nguoi mua",
                "buyer username",
                "buyer",
                "customer"
        ));

        SHOPEE_ORDER_ALIASES.put("sku", List.of(
                "ma sku",
                "sku",
                "sku phan loai",
                "ma sku phan loai",
                "ma phan loai",
                "ma phan loai hang",
                "ma hang",
                "product sku",
                "item sku"
        ));

        SHOPEE_ORDER_ALIASES.put("product_name", List.of(
                "ten san pham",
                "san pham",
                "product name",
                "item name",
                "ten hang"
        ));

        SHOPEE_ORDER_ALIASES.put("quantity", List.of(
                "so luong",
                "sl",
                "qty",
                "quantity",
                "so luong san pham"
        ));

        SHOPEE_ORDER_ALIASES.put("unit_price", List.of(
                "don gia",
                "gia ban",
                "gia san pham",
                "unit price",
                "price"
        ));

        SHOPEE_ORDER_ALIASES.put("gross_revenue_amount", List.of(
                "doanh thu gop",
                "doanh thu san pham",
                "tong tien hang",
                "gross revenue",
                "gross amount"
        ));

        SHOPEE_ORDER_ALIASES.put("discount_amount", List.of(
                "giam gia",
                "voucher",
                "ma giam gia",
                "discount",
                "seller voucher",
                "shopee voucher"
        ));

        SHOPEE_ORDER_ALIASES.put("platform_fee_amount", List.of(
                "phi san",
                "phi nen tang",
                "platform fee",
                "commission fee",
                "phi co dinh"
        ));

        SHOPEE_ORDER_ALIASES.put("transaction_fee_amount", List.of(
                "phi thanh toan",
                "transaction fee",
                "payment fee",
                "phi giao dich"
        ));

        SHOPEE_ORDER_ALIASES.put("shipping_fee_amount", List.of(
                "phi van chuyen",
                "shipping fee",
                "ship fee",
                "phi ship",
                "tro gia van chuyen"
        ));

        SHOPEE_ORDER_ALIASES.put("refund_amount", List.of(
                "hoan tien",
                "refund",
                "tien hoan",
                "so tien hoan"
        ));

        SHOPEE_ORDER_ALIASES.put("net_revenue_amount", List.of(
                "doanh thu thuan",
                "doanh thu sau phi",
                "thuc nhan",
                "net revenue",
                "net amount",
                "tong thanh toan"
        ));
    }

    public Optional<String> findTargetFieldForShopeeOrder(String sourceColumn) {
        String normalizedSource = fieldNormalizer.normalize(sourceColumn);

        for (Map.Entry<String, List<String>> entry : SHOPEE_ORDER_ALIASES.entrySet()) {
            String targetField = entry.getKey();

            for (String alias : entry.getValue()) {
                String normalizedAlias = fieldNormalizer.normalize(alias);

                if (normalizedSource.equals(normalizedAlias)) {
                    return Optional.of(targetField);
                }

                if (normalizedSource.contains(normalizedAlias)) {
                    return Optional.of(targetField);
                }
            }
        }

        return Optional.empty();
    }

    public double confidenceFor(String sourceColumn, String targetField) {
        Optional<String> matched = findTargetFieldForShopeeOrder(sourceColumn);

        if (matched.isPresent() && matched.get().equals(targetField)) {
            return 0.98;
        }

        return 0.0;
    }

    public String reasonFor(String sourceColumn, String targetField) {
        return "Rule Shopee VN: cột \"" + sourceColumn + "\" được nhận diện là \"" + targetField + "\".";
    }
}