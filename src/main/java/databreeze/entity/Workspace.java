package databreeze.entity;

import databreeze.entity.enums.WorkspaceStatus;
import databreeze.entity.enums.WorkspaceType;
import jakarta.persistence.*;
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
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_type", nullable = false, length = 50)
    @Builder.Default
    private WorkspaceType workspaceType = WorkspaceType.PERSONAL;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "country_code", nullable = false, length = 10)
    @Builder.Default
    private String countryCode = "VN";

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "timezone", nullable = false, length = 100)
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "tax_code", length = 100)
    private String taxCode;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
