package databreeze.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestMappingResponse {
    private UUID importJobId;
    private UUID targetSchemaId;
    private String platform;
    private String dataSourceType;
    private List<ColumnMappingDto> mappings;
    private List<String> unmappedColumns;
    private List<String> missingRequiredFields;
    private String source;
    private String nextStep;
    private String message;
}
