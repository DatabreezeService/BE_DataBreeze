package databreeze.entity;

import databreeze.enums.CommercePlatform;
import databreeze.enums.WorkspaceStatus;
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
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private CommercePlatform platform;

    @Column(name = "external_store_id", length = 255)
    private String externalStoreId;

    @Column(name = "country_code", nullable = false, length = 10)
    @Builder.Default
    private String countryCode = "VN";

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

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
