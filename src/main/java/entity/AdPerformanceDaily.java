package com.databreeze.entity;

import com.databreeze.enums.AdPlatform;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "ad_performance_daily")
public class AdPerformanceDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "ad_account_id")
    private UUID adAccountId;

    @Column(name = "ad_campaign_id")
    private UUID adCampaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private AdPlatform platform;

    @Column(name = "performance_date", nullable = false)
    private LocalDate performanceDate;

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions", nullable = false)
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "spend_amount")
    private BigDecimal spendAmount;

    @Column(name = "attributed_revenue_amount")
    private BigDecimal attributedRevenueAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "source_upload_id")
    private UUID sourceUploadId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
