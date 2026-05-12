package databreeze.service.etl;

import databreeze.entity.RawImportRow;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý raw_import_rows.
 * Raw row là lớp audit/debug để có thể xem lại dữ liệu gốc và lỗi từng dòng.
 */
public interface RawRowService {
    /** Lưu toàn bộ dòng gốc sau upload. */
    void saveRawRows(UUID workspaceId, UUID uploadId, UUID importJobId, ParsedFile parsedFile);

    /** Load lại raw rows theo đúng thứ tự dòng trong file. */
    List<RawImportRow> findRows(UUID importJobId);

    /** Tạo ParsedFile từ raw_import_rows để phục vụ suggest mapping lại mà không cần đọc file gốc. */
    ParsedFile toParsedFile(UUID importJobId);
}
