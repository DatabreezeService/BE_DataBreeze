package databreeze.entity;

import databreeze.enums.TargetDataType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mapping_template_fields")
public class MappingTemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "mapping_template_id", nullable = false)
    private UUID mappingTemplateId;

    @Column(name = "target_schema_field_id")
    private UUID targetSchemaFieldId;

    @Column(name = "source_column_name", nullable = false, length = 255)
    private String sourceColumnName;

    @Column(name = "target_field_name", nullable = false, length = 255)
    private String targetFieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_data_type", nullable = false, length = 50)
    private TargetDataType targetDataType;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transform_rule", columnDefinition = "jsonb")
    private Map<String, Object> transformRule;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}
