package databreeze.service.etl;

import databreeze.dto.etl.ColumnMappingDto;
import databreeze.entity.TargetSchema;
import databreeze.entity.TargetSchemaField;
import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý schema chuẩn của từng loại nguồn dữ liệu.
 */
public interface TargetSchemaService {
    /** Seed schema mặc định cho Swagger/local test. */
    void seedDefaultSchemas();

    /** Lấy schema đang active theo platform + data source type. */
    TargetSchema getActiveSchema(SourcePlatform platform, DataSourceType dataSourceType);

    /** Lấy danh sách field của target schema. */
    List<TargetSchemaField> getActiveFields(UUID targetSchemaId);

    /** Kiểm tra các field bắt buộc có được mapping chưa. */
    List<String> findMissingRequiredFields(List<ColumnMappingDto> mappings, List<TargetSchemaField> targetFields);
}
