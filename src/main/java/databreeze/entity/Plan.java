package databreeze.entity;

import databreeze.enums.BillingCycle;
import databreeze.enums.PlanStatus;
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
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 50)
    private BillingCycle billingCycle;

    @Column(name = "price_amount")
    private BigDecimal priceAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "max_stores")
    private Integer maxStores;

    @Column(name = "max_uploads_per_month")
    private Integer maxUploadsPerMonth;

    @Column(name = "max_rows_per_month")
    private Long maxRowsPerMonth;

    @Column(name = "max_file_size_mb")
    private Integer maxFileSizeMb;

    @Column(name = "allow_ai_mapping", nullable = false)
    @Builder.Default
    private Boolean allowAiMapping = true;

    @Column(name = "allow_profit_dashboard", nullable = false)
    @Builder.Default
    private Boolean allowProfitDashboard = false;

    @Column(name = "allow_insights", nullable = false)
    @Builder.Default
    private Boolean allowInsights = false;

    @Column(name = "allow_priority_support", nullable = false)
    @Builder.Default
    private Boolean allowPrioritySupport = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PlanStatus status = PlanStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
