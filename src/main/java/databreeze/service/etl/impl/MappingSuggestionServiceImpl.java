package databreeze.service.etl.impl;

import databreeze.dto.etl.ColumnMappingDto;
import databreeze.entity.ImportColumnMapping;
import databreeze.entity.TargetSchemaField;
import databreeze.enums.MappingSource;
import databreeze.repository.ImportColumnMappingRepository;
import databreeze.service.ai.AiMappingClient;
import databreeze.service.etl.MappingRuleBook;
import databreeze.service.etl.MappingSuggestionService;
import databreeze.service.etl.ParsedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý gợi ý mapping cột cho ETL.
 *
 * Core hiện tại tập trung Shopee VN:
 * - Map "Mã đơn hàng" -> external_order_id
 * - Map "Mã SKU" / "SKU phân loại" -> sku
 * - Map "Tên sản phẩm" -> product_name
 * - Map "Số lượng" -> quantity
 * - Map các cột doanh thu/phí/hoàn tiền tương ứng
 */
@Service
public class MappingSuggestionServiceImpl implements MappingSuggestionService {

    @Autowired
    private MappingRuleBook mappingRuleBook;

    @Autowired
    private AiMappingClient aiMappingClient;

    @Autowired
    private ImportColumnMappingRepository importColumnMappingRepository;

    /**
     * Gợi ý mapping.
     *
     * Flow:
     * 1. Rule-based mapping chạy trước để xử lý tốt Shopee VN.
     * 2. Nếu useAi=true thì gọi AI để bổ sung những cột rule chưa nhận ra.
     * 3. Merge mapping theo targetFieldName để tránh trùng field.
     * 4. Sort theo displayOrder của target schema để FE hiển thị dễ nhìn.
     */
    @Override
    public List<ColumnMappingDto> suggest(
            ParsedFile file,
            List<TargetSchemaField> targetFields,
            boolean useAi
    ) {
        if (file == null || file.getHeaders() == null || file.getHeaders().isEmpty()) {
            return List.of();
        }

        if (targetFields == null || targetFields.isEmpty()) {
            return List.of();
        }

        List<ColumnMappingDto> ruleMappings = buildRuleMappings(file, targetFields);

        if (!useAi) {
            return sortByTargetSchemaOrder(ruleMappings, targetFields);
        }

        List<ColumnMappingDto> aiMappings = aiMappingClient.suggestMappings(file, targetFields);

        List<ColumnMappingDto> mergedMappings = mergeMappings(
                ruleMappings,
                aiMappings,
                targetFields
        );

        return sortByTargetSchemaOrder(mergedMappings, targetFields);
    }

    /**
     * Lưu mapping xuống bảng import_column_mappings.
     *
     * Lưu ý:
     * - Nếu confirmed=false: mapping chỉ là gợi ý.
     * - Nếu confirmed=true: mapping do user xác nhận, có thể dùng để import.
     */
    @Override
    public void persistMappings(
            UUID importJobId,
            List<ColumnMappingDto> mappings,
            MappingSource source,
            boolean confirmed,
            List<TargetSchemaField> targetFields
    ) {
        if (importJobId == null) {
            throw new IllegalArgumentException("Thiếu importJobId khi lưu mapping.");
        }

        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        Map<String, TargetSchemaField> targetFieldMap = buildTargetFieldMap(targetFields);
        List<ImportColumnMapping> entities = new ArrayList<>();

        for (ColumnMappingDto dto : mappings) {
            if (dto == null) {
                continue;
            }

            if (isBlank(dto.getSourceColumnName()) || isBlank(dto.getTargetFieldName())) {
                continue;
            }

            TargetSchemaField targetField = targetFieldMap.get(dto.getTargetFieldName());

            ImportColumnMapping entity = new ImportColumnMapping();
            entity.setId(UUID.randomUUID());
            entity.setImportJobId(importJobId);
            entity.setSourceColumnName(dto.getSourceColumnName());
            entity.setTargetFieldName(dto.getTargetFieldName());

            if (targetField != null) {
                entity.setTargetSchemaFieldId(targetField.getId());
            }

            entity.setMappingSource(source);
            entity.setConfidenceScore(safeConfidence(dto.getConfidenceScore()));
            entity.setReason(dto.getReason());
            entity.setUserConfirmed(confirmed || Boolean.TRUE.equals(dto.getUserConfirmed()));
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());

            entities.add(entity);
        }

        if (!entities.isEmpty()) {
            importColumnMappingRepository.saveAll(entities);
        }
    }

    /**
     * Build mapping bằng rule Shopee VN.
     *
     * Điểm quan trọng:
     * - Không so sánh exact text thô.
     * - MappingRuleBook phải normalize header tiếng Việt trước.
     * - "Mã SKU" và "SKU phân loại" đều phải map về sku.
     */
    private List<ColumnMappingDto> buildRuleMappings(
            ParsedFile file,
            List<TargetSchemaField> targetFields
    ) {
        Map<String, TargetSchemaField> targetFieldMap = buildTargetFieldMap(targetFields);

        List<ColumnMappingDto> result = new ArrayList<>();
        Set<String> usedTargetFields = new HashSet<>();

        for (String header : file.getHeaders()) {
            Optional<String> matchedTarget = mappingRuleBook.findTargetFieldForShopeeOrder(header);

            if (matchedTarget.isEmpty()) {
                continue;
            }

            String targetFieldName = matchedTarget.get();

            if (usedTargetFields.contains(targetFieldName)) {
                continue;
            }

            TargetSchemaField targetField = targetFieldMap.get(targetFieldName);

            if (targetField == null) {
                continue;
            }

            ColumnMappingDto dto = new ColumnMappingDto();
            dto.setSourceColumnName(header);
            dto.setTargetFieldName(targetField.getFieldName());
            dto.setTargetDisplayName(targetField.getDisplayName());
            dto.setTargetDataType(targetField.getDataType());
            dto.setRequired(Boolean.TRUE.equals(targetField.getIsRequired()));
            dto.setConfidenceScore(BigDecimal.valueOf(mappingRuleBook.confidenceFor(header, targetFieldName)));
            dto.setReason(mappingRuleBook.reasonFor(header, targetFieldName));
            dto.setUserConfirmed(false);

            result.add(dto);
            usedTargetFields.add(targetFieldName);
        }

        return result;
    }

    /**
     * Merge rule mapping và AI mapping.
     *
     * Rule được ưu tiên hơn AI vì:
     * - Rule Shopee VN ổn định.
     * - AI chỉ nên bổ sung cột khó/khác format.
     * - Không để AI override mapping đã chắc chắn.
     */
    private List<ColumnMappingDto> mergeMappings(
            List<ColumnMappingDto> ruleMappings,
            List<ColumnMappingDto> aiMappings,
            List<TargetSchemaField> targetFields
    ) {
        Map<String, ColumnMappingDto> byTargetField = new LinkedHashMap<>();

        if (ruleMappings != null) {
            for (ColumnMappingDto rule : ruleMappings) {
                if (!isBlank(rule.getTargetFieldName())) {
                    byTargetField.put(rule.getTargetFieldName(), rule);
                }
            }
        }

        if (aiMappings == null || aiMappings.isEmpty()) {
            return new ArrayList<>(byTargetField.values());
        }

        Map<String, TargetSchemaField> targetFieldMap = buildTargetFieldMap(targetFields);

        for (ColumnMappingDto ai : aiMappings) {
            if (ai == null) {
                continue;
            }

            if (isBlank(ai.getSourceColumnName()) || isBlank(ai.getTargetFieldName())) {
                continue;
            }

            if (byTargetField.containsKey(ai.getTargetFieldName())) {
                continue;
            }

            TargetSchemaField target = targetFieldMap.get(ai.getTargetFieldName());

            if (target == null) {
                continue;
            }

            ai.setTargetDisplayName(target.getDisplayName());
            ai.setTargetDataType(target.getDataType());
            ai.setRequired(Boolean.TRUE.equals(target.getIsRequired()));
            ai.setUserConfirmed(false);

            if (ai.getConfidenceScore() == null) {
                ai.setConfidenceScore(BigDecimal.valueOf(0.75));
            }

            if (isBlank(ai.getReason())) {
                ai.setReason("AI gợi ý mapping dựa trên header và dữ liệu mẫu.");
            }

            byTargetField.put(ai.getTargetFieldName(), ai);
        }

        return new ArrayList<>(byTargetField.values());
    }

    /**
     * Sort mapping theo thứ tự field trong target schema.
     * FE hiển thị sẽ dễ hiểu hơn.
     */
    private List<ColumnMappingDto> sortByTargetSchemaOrder(
            List<ColumnMappingDto> mappings,
            List<TargetSchemaField> targetFields
    ) {
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> orderMap = new HashMap<>();

        for (TargetSchemaField field : targetFields) {
            Integer order = field.getDisplayOrder();
            orderMap.put(field.getFieldName(), order == null ? 9999 : order);
        }

        mappings.sort(Comparator.comparingInt(
                item -> orderMap.getOrDefault(item.getTargetFieldName(), 9999)
        ));

        return mappings;
    }

    private Map<String, TargetSchemaField> buildTargetFieldMap(
            List<TargetSchemaField> targetFields
    ) {
        Map<String, TargetSchemaField> map = new HashMap<>();

        if (targetFields == null) {
            return map;
        }

        for (TargetSchemaField field : targetFields) {
            if (field.getFieldName() != null) {
                map.put(field.getFieldName(), field);
            }
        }

        return map;
    }

    private BigDecimal safeConfidence(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }

        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}