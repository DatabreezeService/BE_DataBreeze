package databreeze.entity;

import databreeze.enums.ConnectionType;
import databreeze.enums.DataSourceStatus;
import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
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
@Table(name = "data_sources")
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private SourcePlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source_type", nullable = false, length = 50)
    private DataSourceType dataSourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 50)
    @Builder.Default
    private ConnectionType connectionType = ConnectionType.FILE_UPLOAD;

    @Column(name = "default_target_schema_id")
    private UUID defaultTargetSchemaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DataSourceStatus status = DataSourceStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
