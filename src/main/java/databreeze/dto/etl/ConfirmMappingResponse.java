package databreeze.dto.etl;

import java.util.List;
import java.util.UUID;

/**
 * Response sau khi lưu mapping đã xác nhận.
 */
public record ConfirmMappingResponse(
        UUID importJobbID,
        boolean confirmed,
        int mappedColumnCount,
        List<String> missingRequiredFields,
        String nextStep,
        String message
 ) {
}
