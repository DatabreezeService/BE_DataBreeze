# DataBreeze BE Core ETL - Shopee Việt Nam

Bản này giữ đúng cấu trúc project hiện tại:

```txt
src/main/java/databreeze
├── api              # Controller/API layer nhận request từ React/Swagger
├── config           # Env, Swagger/OpenAPI, CORS/JWT sau này
├── dto              # Request/Response DTO, không dùng Entity để trả ra FE
├── entity           # JPA Entity map trực tiếp bảng DB
├── enums            # Enum dùng chung
├── repository       # Lớp giao tiếp DB, extends JpaRepository
├── service          # Business logic, mỗi service có interface riêng
└── DataBreezeApplication.java
```

## 1. Nguyên tắc data transfer

Không transfer dữ liệu thuần qua Entity.

```txt
React/Swagger request
    → DTO Request
    → Controller
    → Service interface
    → Service implementation
    → Repository
    → Entity/DB
    → DTO Response
    → React
```

Entity chỉ dùng ở tầng service/repository để map DB. Controller chỉ nhận/trả DTO.

## 2. DTO đã tách riêng

```txt
dto/common
├── ApiResponse.java
└── ErrorResponse.java

dto/etl
├── UploadFileRequest.java
├── UploadFileResponse.java
├── ParsedRowPreviewDto.java
├── SuggestMappingRequest.java
├── SuggestMappingResponse.java
├── ColumnMappingDto.java
├── ConfirmMappingRequest.java
├── ConfirmMappingResponse.java
├── RunImportRequest.java
├── RunImportResponse.java
└── ImportJobStatusResponse.java

dto/shopee
├── ShopeeNormalizedOrderRow.java
├── ShopeeImportResult.java
└── ShopeeDailyCalculationResult.java
```

## 3. Service interface + implementation

```txt
service/etl
├── EtlImportService.java                  # Điều phối full flow ETL
├── FileParsingService.java                # Đọc Excel/CSV
├── RawRowService.java                     # Lưu/đọc raw_import_rows
├── MappingSuggestionService.java          # Rule/AI mapping
├── TargetSchemaService.java               # Target schema + required fields
└── impl
    ├── EtlImportServiceImpl.java
    ├── FileParsingServiceImpl.java
    ├── RawRowServiceImpl.java
    ├── MappingSuggestionServiceImpl.java
    └── TargetSchemaServiceImpl.java

service/shopee
├── ShopeeOrderImportService.java          # Import nghiệp vụ Shopee Order VN
└── impl/ShopeeOrderImportServiceImpl.java

service/analytics
├── ShopeeAnalyticsService.java            # Tính revenue_daily/profit_daily
└── impl/ShopeeAnalyticsServiceImpl.java

service/ai
├── AiMappingClient.java                   # Interface gọi AI mapping
└── impl/OpenAiMappingClient.java
```

## 4. Giải thích từng service chính

### EtlImportService

#### uploadAndAnalyze(...)
- Nhận file Excel/CSV từ FE.
- Chỉ cho phép `SHOPEE + MARKETPLACE_ORDER` trong MVP.
- Gọi `TargetSchemaService.getActiveSchema()` để lấy schema Shopee VN.
- Gọi `FileParsingService.parse()` để đọc header + rows.
- Lưu file vào local storage.
- Lưu metadata vào `uploads`.
- Tạo `import_jobs`.
- Gọi `RawRowService.saveRawRows()` để lưu dữ liệu gốc vào `raw_import_rows`.
- Trả `UploadFileResponse` cho FE gồm `uploadId`, `importJobId`, `headers`, `sampleRows`.

#### suggestMapping(importJobId, request)
- Load lại file đã upload từ `raw_import_rows`.
- Lấy field chuẩn từ `target_schema_fields`.
- Gọi `MappingSuggestionService.suggest()`.
- Rule Shopee VN chạy trước.
- AI chỉ được gọi nếu `request.useAi=true` và `APP_AI_ENABLED=true`.
- Lưu mapping vào `import_column_mappings` với `userConfirmed=false`.
- Trả `SuggestMappingResponse` để FE hiển thị Schema Change Report.

#### confirmMapping(importJobId, request)
- FE gửi mapping user đã xác nhận/chỉnh sửa.
- Service kiểm tra field bắt buộc như `external_order_id`, `sku`.
- Lưu mapping với `userConfirmed=true`.
- Nếu thiếu field bắt buộc thì trả `missingRequiredFields` để FE chặn import.

#### runImport(importJobId, request)
- Kiểm tra mapping đã confirmed.
- Gọi `ShopeeOrderImportService.importOrders()`.
- Ghi dữ liệu vào `products`, `orders`, `order_items`.
- Gọi `ShopeeAnalyticsService.recalculateDaily()`.
- Cập nhật `import_jobs` thành `COMPLETED` hoặc `FAILED`.

#### getStatus(importJobId)
- Trả trạng thái job cho FE polling.

### FileParsingService

#### parse(file)
- Nhận `MultipartFile`.
- Nếu `.xlsx/.xls` dùng Apache POI.
- Nếu `.csv` dùng Apache Commons CSV.
- Dòng đầu tiên là header.
- Trả `ParsedFile(headers, rows)`.

### RawRowService

#### saveRawRows(...)
- Lưu từng dòng gốc vào `raw_import_rows`.
- Mỗi row giữ nguyên tên cột gốc tiếng Việt từ file.
- Có `rowHash` để audit/debug.

#### toParsedFile(importJobId)
- Dựng lại `ParsedFile` từ raw rows để suggest mapping lại mà không cần đọc file gốc.

### MappingSuggestionService

#### suggest(...)
- Chuẩn hóa tên cột bằng `FieldNormalizer`.
- So với alias trong `MappingRuleBook.SHOPEE_ALIASES`.
- Nếu user bật AI, gọi `AiMappingClient` để map cột khó.

#### persistMappings(...)
- Xóa mapping cũ của job.
- Lưu mapping mới vào `import_column_mappings`.
- Cho phép FE chỉnh mapping nhiều lần trước khi import.

### ShopeeOrderImportService

#### importOrders(...)
- Nhận `Upload`, `RawImportRow`, `ImportColumnMapping`.
- Normalize từng dòng thành `ShopeeNormalizedOrderRow`.
- Validate field bắt buộc: mã đơn hàng, SKU.
- Upsert `orders` theo `workspaceId + platform + externalOrderId`.
- Upsert `products` theo `workspaceId + sku`.
- Replace `order_items` theo order.
- Cập nhật status từng raw row: `IMPORTED` hoặc `INVALID`.

### ShopeeAnalyticsService

#### recalculateDaily(...)
- Xóa dữ liệu `revenue_daily`, `profit_daily` trong khoảng ngày import.
- Tính lại tổng doanh thu/lợi nhuận bằng Java để chạy được cả MySQL và PostgreSQL.

## 5. Swagger test flow

Mở Swagger:

```txt
http://localhost:8080/swagger-ui/index.html
```

Lấy UUID mẫu:

```txt
GET /api/v1/dev/swagger-test-ids
```

Core flow:

```txt
1. POST /api/v1/workspaces/{workspaceId}/etl/uploads
2. POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/suggest-mapping
3. POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/confirm-mapping
4. POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/run
5. GET  /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}
```

## 6. Response sau upload

```json
{
  "uploadId": "uuid",
  "importJobId": "uuid",
  "targetSchemaId": "uuid",
  "totalRows": 120,
  "totalColumns": 18,
  "headers": ["Mã đơn hàng", "Ngày đặt hàng", "SKU phân loại", "Doanh thu sản phẩm"],
  "sampleRows": [
    {
      "rowNumber": 2,
      "values": {
        "Mã đơn hàng": "240101ABC",
        "Ngày đặt hàng": "01/01/2026",
        "SKU phân loại": "SKU-AO-001",
        "Doanh thu sản phẩm": "150000"
      }
    }
  ],
  "nextStep": "Gọi POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/suggest-mapping",
  "message": "Upload file Shopee thành công. Hệ thống đã lưu dữ liệu gốc, chưa import vào dashboard."
}
```

## 7. AI mapping

AI có thể tự gợi ý mapping, nhưng không được tự import thẳng vào DB.

Flow đúng:

```txt
Rule Shopee VN
    → AI bổ sung cột khó nếu bật
    → FE hiển thị mapping
    → User xác nhận
    → BE mới import
```

Local test:

```properties
APP_AI_ENABLED=false
```

Production:

```properties
APP_AI_ENABLED=true
APP_AI_BASE_URL=https://api.openai.com/v1
APP_AI_API_KEY=sk-...
APP_AI_MODEL=gpt-4.1-mini
```

Model đề xuất:
- `gpt-4.1-mini`: hợp lý cho mapping cột tiếng Việt, chi phí thấp.
- `gpt-4.1`: dùng khi file rất lộn xộn hoặc cần độ chính xác cao hơn.

Không gọi AI theo từng row. Chỉ gửi header, tối đa 5 dòng mẫu và target fields.

## 8. Feature sau core Shopee

### Phase 2 - Dashboard doanh thu
- API `revenue_daily`.
- API doanh thu theo SKU.
- Filter theo ngày/store/platform.
- Tổng đơn, tổng item, refund, phí sàn.

### Phase 3 - Profit dashboard
- Import giá vốn sản phẩm.
- Import chi phí vận hành.
- Tính COGS.
- Tính gross profit/net profit/margin.
- SKU lời/lỗ.

### Phase 4 - Insight AI
- SKU doanh thu cao nhưng margin thấp.
- SKU refund nhiều.
- Doanh thu tăng nhưng lợi nhuận giảm.
- Thiếu giá vốn.
- Phí sàn bất thường.

### Phase 5 - Ads
- Google Ads import.
- Facebook Ads import.
- TikTok Ads import.
- Shopee Ads import.
- ROAS/ROI theo campaign.

### Phase 6 - Workspace/Auth/Billing
- Firebase/JWT hoặc Spring Security JWT.
- Workspace member/RBAC.
- Subscription/usage limit.
- Audit log.


---

## Update: quyền tài khoản cá nhân và workspace chung

Bản này đã bổ sung kiểm tra quyền cho 2 luồng sử dụng:

- Personal workspace: dữ liệu cá nhân của một user, chỉ owner thao tác.
- Organization workspace: dữ liệu chung của team/business, kiểm tra qua `workspace_members`.

Xem chi tiết trong `WORKSPACE_FLOW_GUIDE.md`.

Swagger local không còn trả UUID random. API `GET /api/v1/dev/swagger-test-ids` tạo/lấy user, workspace và store thật trong DB để test core ETL.

MVP chưa có JWT nên các API ETL nhận `actorUserId` bằng request param. Khi setup JWT, thay phần này bằng userId lấy từ token.
