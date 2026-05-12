package databreeze.api;

import databreeze.dto.etl.*;
import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import databreeze.service.etl.EtlImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/etl")
@Tag(name = "ETL Shopee Việt Nam", description = "Core ETL dùng chung cho personal workspace và organization workspace.")
public class EtlController {

    @Autowired
    private EtlImportService etlImportService;

    @Value("${app.default-platform:SHOPEE}")
    private SourcePlatform defaultPlatform;

    @Value("${app.default-data-source-type:MARKETPLACE_ORDER}")
    private DataSourceType defaultDataSourceType;

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file Shopee VN", description = "Cùng một endpoint cho tài khoản cá nhân và workspace chung. BE check permission IMPORT_DATA theo workspace.")
    public UploadFileResponse upload(@PathVariable UUID workspaceId,
                                     @RequestParam UUID actorUserId,
                                     @RequestParam(required = false) UUID storeId,
                                     @RequestParam(required = false) SourcePlatform platform,
                                     @RequestParam(required = false) DataSourceType dataSourceType,
                                     @RequestPart("file") MultipartFile file) throws IOException {
        return etlImportService.uploadAndAnalyze(
                workspaceId,
                actorUserId,
                storeId,
                platform == null ? defaultPlatform : platform,
                dataSourceType == null ? defaultDataSourceType : dataSourceType,
                file
        );
    }

    @PostMapping("/jobs/{importJobId}/suggest-mapping")
    @Operation(summary = "Gợi ý mapping", description = "Rule Shopee VN chạy trước. Nếu request.useAi=true và APP_AI_ENABLED=true thì gọi AI để bổ sung cột khó.")
    public SuggestMappingResponse suggestMapping(@PathVariable UUID workspaceId,
                                                 @PathVariable UUID importJobId,
                                                 @RequestParam UUID actorUserId,
                                                 @RequestBody(required = false) SuggestMappingRequest request) {
        return etlImportService.suggestMapping(workspaceId, actorUserId, importJobId, request == null ? new SuggestMappingRequest(false) : request);
    }

    @PostMapping("/jobs/{importJobId}/confirm-mapping")
    @Operation(summary = "Xác nhận mapping", description = "FE gửi danh sách mapping user đã duyệt. Nếu thiếu field bắt buộc, response trả missingRequiredFields.")
    public ConfirmMappingResponse confirmMapping(@PathVariable UUID workspaceId,
                                                 @PathVariable UUID importJobId,
                                                 @RequestParam UUID actorUserId,
                                                 @Valid @RequestBody ConfirmMappingRequest request) {
        return etlImportService.confirmMapping(workspaceId, actorUserId, importJobId, request);
    }

    @PostMapping("/jobs/{importJobId}/run")
    @Operation(summary = "Chạy import", description = "Import dữ liệu đã mapping vào products/orders/order_items và tính revenue_daily/profit_daily.")
    public RunImportResponse runImport(@PathVariable UUID workspaceId,
                                       @PathVariable UUID importJobId,
                                       @RequestParam UUID actorUserId,
                                       @RequestBody(required = false) RunImportRequest request) {
        return etlImportService.runImport(workspaceId, actorUserId, importJobId, request == null ? new RunImportRequest(true) : request);
    }

    @GetMapping("/jobs/{importJobId}")
    @Operation(summary = "Xem trạng thái import job")
    public ImportJobStatusResponse status(@PathVariable UUID workspaceId,
                                          @PathVariable UUID importJobId,
                                          @RequestParam UUID actorUserId) {
        return etlImportService.getStatus(workspaceId, actorUserId, importJobId);
    }
}
