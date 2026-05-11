package databreeze.entity;

import databreeze.enums.SourcePlatform;
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
@Table(name = "revenue_daily")
public class RevenueDaily {

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

    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private Long orderCount = 0L;

    @Column(name = "item_quantity", nullable = false)
    @Builder.Default
    private Long itemQuantity = 0L;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

}
