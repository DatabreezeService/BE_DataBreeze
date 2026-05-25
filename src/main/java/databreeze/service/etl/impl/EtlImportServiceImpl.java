package databreeze.service.etl.impl;

import databreeze.dto.etl.*;
import databreeze.dto.shopee.ShopeeImportResult;
import databreeze.entity.*;
import databreeze.enums.*;
import databreeze.repository.ImportColumnMappingRepository;
import databreeze.repository.ImportJobRepository;
import databreeze.repository.UploadRepository;
import databreeze.service.analytics.ShopeeAnalyticsService;
import databreeze.service.billing.UsageMeterService;
import databreeze.service.etl.*;
import databreeze.service.shopee.ShopeeOrderImportService;
import databreeze.service.workspace.WorkspaceAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Implementation tổng điều phối core ETL.
 * Constructor injection rõ ràng: repository giao tiếp DB, service xử lý nghiệp vụ.
 */
@Service
public class EtlImportServiceImpl implements EtlImportService {
    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private ImportColumnMappingRepository mappingRepository;

    @Autowired
    private TargetSchemaService targetSchemaService;

    @Autowired
    private FileParsingService fileParsingService;

    @Autowired
    private RawRowService rawRowService;

    @Autowired
    private ImportErrorReportService importErrorReportService;

    @Autowired
    private MappingSuggestionService mappingSuggestionService;

    @Autowired
    private ShopeeOrderImportService shopeeOrderImportService;

    @Autowired
    private ShopeeAnalyticsService shopeeAnalyticsService;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private UsageMeterService usageMeterService;

    @Value("${app.storage.local-dir:./storage/uploads}")
    private String storageDir;

    @Override
    @Transactional
    public UploadFileResponse uploadAndAnalyze(UUID workspaceId,
                                               UUID actorUserId,
                                               UUID storeId,
                                               SourcePlatform platform,
                                               DataSourceType dataSourceType,
                                               MultipartFile file) throws IOException {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);
        validateSupportedSource(platform, dataSourceType);
        Path uploadDir = Paths.get(storageDir);
        Files.createDirectories(uploadDir);

        TargetSchema schema = targetSchemaService.getActiveSchema(platform, dataSourceType);
        ParsedFile parsedFile = fileParsingService.parse(file);
        if (parsedFile.getHeaders().isEmpty()) {
            throw new IllegalArgumentException("File không có header. Vui lòng kiểm tra dòng đầu tiên của file Excel/CSV.");
        }

        usageMeterService.recordUpload(workspaceId, actorUserId, file.getSize());

        String safeFileName = safeFileName(file.getOriginalFilename());
        String storageKey = UUID.randomUUID() + "-" + safeFileName;
        Files.copy(file.getInputStream(), uploadDir.resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);

        Upload upload = uploadRepository.save(Upload.builder()
                .workspaceId(workspaceId)
                .storeId(storeId)
                .targetSchemaId(schema.getId())
                .uploadedBy(actorUserId)
                .dataSourceType(dataSourceType)
                .platform(platform)
                .originalFileName(safeFileName)
                .storageKey(storageKey)
                .fileMimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .detectedEncoding("UTF-8")
                .totalRows((long) parsedFile.getRows().size())
                .totalColumns(parsedFile.getHeaders().size())
                .status(UploadStatus.MAPPING_REQUIRED)
                .build());

        ImportJob job = importJobRepository.save(ImportJob.builder()
                .workspaceId(workspaceId)
                .uploadId(upload.getId())
                .targetSchemaId(schema.getId())
                .jobType(ImportJobType.SUGGEST_MAPPING)
                .status(ImportJobStatus.WAITING_FOR_MAPPING)
                .totalRows((long) parsedFile.getRows().size())
                .build());

        rawRowService.saveRawRows(workspaceId, upload.getId(), job.getId(), parsedFile);

        List<ParsedRowPreviewDto> previewRows = new ArrayList<>();
        long rowNumber = 2;
        for (Map<String, Object> row : parsedFile.getRows().stream().limit(5).toList()) {
            previewRows.add(new ParsedRowPreviewDto(rowNumber++, row));
        }

        return new UploadFileResponse(
                upload.getId(),
                job.getId(),
                schema.getId(),
                parsedFile.getRows().size(),
                parsedFile.getHeaders().size(),
                parsedFile.getHeaders(),
                previewRows,
                "Gọi POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/suggest-mapping",
                "Upload file Shopee thành công. Hệ thống đã lưu dữ liệu gốc, chưa import vào dashboard."
        );
    }

    @Override
    @Transactional
    public SuggestMappingResponse suggestMapping(UUID workspaceId, UUID actorUserId, UUID importJobId, SuggestMappingRequest request) {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        ImportJob job = getJob(importJobId);
        requireJobInWorkspace(job, workspaceId);
        ParsedFile parsedFile = rawRowService.toParsedFile(importJobId);
        List<TargetSchemaField> targetFields = targetSchemaService.getActiveFields(job.getTargetSchemaId());
        boolean useAi = request != null && request.shouldUseAi();
        if (useAi) {
            usageMeterService.ensureAiMappingAvailable(workspaceId, actorUserId);
        }

        MappingSuggestionResult suggestionResult = mappingSuggestionService.suggest(parsedFile, targetFields, useAi);
        if (useAi && suggestionResult.isAiCalled()) {
            usageMeterService.recordAiMapping(
                    workspaceId,
                    actorUserId,
                    suggestionResult.getTokenUsage().getInputTokens(),
                    suggestionResult.getTokenUsage().getOutputTokens()
            );
        }
        List<ColumnMappingDto> mappings = suggestionResult.getMappings();
        mappingSuggestionService.persistMappings(importJobId, mappings, MappingSource.AI, false, targetFields);

        List<String> mappedColumns = mappings.stream().map(ColumnMappingDto::getSourceColumnName).toList();
        List<String> unmappedColumns = parsedFile.getHeaders().stream()
                .filter(header -> !mappedColumns.contains(header))
                .toList();
        List<String> missingRequiredFields = targetSchemaService.findMissingRequiredFields(mappings, targetFields);

        job.setStatus(ImportJobStatus.WAITING_FOR_MAPPING);
        importJobRepository.save(job);

        return new SuggestMappingResponse(
                importJobId,
                job.getTargetSchemaId(),
                SourcePlatform.SHOPEE.name(),
                DataSourceType.MARKETPLACE_ORDER.name(),
                mappings,
                unmappedColumns,
                missingRequiredFields,
                useAi ? "RULE_SHOPEE_VN_PLUS_AI" : "RULE_SHOPEE_VN",
                suggestionResult.getTokenUsage().getInputTokens(),
                suggestionResult.getTokenUsage().getOutputTokens(),
                suggestionResult.getTokenUsage().getTotalTokens(),
                "FE hiển thị mapping cho user xác nhận, sau đó gọi confirm-mapping.",
                missingRequiredFields.isEmpty()
                        ? "Đã gợi ý mapping. Vui lòng kiểm tra và xác nhận trước khi import."
                        : "Mapping còn thiếu field bắt buộc. FE cần yêu cầu user chỉnh trước khi import."
        );
    }

    @Override
    @Transactional
    public ConfirmMappingResponse confirmMapping(UUID workspaceId, UUID actorUserId, UUID importJobId, ConfirmMappingRequest request) {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        ImportJob job = getJob(importJobId);
        requireJobInWorkspace(job, workspaceId);
        List<TargetSchemaField> targetFields = targetSchemaService.getActiveFields(job.getTargetSchemaId());
        List<String> missingRequiredFields = targetSchemaService.findMissingRequiredFields(request.getMappings(), targetFields);

        mappingSuggestionService.persistMappings(importJobId, request.getMappings(), MappingSource.USER, true, targetFields);
        job.setStatus(missingRequiredFields.isEmpty() ? ImportJobStatus.VALIDATING : ImportJobStatus.WAITING_FOR_MAPPING);
        importJobRepository.save(job);

        return new ConfirmMappingResponse(
                importJobId,
                missingRequiredFields.isEmpty(),
                request.getMappings().size(),
                missingRequiredFields,
                missingRequiredFields.isEmpty()
                        ? "Gọi POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/run"
                        : "Bổ sung mapping field bắt buộc rồi confirm lại.",
                missingRequiredFields.isEmpty()
                        ? "Mapping đã được xác nhận. Có thể chạy import."
                        : "Mapping chưa đủ field bắt buộc. Chưa nên chạy import."
        );
    }

    @Override
    @Transactional
    public RunImportResponse runImport(UUID workspaceId, UUID actorUserId, UUID importJobId, RunImportRequest request) {
        workspaceAccessService.requireImportAccess(workspaceId, actorUserId);
        ImportJob job = getJob(importJobId);
        requireJobInWorkspace(job, workspaceId);
        Upload upload = uploadRepository.findById(job.getUploadId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy upload tương ứng với import job."));
        validateSupportedSource(upload.getPlatform(), upload.getDataSourceType());

        List<ImportColumnMapping> mappings = mappingRepository.findByImportJobId(importJobId);
        if (mappings.stream().noneMatch(mapping -> Boolean.TRUE.equals(mapping.getUserConfirmed()))) {
            throw new IllegalStateException("Bạn cần xác nhận mapping trước khi chạy import.");
        }

        job.setStatus(ImportJobStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        importJobRepository.save(job);

        try {
            boolean skipInvalidRows = request == null || request.shouldSkipInvalidRows();
            ShopeeImportResult result = shopeeOrderImportService.importOrders(upload, rawRowService.findRows(importJobId), mappings, skipInvalidRows);
            usageMeterService.recordImportedRows(workspaceId, actorUserId, result.getSuccessRows());
            var dailyResult = shopeeAnalyticsService.recalculateDaily(upload.getWorkspaceId(), upload.getStoreId(), result.getMinBusinessDate(), result.getMaxBusinessDate());

            job.setStatus(ImportJobStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setTotalRows(result.getTotalRows());
            job.setSuccessRows(result.getSuccessRows());
            job.setFailedRows(result.getFailedRows());
            job.setWarningRows(result.getWarningRows());
            job.setErrorMessage(null);
            attachErrorReport(job);
            importJobRepository.save(job);

            return RunImportResponse.builder()
                    .importJobId(importJobId)
                    .storeId(upload.getStoreId())
                    .status(job.getStatus())
                    .totalRows(result.getTotalRows())
                    .successRows(result.getSuccessRows())
                    .failedRows(result.getFailedRows())
                    .warningRows(result.getWarningRows())
                    .createdOrders(result.getCreatedOrders())
                    .createdOrderItems(result.getCreatedOrderItems())
                    .createdProducts(result.getCreatedProducts())
                    .minBusinessDate(result.getMinBusinessDate())
                    .maxBusinessDate(result.getMaxBusinessDate())
                    .revenueDailyRows(dailyResult.getRevenueDailyRows())
                    .profitDailyRows(dailyResult.getProfitDailyRows())
                    .errorReportDownloadUrl(errorReportUrl(workspaceId, job))
                    .dashboardUrl(dashboardUrl(workspaceId, upload.getStoreId(), result.getMinBusinessDate(), result.getMaxBusinessDate()))
                    .processedDataUrl(processedDataUrl(workspaceId, upload.getStoreId(), result.getMinBusinessDate(), result.getMaxBusinessDate()))
                    .insightGenerateUrl("/api/v1/workspaces/" + workspaceId + "/insights/generate")
                    .nextStep("FE goi dashboardUrl de hien KPI/chart/top products, goi insightGenerateUrl de tao insight tu cung storeId va range ngay.")
                    .message("Import Shopee hoan tat. Du lieu da ghi vao orders/order_items/products va tong hop daily neu co ngay kinh doanh.")
                    .build();
        } catch (Exception ex) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setFailedAt(OffsetDateTime.now());
            job.setErrorMessage(ex.getMessage());
            job.setFailedRows(Math.max(job.getFailedRows(), 1L));
            attachErrorReport(job);
            importJobRepository.save(job);
            return new RunImportResponse(
                    importJobId,
                    job.getStatus(),
                    job.getTotalRows(),
                    job.getSuccessRows(),
                    job.getFailedRows(),
                    job.getWarningRows(),
                    0,
                    0,
                    0,
                    errorReportUrl(workspaceId, job),
                    "Tải file Excel lỗi, sửa dữ liệu nguồn rồi chạy lại import.",
                    "Import thất bại. Hệ thống đã ghi lý do lỗi theo từng dòng nếu có thể."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ImportJobStatusResponse getStatus(UUID workspaceId, UUID actorUserId, UUID importJobId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        ImportJob job = getJob(importJobId);
        requireJobInWorkspace(job, workspaceId);
        return new ImportJobStatusResponse(
                job.getId(),
                job.getUploadId(),
                job.getTargetSchemaId(),
                job.getStatus(),
                job.getTotalRows(),
                job.getSuccessRows(),
                job.getFailedRows(),
                job.getWarningRows(),
                job.getErrorMessage(),
                errorReportUrl(workspaceId, job)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UploadListItemResponse> listUploads(UUID workspaceId, UUID actorUserId, UUID storeId, int limit) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        workspaceAccessService.requireStoreBelongsToWorkspace(workspaceId, storeId);
        int safeLimit = safeLimit(limit);
        List<Upload> uploads = storeId == null
                ? uploadRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                : uploadRepository.findByWorkspaceIdAndStoreIdOrderByCreatedAtDesc(workspaceId, storeId);
        return uploads.stream().limit(safeLimit).map(this::toUploadListItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UploadListItemResponse getUpload(UUID workspaceId, UUID actorUserId, UUID uploadId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay upload."));
        if (!workspaceId.equals(upload.getWorkspaceId())) {
            throw new SecurityException("Upload khong thuoc workspace hien tai.");
        }
        return toUploadListItem(upload);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportJobListItemResponse> listJobs(UUID workspaceId, UUID actorUserId, UUID uploadId, int limit) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        int safeLimit = safeLimit(limit);
        List<ImportJob> jobs = uploadId == null
                ? importJobRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                : importJobRepository.findByWorkspaceIdAndUploadIdOrderByCreatedAtDesc(workspaceId, uploadId);
        return jobs.stream().limit(safeLimit).map(job -> toImportJobListItem(workspaceId, job)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Path getErrorReport(UUID workspaceId, UUID actorUserId, UUID importJobId) {
        return importErrorReportService.resolveReport(workspaceId, actorUserId, importJobId);
    }

    private void requireJobInWorkspace(ImportJob job, UUID workspaceId) {
        if (!workspaceId.equals(job.getWorkspaceId())) {
            throw new SecurityException("Import job không thuộc workspace hiện tại. Vui lòng kiểm tra workspaceId/importJobId.");
        }
    }

    private void validateSupportedSource(SourcePlatform platform, DataSourceType dataSourceType) {
        if (platform != SourcePlatform.SHOPEE || dataSourceType != DataSourceType.MARKETPLACE_ORDER) {
            throw new IllegalArgumentException("MVP hiện chỉ hỗ trợ import file đơn hàng Shopee Việt Nam. Google Ads, TikTok Shop và các nguồn khác sẽ làm ở phase sau.");
        }
    }

    private ImportJob getJob(UUID importJobId) {
        return importJobRepository.findById(importJobId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy import job. Vui lòng kiểm tra importJobId."));
    }

    private String safeFileName(String originalFileName) {
        String fallback = "shopee-upload.xlsx";
        String name = originalFileName == null || originalFileName.isBlank() ? fallback : originalFileName;
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void attachErrorReport(ImportJob job) {
        try {
            importErrorReportService.generateReport(job).ifPresent(storageKey -> {
                job.setErrorReportStorageKey(storageKey);
                job.setErrorReportGeneratedAt(OffsetDateTime.now());
            });
        } catch (IOException ex) {
            job.setErrorMessage((job.getErrorMessage() == null ? "" : job.getErrorMessage() + " | ")
                    + "Không thể tạo file Excel lỗi: " + ex.getMessage());
        }
    }

    private String errorReportUrl(UUID workspaceId, ImportJob job) {
        if (job.getErrorReportStorageKey() == null || job.getErrorReportStorageKey().isBlank()) {
            return null;
        }
        return "/api/v1/workspaces/" + workspaceId + "/etl/jobs/" + job.getId() + "/error-report";
    }

    private String dashboardUrl(UUID workspaceId, UUID storeId, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        return "/api/v1/workspaces/" + workspaceId + "/dashboard/shopee" + dateAndStoreQuery(storeId, fromDate, toDate);
    }

    private String processedDataUrl(UUID workspaceId, UUID storeId, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        return "/api/v1/workspaces/" + workspaceId + "/processed-data/shopee" + dateAndStoreQuery(storeId, fromDate, toDate);
    }

    private String dateAndStoreQuery(UUID storeId, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        List<String> params = new ArrayList<>();
        if (storeId != null) {
            params.add("storeId=" + storeId);
        }
        if (fromDate != null) {
            params.add("fromDate=" + fromDate);
        }
        if (toDate != null) {
            params.add("toDate=" + toDate);
        }
        return params.isEmpty() ? "" : "?" + String.join("&", params);
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }

    private UploadListItemResponse toUploadListItem(Upload upload) {
        return UploadListItemResponse.builder()
                .id(upload.getId())
                .workspaceId(upload.getWorkspaceId())
                .storeId(upload.getStoreId())
                .uploadedBy(upload.getUploadedBy())
                .type(upload.getDataSourceType())
                .platform(upload.getPlatform())
                .originalFilename(upload.getOriginalFileName())
                .fileSizeBytes(upload.getFileSizeBytes())
                .totalRows(upload.getTotalRows())
                .totalColumns(upload.getTotalColumns() == null ? null : upload.getTotalColumns().longValue())
                .status(upload.getStatus())
                .createdAt(upload.getCreatedAt())
                .updatedAt(upload.getUpdatedAt())
                .build();
    }

    private ImportJobListItemResponse toImportJobListItem(UUID workspaceId, ImportJob job) {
        return ImportJobListItemResponse.builder()
                .id(job.getId())
                .workspaceId(job.getWorkspaceId())
                .uploadId(job.getUploadId())
                .targetSchemaId(job.getTargetSchemaId())
                .status(job.getStatus())
                .jobType(job.getJobType())
                .totalRows(job.getTotalRows())
                .successRows(job.getSuccessRows())
                .failedRows(job.getFailedRows())
                .warningRows(job.getWarningRows())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .failedAt(job.getFailedAt())
                .errorReportDownloadUrl(errorReportUrl(workspaceId, job))
                .build();
    }

}
