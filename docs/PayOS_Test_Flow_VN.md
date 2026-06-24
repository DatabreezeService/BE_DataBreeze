# DataBreeze BE - PayOS Flow va Cach Test API

File nay mo ta luong thanh toan PayOS theo code hien tai trong BE, thu tu goi API trong mot giao dich thuc te, cong dung cua tung API, va cac mau request de test nhanh bang Swagger hoac Postman.

## Dieu kien truoc khi test

- `app.payos.enabled=true`
- Da cau hinh:
  - `app.payos.client-id`
  - `app.payos.api-key`
  - `app.payos.checksum-key`
- App dang chay va mo Swagger tai `/swagger-ui/index.html`
- Cac API sau can bearer token:
  - `/api/v1/payos/checkout/**`
  - `/api/v1/payos/order/**`
  - `/api/v1/payos/payouts/**`
- Webhook PayOS dang de public:
  - `POST /api/v1/payos/webhook`
  - `POST /api/v1/payos/payos_transfer_handler`

## Luong thanh toan thuc te

### Luong 1: Tao link thanh toan va redirect nguoi dung

1. FE hoac BE goi `POST /api/v1/payos/order/create` de tao payment link.
2. He thong nhan ve `checkoutUrl` tu PayOS.
3. FE redirect nguoi dung sang `checkoutUrl`, hoac goi `POST /api/v1/payos/checkout/redirect` neu muon backend tra ve `302`.
4. Nguoi dung thanh toan tren trang PayOS.
5. PayOS goi webhook ve `POST /api/v1/payos/webhook` hoac `POST /api/v1/payos/payos_transfer_handler`.
6. He thong verify webhook bang `payOS.webhooks().verify(body)`.
7. Sau khi thanh toan, he thong co the goi `GET /api/v1/payos/order/{orderId}` de kiem tra lai trang thai.

### Luong 2: Xac nhan webhook URL voi PayOS

1. Backend expose webhook URL public.
2. Goi `POST /api/v1/payos/order/confirm-webhook` de dang ky webhook URL voi PayOS.
3. Sau buoc nay, PayOS moi day ket qua thanh toan ve dung endpoint webhook.

### Luong 3: Tra cuu, huy don, va tai hoa don

1. Goi `GET /api/v1/payos/order/{orderId}` de xem thong tin payment link.
2. Goi `PUT /api/v1/payos/order/{orderId}` de huy payment link neu khach chua thanh toan.
3. Goi `GET /api/v1/payos/order/{orderId}/invoices` de lay thong tin hoa don.
4. Goi `GET /api/v1/payos/order/{orderId}/invoices/{invoiceId}/download` de tai file hoa don.

## Cong dung tung API

### 1. `POST /api/v1/payos/order/create`

Cong dung:
- Tao payment link theo kieu API JSON.
- Dung khi FE muon tu lay `checkoutUrl` roi tu dieu huong.

Request body:

```json
{
  "orderCode": 20260613001,
  "productName": "Goi Premium",
  "description": "Thanh toan goi Premium thang 6",
  "returnUrl": "http://localhost:3000/payment/success",
  "cancelUrl": "http://localhost:3000/payment/cancel",
  "price": 10000,
  "quantity": 1
}
```

Ket qua:
- Tra ve `ApiResponse<CreatePaymentLinkResponse>`
- Du lieu quan trong nhat de test la `checkoutUrl`

### 2. `POST /api/v1/payos/checkout/redirect`

Cong dung:
- Tao payment link va backend redirect thang sang trang PayOS.
- Hop voi flow web server-side hon la mobile app hoac SPA muon tu xu ly URL.

Request body:

```json
{
  "orderCode": 20260613002,
  "productName": "Goi Premium",
  "description": "Redirect den trang thanh toan",
  "returnUrl": "http://localhost:3000/payment/success",
  "cancelUrl": "http://localhost:3000/payment/cancel",
  "price": 10000,
  "quantity": 1
}
```

Ket qua:
- HTTP `302 FOUND`
- Header `Location` tro den `checkoutUrl`

### 3. `GET /api/v1/payos/order/{orderId}`

Cong dung:
- Tra cuu trang thai payment link theo `orderId`
- Dung sau khi thanh toan hoac khi can doi soat

### 4. `PUT /api/v1/payos/order/{orderId}`

Cong dung:
- Huy payment link
- Code hien tai huy voi ly do mac dinh: `change my mind`

### 5. `POST /api/v1/payos/order/confirm-webhook`

Cong dung:
- Dang ky webhook URL voi PayOS
- Thuong chi can goi mot vai lan khi setup moi moi truong

Request body:

```json
{
  "webhookUrl": "https://your-domain.com/api/v1/payos/webhook"
}
```

### 6. `POST /api/v1/payos/webhook`

Cong dung:
- Endpoint public de PayOS push ket qua thanh toan
- Backend verify checksum/signature bang `payOS.webhooks().verify(body)`

Luu y:
- Endpoint nay khong dung bearer token
- Endpoint nay phai public de PayOS goi vao duoc

### 7. `POST /api/v1/payos/payos_transfer_handler`

Cong dung:
- Alias webhook thu hai
- Hanh vi giong `/api/v1/payos/webhook`

### 8. `GET /api/v1/payos/order/{orderId}/invoices`

Cong dung:
- Lay thong tin hoa don lien quan den don thanh toan

### 9. `GET /api/v1/payos/order/{orderId}/invoices/{invoiceId}/download`

Cong dung:
- Tai file hoa don
- Response co the la PDF hoac content-type theo du lieu PayOS tra ve

### 10. `POST /api/v1/payos/payouts/create`

Cong dung:
- Tao payout don le
- Thuong dung cho nghiep vu chuyen tien ra tai khoan thay vi thu tien tu khach

### 11. `POST /api/v1/payos/payouts/batch/create`

Cong dung:
- Tao payout theo lo

### 12. `GET /api/v1/payos/payouts/{payoutId}`

Cong dung:
- Tra cuu chi tiet payout

### 13. `GET /api/v1/payos/payouts/list`

Cong dung:
- Liet ke payout theo bo loc

### 14. `GET /api/v1/payos/payouts/balance`

Cong dung:
- Xem so du tai khoan payout

## Thu tu test de xac nhan flow thanh toan

### Cach test nhanh nhat trong Swagger

1. Authorize bearer token.
2. Goi `POST /api/v1/payos/order/confirm-webhook` voi webhook public cua moi truong.
3. Goi `POST /api/v1/payos/order/create`.
4. Copy `checkoutUrl` tra ve va mo tren trinh duyet.
5. Thuc hien thanh toan tren PayOS sandbox hoac moi truong test.
6. Kiem tra webhook co ve app hay khong.
7. Goi `GET /api/v1/payos/order/{orderId}` de doi chieu trang thai.
8. Neu PayOS co hoa don, goi `GET /api/v1/payos/order/{orderId}/invoices`.
9. Goi tiep API download invoice neu can.

## Bo request mau de test

### Tao payment link

```json
{
  "orderCode": 20260613003,
  "productName": "DataBreeze Pro Monthly",
  "description": "Thanh toan goi DataBreeze Pro",
  "returnUrl": "http://localhost:3000/billing/payos-success",
  "cancelUrl": "http://localhost:3000/billing/payos-cancel",
  "price": 10000,
  "quantity": 1
}
```

### Confirm webhook

```json
{
  "webhookUrl": "https://your-domain.com/api/v1/payos/webhook"
}
```

### Mau test webhook

Khong nen tu fake webhook bang tay neu muon test verify that su, vi `payOS.webhooks().verify(body)` can dung payload co checksum/signature hop le tu PayOS.

Cach test dung:
- Tao payment that su trong sandbox
- De PayOS goi webhook ve endpoint public cua app

## Ghi chu quan trong

- `orderCode` nen la duy nhat cho moi giao dich de tranh xung dot doi soat.
- `POST /api/v1/payos/checkout/redirect` va `POST /api/v1/payos/order/create` deu tao payment link, nhung phuc vu 2 kieu tich hop khac nhau.
- Webhook la nguon su kien bat dong bo, nhung khi can chot trang thai cuoi cung thi nen doi chieu lai bang `GET /api/v1/payos/order/{orderId}`.
- Neu webhook khong ve duoc local, can dung domain public hoac tunnel nhu ngrok/cloudflared.
