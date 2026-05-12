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
public class UploadFileResponse {
    private UUID uploadId;
    private UUID importJobId;
    private UUID targetSchemaId;
    private long totalRows;
    private int totalColumns;
    private List<String> headers;
    private List<ParsedRowPreviewDto> sampleRows;
    private String nextStep;
    private String message;
}
