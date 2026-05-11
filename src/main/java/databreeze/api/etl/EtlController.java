//package databreeze.api.etl;
//
//import databreeze.entity.enums.DataSourceType;
//import databreeze.entity.enums.SourcePlatform;
//import databreeze.service.etl.EtlImportService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/v1/workspaces/{workspaceId}/etl")
//@Tag(name = "ETL Shopee Việt Nam", description = "Tải lên Excel/CSV, gợi ý mapping cột tiếng Việt, xác nhận mapping, import đơn hàng và xem trạng thái xử lý.")
//public class EtlController {
//    private final EtlImportService etlImportService;
//    private final SourcePlatform defaultPlatform;
//    private final DataSourceType defaultDataSourceType;
//
//    public EtlController(EtlImportService etlImportService,
//                         @Value("${app.default-platform:SHOPEE}") SourcePlatform defaultPlatform,
//                         @Value("${app.default-data-source-type:MARKETPLACE_ORDER}") DataSourceType defaultDataSourceType) {
//        this.etlImportService = etlImportService;
//        this.defaultPlatform = defaultPlatform;
//        this.defaultDataSourceType = defaultDataSourceType;
//    }
//
//    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(
//            summary = "Tải lên file Excel/CSV Shopee VN và tạo job import",
//            description = "Đọc header + dữ liệu mẫu, lưu raw rows, chọn target schema cho Shopee Việt Nam và trả về uploadId/importJobId để FE tiếp tục bước mapping."
//    )
//    public UploadResponse upload(@PathVariable UUID workspaceId,
//                                 @RequestParam(required = false) UUID storeId,
//                                 @RequestParam UUID uploadedBy,
//                                 @RequestParam(required = false) SourcePlatform platform,
//                                 @RequestParam(required = false) DataSourceType dataSourceType,
//                                 @RequestPart("file") MultipartFile file) throws IOException {
//        return etlImportService.uploadAndAnalyze(
//                workspaceId,
//                uploadedBy,
//                storeId,
//                platform == null ? defaultPlatform : platform,
//                dataSourceType == null ? defaultDataSourceType : dataSourceType,
//                file
//        );
//    }
//
//    @PostMapping("/jobs/{importJobId}/suggest-mapping")
//    @Operation(
//            summary = "Gợi ý mapping cột",
//            description = "Ưu tiên rule alias Shopee VN trước. Nếu AI_ENABLED=true thì backend gọi AI để xử lý cột tiếng Việt/viết tắt/khác format."
//    )
//    public MappingSuggestionResponse suggestMapping(@PathVariable UUID importJobId) {
//        return etlImportService.suggestMapping(importJobId);
//    }
//
//    @PostMapping("/jobs/{importJobId}/confirm-mapping")
//    @Operation(
//            summary = "Xác nhận mapping",
//            description = "FE gửi lại bảng mapping sau khi user chấp nhận hoặc sửa thủ công. Import chỉ chạy sau bước xác nhận này."
//    )
//    public MappingSuggestionResponse confirmMapping(@PathVariable UUID importJobId,
//                                                    @Valid @RequestBody ConfirmMappingRequest request) {
//        return etlImportService.confirmMapping(importJobId, request);
//    }
//
//    @PostMapping("/jobs/{importJobId}/run")
//    @Operation(
//            summary = "Chạy import dữ liệu",
//            description = "Chuẩn hóa dữ liệu vào orders/order_items/products, sau đó tính lại revenue_daily và profit_daily theo VND/Asia_Ho_Chi_Minh."
//    )
//    public ImportResponse run(@PathVariable UUID importJobId) {
//        return etlImportService.runImport(importJobId);
//    }
//
//    @GetMapping("/jobs/{importJobId}")
//    @Operation(summary = "Xem trạng thái job import")
//    public JobStatusResponse status(@PathVariable UUID importJobId) {
//        return etlImportService.status(importJobId);
//    }
//}
