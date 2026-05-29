# DataBreeze BE - User Flow APIs

File nay note cac API user-flow moi bo sung de FE co the dung app end-to-end sau khi import Shopee.

## ETL history

- `GET /api/v1/workspaces/{workspaceId}/etl/uploads?storeId=&limit=`
- `GET /api/v1/workspaces/{workspaceId}/etl/uploads/{uploadId}`
- `GET /api/v1/workspaces/{workspaceId}/etl/jobs?uploadId=&limit=`
- `GET /api/v1/workspaces/{workspaceId}/etl/uploads/{uploadId}/jobs?limit=`

## Dashboard Shopee

- `GET /api/v1/workspaces/{workspaceId}/dashboard/shopee?storeId=&fromDate=&toDate=&topProductLimit=`
- `GET /api/v1/workspaces/{workspaceId}/dashboard/shopee/summary?storeId=&fromDate=&toDate=`
- `GET /api/v1/workspaces/{workspaceId}/dashboard/shopee/daily?storeId=&fromDate=&toDate=`
- `GET /api/v1/workspaces/{workspaceId}/dashboard/shopee/top-products?storeId=&fromDate=&toDate=&limit=`
- `POST /api/v1/workspaces/{workspaceId}/dashboard/shopee/recalculate`

Body recalculate:

```json
{
  "storeId": "uuid",
  "fromDate": "2026-05-10",
  "toDate": "2026-05-14"
}
```

## Product costs / COGS

- `GET /api/v1/workspaces/{workspaceId}/product-costs?sku=`
- `POST /api/v1/workspaces/{workspaceId}/product-costs`
- `POST /api/v1/workspaces/{workspaceId}/product-costs/bulk`
- `PATCH /api/v1/workspaces/{workspaceId}/product-costs/{costId}`
- `DELETE /api/v1/workspaces/{workspaceId}/product-costs/{costId}`
- `POST /api/v1/workspaces/{workspaceId}/product-costs/apply`
- `GET /api/v1/workspaces/{workspaceId}/product-costs/missing-skus?storeId=&fromDate=&toDate=`

Body tao gia von:

```json
{
  "sku": "SKIN-001",
  "costType": "COGS",
  "unitCost": 45000,
  "currencyCode": "VND",
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
```

Body apply COGS:

```json
{
  "storeId": "uuid",
  "fromDate": "2026-05-10",
  "toDate": "2026-05-14",
  "recalculateDashboard": true
}
```

## Operating expenses

- `GET /api/v1/workspaces/{workspaceId}/operating-expenses?storeId=&fromDate=&toDate=`
- `POST /api/v1/workspaces/{workspaceId}/operating-expenses`
- `PATCH /api/v1/workspaces/{workspaceId}/operating-expenses/{expenseId}`
- `DELETE /api/v1/workspaces/{workspaceId}/operating-expenses/{expenseId}`

Body tao chi phi:

```json
{
  "storeId": "uuid",
  "expenseDate": "2026-05-10",
  "category": "SOFTWARE",
  "description": "Phi phan mem",
  "amount": 50000,
  "currencyCode": "VND",
  "allocationMethod": "MANUAL"
}
```

## FE flow goi y

1. Tao/list store: `GET/POST /stores`.
2. Upload Shopee voi `storeId`.
3. Suggest mapping, confirm mapping, run import.
4. Lay `dashboardUrl`, `minBusinessDate`, `maxBusinessDate` tu response run import.
5. Goi dashboard Shopee de hien KPI/chart/top products.
6. Goi missing-skus de nhap gia von SKU con thieu.
7. Tao product-costs, sau do goi apply COGS.
8. Goi generate insights voi cung `storeId/fromDate/toDate`.
9. Goi list insights co filter `storeId/fromDate/toDate` de tranh hien insight cu.
