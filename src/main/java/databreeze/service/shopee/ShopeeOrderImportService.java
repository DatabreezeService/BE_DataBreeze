package databreeze.service.shopee;

import databreeze.dto.shopee.ShopeeImportResult;
import databreeze.entity.ImportColumnMapping;
import databreeze.entity.RawImportRow;
import databreeze.entity.Upload;

import java.util.List;

public interface ShopeeOrderImportService {
    ShopeeImportResult importOrders(
            Upload upload,
            List<RawImportRow> rawRows,
            List<ImportColumnMapping> mappings,
            boolean skipInvalidRows
    );
}
