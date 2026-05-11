package databreeze.dto.etl;

import databreeze.entity.enums.TargetDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * DTO mapping 1 cột file Excel/CSV sang 1 field chuẩn trong target_schema_fields.
 */
public record ColumnMappingDto (
        UUID targetSchemaField,
        @NotBlank(message = "Tên cột nguồn không được để trống.")
        String sourceColumnName,
        @NotBlank(message = "Field đích không được để trống.")
        String targetFieldName,
        String targetDisplayName,
        @NotNull(message = "Kiểu dữ liệu đích là bắt buộc.")
        TargetDataType targetDataType,
        Boolean required,
        BigDecimal confidenceScore,
        String reason,
        Map<String, Object> transformRule,
        Boolean userConfirmed
) {}
