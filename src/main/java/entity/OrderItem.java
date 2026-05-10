package com.databreeze.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "external_item_id", length = 255)
    private String externalItemId;

    @Column(name = "sku", length = 255)
    private String sku;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "gross_revenue_amount")
    private BigDecimal grossRevenueAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "net_revenue_amount")
    private BigDecimal netRevenueAmount;

    @Column(name = "cogs_amount")
    private BigDecimal cogsAmount;

    @Column(name = "allocated_platform_fee_amount")
    private BigDecimal allocatedPlatformFeeAmount;

    @Column(name = "allocated_ad_spend_amount")
    private BigDecimal allocatedAdSpendAmount;

    @Column(name = "gross_profit_amount")
    private BigDecimal grossProfitAmount;

    @Column(name = "net_profit_amount")
    private BigDecimal netProfitAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
