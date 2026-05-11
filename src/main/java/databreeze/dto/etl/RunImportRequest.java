package databreeze.dto.etl;

/**
 * Dự phòng cho phase: user có thể chọn skip dòng lỗi hoặc dừng khi gặp lỗi.
 */
public record RunImportRequest(
        Boolean skipInvalidRows
) {
    public boolean shouldSkipInvalidRows() { return !Boolean.FALSE.equals(skipInvalidRows); }
}
