package databreeze.entity;

import databreeze.enums.TargetDataType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "target_schema_fields")
public class TargetSchemaField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "target_schema_id", nullable = false)
    private UUID targetSchemaId;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private TargetDataType dataType;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "is_unique_key", nullable = false)
    @Builder.Default
    private Boolean isUniqueKey = false;

    @Column(name = "is_amount_field", nullable = false)
    @Builder.Default
    private Boolean isAmountField = false;

    @Column(name = "is_date_field", nullable = false)
    @Builder.Default
    private Boolean isDateField = false;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "format_hint", length = 255)
    private String formatHint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enum_values", columnDefinition = "json")
    private Map<String, Object> enumValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rule", columnDefinition = "json")
    private Map<String, Object> validationRule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transform_rule", columnDefinition = "json")
    private Map<String, Object> transformRule;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
