package databreeze.service.etl;

import databreeze.dto.etl.*;
import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Service tổng điều phối luồng ETL từ FE:
 * upload file -> gợi ý mapping -> xác nhận mapping -> import -> xem trạng thái.
 *
 * MVP chưa có JWT nên mọi hàm nhận actorUserId để kiểm tra quyền workspace.
 * Sau khi có JWT, actorUserId sẽ lấy từ SecurityContext thay vì request param.
 */
public interface EtlImportService {

    /**
     * Bước 1: nhận Excel/CSV, kiểm tra quyền ghi workspace, parse header + sample rows,
     * lưu uploads/import_jobs/raw_import_rows. Chưa ghi vào orders/order_items/products.
     */
    UploadFileResponse uploadAndAnalyze(
            UUID workspaceId,
            UUID actorUserId,
            UUID storeId,
            SourcePlatform platform,
            DataSourceType dataSourceType,
            MultipartFile file
    ) throws IOException;

    /**
     * Bước 2: tạo gợi ý mapping từ cột file sang target_schema_fields.
     * Rule Shopee VN chạy trước, AI chỉ hỗ trợ khi được bật.
     */
    SuggestMappingResponse suggestMapping(UUID workspaceId, UUID actorUserId, UUID importJobId, SuggestMappingRequest request);

    /**
     * Bước 3: lưu mapping user đã xác nhận/chỉnh sửa.
     * Nếu thiếu field bắt buộc thì trả danh sách missingRequiredFields để FE chặn nút import.
     */
    ConfirmMappingResponse confirmMapping(UUID workspaceId, UUID actorUserId, UUID importJobId, ConfirmMappingRequest request);

    /**
     * Bước 4: import dữ liệu vào bảng nghiệp vụ Shopee.
     */
    RunImportResponse runImport(UUID workspaceId, UUID actorUserId, UUID importJobId, RunImportRequest request);

    /**
     * Bước 5: lấy trạng thái job cho FE polling.
     */
    ImportJobStatusResponse getStatus(UUID workspaceId, UUID actorUserId, UUID importJobId);

    List<UploadListItemResponse> listUploads(UUID workspaceId, UUID actorUserId, UUID storeId, int limit);

    UploadListItemResponse getUpload(UUID workspaceId, UUID actorUserId, UUID uploadId);

    List<ImportJobListItemResponse> listJobs(UUID workspaceId, UUID actorUserId, UUID uploadId, int limit);

    Path getErrorReport(UUID workspaceId, UUID actorUserId, UUID importJobId);
}
