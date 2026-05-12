package databreeze.entity;

import databreeze.enums.MappingSource;
import databreeze.enums.TargetDataType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_column_mappings")
public class ImportColumnMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "import_job_id", nullable = false)
    private UUID importJobId;

    @Column(name = "target_schema_field_id")
    private UUID targetSchemaFieldId;

    @Column(name = "source_column_name", nullable = false, length = 255)
    private String sourceColumnName;

    @Column(name = "target_field_name", nullable = false, length = 255)
    private String targetFieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_data_type", nullable = false, length = 50)
    private TargetDataType targetDataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_source", nullable = false, length = 50)
    private MappingSource mappingSource;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "user_confirmed", nullable = false)
    @Builder.Default
    private Boolean userConfirmed = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transform_rule", columnDefinition = "json")
    private Map<String, Object> transformRule;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
