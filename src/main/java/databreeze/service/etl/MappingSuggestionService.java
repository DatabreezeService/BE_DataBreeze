package databreeze.service.etl;

import databreeze.dto.etl.ColumnMappingDto;
import databreeze.entity.TargetSchemaField;
import databreeze.enums.MappingSource;

import java.util.List;
import java.util.UUID;

/**
 * Service gợi ý và lưu mapping cột.
 *
 * Nguyên tắc:
 * - Rule Shopee VN chạy trước.
 * - AI chỉ bổ sung mapping nếu useAi=true và app bật AI.
 * - Không cho AI ghi thẳng vào bảng nghiệp vụ.
 * - User phải confirm mapping trước khi import.
 */
public interface MappingSuggestionService {

    /**
     * Gợi ý mapping từ cột file Excel/CSV sang target schema field.
     *
     * @param file         File đã parse gồm headers + sampleRows + rows.
     * @param targetFields Danh sách field chuẩn trong target schema.
     * @param useAi        true nếu muốn gọi AI bổ sung mapping.
     * @return Danh sách mapping gợi ý để FE hiển thị cho user xác nhận.
     */
    List<ColumnMappingDto> suggest(
            ParsedFile file,
            List<TargetSchemaField> targetFields,
            boolean useAi
    );

    /**
     * Lưu mapping vào bảng import_column_mappings.
     *
     * @param importJobId  ID của import job.
     * @param mappings     Danh sách mapping cần lưu.
     * @param source       RULE / AI / USER / SYSTEM.
     * @param confirmed    true nếu user đã confirm mapping.
     * @param targetFields Danh sách field chuẩn để lấy targetSchemaFieldId.
     */
    void persistMappings(
            UUID importJobId,
            List<ColumnMappingDto> mappings,
            MappingSource source,
            boolean confirmed,
            List<TargetSchemaField> targetFields
    );
}