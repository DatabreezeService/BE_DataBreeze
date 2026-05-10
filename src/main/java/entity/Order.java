package com.databreeze.entity;

import com.databreeze.enums.CommercePlatform;
import com.databreeze.enums.OrderStatus;
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
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "upload_id")
    private UUID uploadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private CommercePlatform platform;

    @Column(name = "external_order_id", nullable = false, length = 255)
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 50)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.UNKNOWN;

    @Column(name = "order_date")
    private OffsetDateTime orderDate;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "buyer_username", length = 255)
    private String buyerUsername;

    @Column(name = "gross_revenue_amount")
    private BigDecimal grossRevenueAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "net_revenue_amount")
    private BigDecimal netRevenueAmount;

    @Column(name = "platform_fee_amount")
    private BigDecimal platformFeeAmount;

    @Column(name = "transaction_fee_amount")
    private BigDecimal transactionFeeAmount;

    @Column(name = "shipping_fee_amount")
    private BigDecimal shippingFeeAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
