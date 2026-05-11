package databreeze.entity;

import databreeze.entity.enums.SourcePlatform;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "revenue_by_sku_daily")
public class RevenueBySkuDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "product_id")
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private SourcePlatform platform;

    @Column(name = "sku", nullable = false, length = 255)
    private String sku;

    @Column(name = "revenue_date", nullable = false)
    private LocalDate revenueDate;

    @Column(name = "gross_revenue_amount")
    private BigDecimal grossRevenueAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "net_revenue_amount")
    private BigDecimal netRevenueAmount;

    @Column(name = "quantity_sold", nullable = false)
    @Builder.Default
    private Long quantitySold = 0L;

    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private Long orderCount = 0L;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

}
