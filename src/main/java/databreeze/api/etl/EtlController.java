package databreeze.api.etl;

import databreeze.dto.etl.ConfirmMappingRequest;
import databreeze.dto.etl.ConfirmMappingResponse;
import databreeze.dto.etl.ImportJobListItemResponse;
import databreeze.dto.etl.ImportJobStatusResponse;
import databreeze.dto.etl.RunImportRequest;
import databreeze.dto.etl.RunImportResponse;
import databreeze.dto.etl.SuggestMappingRequest;
import databreeze.dto.etl.SuggestMappingResponse;
import databreeze.dto.etl.UploadFileResponse;
import databreeze.dto.etl.UploadListItemResponse;
import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.etl.EtlImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/etl")
@Tag(name = "ETL Shopee Viet Nam", description = "Core ETL dung chung cho personal workspace va organization workspace.")
@SecurityRequirement(name = "bearer")
public class EtlController {

    @Autowired
    private EtlImportService etlImportService;

    @Value("${app.default-platform:SHOPEE}")
    private SourcePlatform defaultPlatform;

    @Value("${app.default-data-source-type:MARKETPLACE_ORDER}")
    private DataSourceType defaultDataSourceType;

    @GetMapping("/uploads")
    @Operation(summary = "Xem lich su upload")
    public List<UploadListItemResponse> listUploads(@PathVariable UUID workspaceId,
                                                    @AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestParam(required = false) UUID storeId,
                                                    @RequestParam(defaultValue = "50") int limit) {
        return etlImportService.listUploads(workspaceId, CurrentUser.requireUserId(principal), storeId, limit);
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file Shopee VN")
    public UploadFileResponse upload(@PathVariable UUID workspaceId,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @RequestParam(required = false) UUID storeId,
                                     @RequestParam(required = false) SourcePlatform platform,
                                     @RequestParam(required = false) DataSourceType dataSourceType,
                                     @RequestPart("file") MultipartFile file) throws IOException {
        return etlImportService.uploadAndAnalyze(
                workspaceId,
                CurrentUser.requireUserId(principal),
                storeId,
                platform == null ? defaultPlatform : platform,
                dataSourceType == null ? defaultDataSourceType : dataSourceType,
                file
        );
    }

    @GetMapping("/uploads/{uploadId}")
    @Operation(summary = "Xem chi tiet upload")
    public UploadListItemResponse getUpload(@PathVariable UUID workspaceId,
                                            @PathVariable UUID uploadId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return etlImportService.getUpload(workspaceId, CurrentUser.requireUserId(principal), uploadId);
    }

    @GetMapping("/jobs")
    @Operation(summary = "Xem lich su import jobs")
    public List<ImportJobListItemResponse> listJobs(@PathVariable UUID workspaceId,
                                                    @AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestParam(required = false) UUID uploadId,
                                                    @RequestParam(defaultValue = "50") int limit) {
        return etlImportService.listJobs(workspaceId, CurrentUser.requireUserId(principal), uploadId, limit);
    }

    @GetMapping("/uploads/{uploadId}/jobs")
    @Operation(summary = "Xem import jobs cua upload")
    public List<ImportJobListItemResponse> listUploadJobs(@PathVariable UUID workspaceId,
                                                          @PathVariable UUID uploadId,
                                                          @AuthenticationPrincipal UserPrincipal principal,
                                                          @RequestParam(defaultValue = "50") int limit) {
        return etlImportService.listJobs(workspaceId, CurrentUser.requireUserId(principal), uploadId, limit);
    }

    @PostMapping("/jobs/{importJobId}/suggest-mapping")
    @Operation(summary = "Goi y mapping")
    public SuggestMappingResponse suggestMapping(@PathVariable UUID workspaceId,
                                                 @PathVariable UUID importJobId,
                                                 @AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestBody(required = false) SuggestMappingRequest request) {
        return etlImportService.suggestMapping(workspaceId, CurrentUser.requireUserId(principal), importJobId, request == null ? new SuggestMappingRequest(false) : request);
    }

    @PostMapping("/jobs/{importJobId}/confirm-mapping")
    @Operation(summary = "Xac nhan mapping")
    public ConfirmMappingResponse confirmMapping(@PathVariable UUID workspaceId,
                                                 @PathVariable UUID importJobId,
                                                 @AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody ConfirmMappingRequest request) {
        return etlImportService.confirmMapping(workspaceId, CurrentUser.requireUserId(principal), importJobId, request);
    }

    @PostMapping("/jobs/{importJobId}/run")
    @Operation(summary = "Chay import")
    public RunImportResponse runImport(@PathVariable UUID workspaceId,
                                       @PathVariable UUID importJobId,
                                       @AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody(required = false) RunImportRequest request) {
        return etlImportService.runImport(workspaceId, CurrentUser.requireUserId(principal), importJobId, request == null ? new RunImportRequest(true) : request);
    }

    @GetMapping("/jobs/{importJobId}")
    @Operation(summary = "Xem trang thai import job")
    public ImportJobStatusResponse status(@PathVariable UUID workspaceId,
                                          @PathVariable UUID importJobId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return etlImportService.getStatus(workspaceId, CurrentUser.requireUserId(principal), importJobId);
    }

    @GetMapping("/jobs/{importJobId}/error-report")
    @Operation(summary = "Tai file Excel loi tung dong")
    public ResponseEntity<Resource> downloadErrorReport(@PathVariable UUID workspaceId,
                                                        @PathVariable UUID importJobId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        Path reportPath = etlImportService.getErrorReport(workspaceId, CurrentUser.requireUserId(principal), importJobId);
        Resource resource = new FileSystemResource(reportPath);
        String fileName = reportPath.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
