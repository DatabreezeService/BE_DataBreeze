package databreeze.entity;

import databreeze.enums.AdEntityStatus;
import databreeze.enums.AdPlatform;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ad_campaigns")
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "ad_account_id")
    private UUID adAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private AdPlatform platform;

    @Column(name = "external_campaign_id", length = 255)
    private String externalCampaignId;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AdEntityStatus status = AdEntityStatus.UNKNOWN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
