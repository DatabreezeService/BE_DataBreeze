package com.databreeze.entity;

import com.databreeze.enums.SourcePlatform;
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
@Table(name = "campaign_profit_daily")
public class CampaignProfitDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "ad_campaign_id")
    private UUID adCampaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private SourcePlatform platform;

    @Column(name = "profit_date", nullable = false)
    private LocalDate profitDate;

    @Column(name = "spend_amount")
    private BigDecimal spendAmount;

    @Column(name = "attributed_revenue_amount")
    private BigDecimal attributedRevenueAmount;

    @Column(name = "estimated_cogs_amount")
    private BigDecimal estimatedCogsAmount;

    @Column(name = "attributed_profit_amount")
    private BigDecimal attributedProfitAmount;

    @Column(name = "roas")
    private BigDecimal roas;

    @Column(name = "roi")
    private BigDecimal roi;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions", nullable = false)
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

}
