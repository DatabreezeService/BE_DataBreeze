package databreeze.dto.etl;

import java.util.List;
import java.util.UUID;

/**
 *
 * Response sau khi upload file. Chưa import vào orders/order_items
 */
public record UploadFileResponse(
        UUID uploadId,
        UUID importJobId,
        UUID targetSchemaId,
        long totalRows,
        int totalColumns,
        List<String> headers,
        List<ParsedRowPreviewDto> sampleRows,
        String nextStep,
        String message
) {
}
