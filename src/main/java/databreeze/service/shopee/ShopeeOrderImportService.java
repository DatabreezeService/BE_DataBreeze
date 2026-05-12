package databreeze.service.shopee;

import databreeze.dto.shopee.ShopeeImportResult;
import databreeze.entity.ImportColumnMapping;
import databreeze.entity.RawImportRow;
import databreeze.entity.Upload;

import java.util.List;

/**
 * Service xử lý nghiệp vụ import Shopee Order VN.
 * Google Ads/TikTok Shop sẽ tạo service riêng sau, không nhét chung vào đây.
 */
public interface ShopeeOrderImportService {
    /**
     * Normalize raw_import_rows theo mapping đã confirm rồi ghi vào orders/order_items/products.
     */
    ShopeeImportResult importOrders(Upload upload, List<RawImportRow> rawRows, List<ImportColumnMapping> mappings, boolean skipInvalidRows);
}
