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
@Table(name = "profit_daily")
public class ProfitDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private SourcePlatform platform;

    @Column(name = "profit_date", nullable = false)
    private LocalDate profitDate;

    @Column(name = "gross_revenue_amount")
    private BigDecimal grossRevenueAmount;

    @Column(name = "net_revenue_amount")
    private BigDecimal netRevenueAmount;

    @Column(name = "cogs_amount")
    private BigDecimal cogsAmount;

    @Column(name = "platform_fee_amount")
    private BigDecimal platformFeeAmount;

    @Column(name = "transaction_fee_amount")
    private BigDecimal transactionFeeAmount;

    @Column(name = "shipping_fee_amount")
    private BigDecimal shippingFeeAmount;

    @Column(name = "ad_spend_amount")
    private BigDecimal adSpendAmount;

    @Column(name = "operating_expense_amount")
    private BigDecimal operatingExpenseAmount;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "gross_profit_amount")
    private BigDecimal grossProfitAmount;

    @Column(name = "net_profit_amount")
    private BigDecimal netProfitAmount;

    @Column(name = "profit_margin")
    private BigDecimal profitMargin;

    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private Long orderCount = 0L;

    @Column(name = "item_quantity", nullable = false)
    @Builder.Default
    private Long itemQuantity = 0L;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

}
