package databreeze.service.etl.impl;

import databreeze.dto.etl.ColumnMappingDto;
import databreeze.entity.TargetSchema;
import databreeze.entity.TargetSchemaField;
import databreeze.enums.*;
import databreeze.repository.TargetSchemaFieldRepository;
import databreeze.repository.TargetSchemaRepository;
import databreeze.service.etl.TargetSchemaService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service cấu hình target schema. MVP seed sẵn schema Shopee VN để test Swagger không cần SQL thủ công.
 */
@Service
public class TargetSchemaServiceImpl implements TargetSchemaService, CommandLineRunner {
    @Autowired
    private TargetSchemaRepository schemaRepository;

    @Autowired
    private TargetSchemaFieldRepository fieldRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedDefaultSchemas();
    }

    @Override
    @Transactional
    public void seedDefaultSchemas() {
        seedShopeeOrderRevenue();
    }

    @Override
    @Transactional(readOnly = true)
    public TargetSchema getActiveSchema(SourcePlatform platform, DataSourceType dataSourceType) {
        return schemaRepository.findFirstByPlatformAndDataSourceTypeAndIsActiveTrueOrderByVersionDesc(platform, dataSourceType)
                .orElseThrow(() -> new IllegalStateException("Chưa có target schema đang hoạt động cho " + platform + "/" + dataSourceType));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetSchemaField> getActiveFields(UUID targetSchemaId) {
        return fieldRepository.findByTargetSchemaIdAndIsActiveTrueOrderByDisplayOrderAsc(targetSchemaId);
    }

    @Override
    public List<String> findMissingRequiredFields(List<ColumnMappingDto> mappings, List<TargetSchemaField> targetFields) {
        Set<String> mappedTargetFields = mappings.stream()
                .map(ColumnMappingDto::getTargetFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return targetFields.stream()
                .filter(field -> Boolean.TRUE.equals(field.getIsRequired()))
                .filter(field -> !mappedTargetFields.contains(field.getFieldName()))
                .map(field -> field.getDisplayName() + " (" + field.getFieldName() + ")")
                .toList();
    }

    private void seedShopeeOrderRevenue() {
        TargetSchema schema = schemaRepository.findByCodeAndIsActiveTrue("SHOPEE_ORDER_REVENUE_V1")
                .orElseGet(() -> schemaRepository.save(TargetSchema.builder()
                        .name("Đơn hàng + Doanh thu Shopee Việt Nam")
                        .code("SHOPEE_ORDER_REVENUE_V1")
                        .platform(SourcePlatform.SHOPEE)
                        .dataSourceType(DataSourceType.MARKETPLACE_ORDER)
                        .version(1)
                        .isSystemSchema(true)
                        .isActive(true)
                        .description("Schema MVP cho import đơn hàng, doanh thu và lợi nhuận Shopee Việt Nam")
                        .build()));

        if (!fieldRepository.findByTargetSchemaIdAndIsActiveTrueOrderByDisplayOrderAsc(schema.getId()).isEmpty()) return;

        List<FieldDef> defs = List.of(
                new FieldDef("external_order_id", "Mã đơn hàng", TargetDataType.STRING, true, true, false, false),
                new FieldDef("order_date", "Ngày đặt hàng", TargetDataType.DATE, false, false, false, true),
                new FieldDef("paid_at", "Ngày thanh toán", TargetDataType.DATE, false, false, false, true),
                new FieldDef("order_status", "Trạng thái đơn hàng", TargetDataType.STRING, false, false, false, false),
                new FieldDef("buyer_username", "Tên người mua", TargetDataType.STRING, false, false, false, false),
                new FieldDef("sku", "SKU phân loại", TargetDataType.STRING, true, false, false, false),
                new FieldDef("product_name", "Tên sản phẩm", TargetDataType.STRING, false, false, false, false),
                new FieldDef("quantity", "Số lượng", TargetDataType.INTEGER, false, false, false, false),
                new FieldDef("unit_price", "Đơn giá", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("gross_revenue_amount", "Doanh thu sản phẩm", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("discount_amount", "Giảm giá/voucher", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("refund_amount", "Hoàn tiền", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("shipping_fee_amount", "Phí vận chuyển", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("platform_fee_amount", "Phí sàn", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("transaction_fee_amount", "Phí thanh toán", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("net_revenue_amount", "Doanh thu thuần/thanh toán", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("cogs_amount", "Giá vốn hàng bán", TargetDataType.DECIMAL, false, false, true, false),
                new FieldDef("allocated_ad_spend_amount", "Chi phí Ads phân bổ", TargetDataType.DECIMAL, false, false, true, false)
        );

        int displayOrder = 1;
        for (FieldDef def : defs) {
            fieldRepository.save(TargetSchemaField.builder()
                    .targetSchemaId(schema.getId())
                    .fieldName(def.getName())
                    .displayName(def.getDisplayName())
                    .dataType(def.getType())
                    .isRequired(def.isRequired())
                    .isUniqueKey(def.isUnique())
                    .isAmountField(def.isAmount())
                    .isDateField(def.isDate())
                    .displayOrder(displayOrder++)
                    .isActive(true)
                    .description("Field chuẩn hệ thống dùng cho ETL mapping thị trường Việt Nam")
                    .build());
        }
    }

    @Data
    @AllArgsConstructor
    private static class FieldDef {
        private String name;
        private String displayName;
        private TargetDataType type;
        private boolean required;
        private boolean unique;
        private boolean amount;
        private boolean date;
    }
}
