package databreeze.dto.etl;

import databreeze.enums.TargetDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnMappingDto {
    private UUID targetSchemaFieldId;

    @NotBlank(message = "Tên cột nguồn không được để trống.")
    private String sourceColumnName;

    @NotBlank(message = "Field đích không được để trống.")
    private String targetFieldName;

    private String targetDisplayName;

    @NotNull(message = "Kiểu dữ liệu đích là bắt buộc.")
    private TargetDataType targetDataType;

    private Boolean required;
    private BigDecimal confidenceScore;
    private String reason;
    private Map<String, Object> transformRule;
    private Boolean userConfirmed;
}
