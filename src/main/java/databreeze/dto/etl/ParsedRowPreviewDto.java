package databreeze.dto.etl;

import java.util.Map;

/**
 *
 * Một dòng dữ liệu preview từ Excel/CSV để FE hiện thị trước khi import.
 */
public record ParsedRowPreviewDto(
        long rowNumber,
        Map<String, Object> values
) {}
