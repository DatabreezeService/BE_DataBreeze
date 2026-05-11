package databreeze.dto.etl;

import java.util.List;
import java.util.UUID;

/**
 * Response gợi ý mapping để FE hiển thị Schema Channge Report cho User xác nhận.
 */
public record SuggestMappingResponse(
        UUID importJobId,
        UUID targetSchemaId,
        String platform,
        String dataSourceType,
        List<ColumnMappingDto> mappings,
        List<String> unmappedColumns,
        List<String> missingRequiredFields,
        String source,
        String nextStep,
        String message
) {
}
