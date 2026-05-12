package databreeze.entity;

import databreeze.enums.SourcePlatform;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "profit_by_sku_daily")
public class ProfitBySkuDaily {

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

    @Column(name = "profit_date", nullable = false)
    private LocalDate profitDate;

    @Column(name = "gross_revenue_amount")
    private BigDecimal grossRevenueAmount;

    @Column(name = "net_revenue_amount")
    private BigDecimal netRevenueAmount;

    @Column(name = "cogs_amount")
    private BigDecimal cogsAmount;

    @Column(name = "allocated_platform_fee_amount")
    private BigDecimal allocatedPlatformFeeAmount;

    @Column(name = "allocated_ad_spend_amount")
    private BigDecimal allocatedAdSpendAmount;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "gross_profit_amount")
    private BigDecimal grossProfitAmount;

    @Column(name = "net_profit_amount")
    private BigDecimal netProfitAmount;

    @Column(name = "profit_margin")
    private BigDecimal profitMargin;

    @Column(name = "quantity_sold", nullable = false)
    @Builder.Default
    private Long quantitySold = 0L;

    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private Long orderCount = 0L;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

}
