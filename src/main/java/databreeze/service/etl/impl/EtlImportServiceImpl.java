package databreeze.service.etl.impl;

import databreeze.dto.etl.*;
import databreeze.dto.shopee.ShopeeImportResult;
import databreeze.entity.*;
import databreeze.enums.*;
import databreeze.repository.ImportColumnMappingRepository;
import databreeze.repository.ImportJobRepository;
import databreeze.repository.UploadRepository;
import databreeze.service.analytics.ShopeeAnalyticsService;
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
    private MappingSuggestionService mappingSuggestionService;

    @Autowired
    private ShopeeOrderImportService shopeeOrderImportService;

    @Autowired
    private ShopeeAnalyticsService shopeeAnalyticsService;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

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

        List<ColumnMappingDto> mappings = mappingSuggestionService.suggest(parsedFile, targetFields, useAi);
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
            shopeeAnalyticsService.recalculateDaily(upload.getWorkspaceId(), upload.getStoreId(), result.getMinBusinessDate(), result.getMaxBusinessDate());

            job.setStatus(ImportJobStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setTotalRows(result.getTotalRows());
            job.setSuccessRows(result.getSuccessRows());
            job.setFailedRows(result.getFailedRows());
            job.setWarningRows(result.getWarningRows());
            job.setErrorMessage(null);
            importJobRepository.save(job);

            return new RunImportResponse(
                    importJobId,
                    job.getStatus(),
                    result.getTotalRows(),
                    result.getSuccessRows(),
                    result.getFailedRows(),
                    result.getWarningRows(),
                    result.getCreatedOrders(),
                    result.getCreatedOrderItems(),
                    result.getCreatedProducts(),
                    "FE có thể gọi API dashboard revenue/profit sau khi build phase tiếp theo.",
                    "Import Shopee hoàn tất. Dữ liệu đã được ghi vào orders/order_items/products và tổng hợp daily."
            );
        } catch (Exception ex) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setFailedAt(OffsetDateTime.now());
            job.setErrorMessage(ex.getMessage());
            importJobRepository.save(job);
            throw ex;
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
                job.getErrorMessage()
        );
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
}
