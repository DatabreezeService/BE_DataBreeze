package databreeze.entity;

import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import databreeze.enums.WorkspaceStatus;
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
@Table(name = "mapping_templates")
public class MappingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "target_schema_id")
    private UUID targetSchemaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private SourcePlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source_type", nullable = false, length = 50)
    private DataSourceType dataSourceType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "is_system_template", nullable = false)
    @Builder.Default
    private Boolean isSystemTemplate = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
