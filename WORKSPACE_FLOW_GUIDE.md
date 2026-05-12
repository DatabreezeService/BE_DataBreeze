# DataBreeze - Logic tài khoản cá nhân và workspace chung

## Kết luận sau khi kiểm tra

Bản trước đã có entity `users`, `workspaces`, `workspace_members`, nhưng core ETL chưa kiểm tra quyền theo user/workspace. API chỉ nhận `workspaceId` và `uploadedBy` dạng UUID ngẫu nhiên, nên còn thiếu logic tài khoản cá nhân thật sự.

Bản này đã bổ sung 2 luồng:

1. **Tài khoản cá nhân**
   - `workspaces.workspace_type = PERSONAL`
   - `workspaces.owner_user_id = actorUserId`
   - Chỉ chính owner được upload/import/xem dữ liệu.
   - Dữ liệu vẫn lưu theo `workspace_id`, nhưng workspace này đại diện cho tài khoản cá nhân.

2. **Workspace chung / team / business**
   - `workspaces.workspace_type = ORGANIZATION`
   - User phải có record active trong `workspace_members`.
   - Quyền ghi ETL: `OWNER`, `ADMIN`, `MEMBER`, `ACCOUNTANT`.
   - `VIEWER` chỉ nên xem dashboard/status, không upload/import.

## Package mới thêm

```txt
src/main/java/databreeze
├── dto
│   ├── dev
│   │   └── SwaggerTestIdsResponse.java
│   └── workspace
│       └── WorkspaceContextDto.java
├── repository
│   ├── UserRepository.java
│   ├── WorkspaceRepository.java
│   ├── WorkspaceMemberRepository.java
│   └── StoreRepository.java
└── service
    ├── dev
    │   ├── DevTestDataService.java
    │   └── impl/DevTestDataServiceImpl.java
    └── workspace
        ├── WorkspaceAccessService.java
        ├── WorkspaceBootstrapService.java
        └── impl
            ├── WorkspaceAccessServiceImpl.java
            └── WorkspaceBootstrapServiceImpl.java
```

## Service mới

### WorkspaceAccessService

Dùng để chặn truy cập sai workspace.

- `requireWriteAccess(workspaceId, actorUserId)`
  - Kiểm tra user có quyền upload/import/mapping không.
  - Personal workspace: chỉ owner được ghi.
  - Organization workspace: user phải là active member và role không phải `VIEWER`.

- `requireReadAccess(workspaceId, actorUserId)`
  - Kiểm tra user có quyền xem job/status/dashboard không.
  - Personal workspace: chỉ owner được xem.
  - Organization workspace: member active được xem.

- `requireStoreBelongsToWorkspace(workspaceId, storeId)`
  - Chặn FE gửi storeId của workspace khác.

- `describeContext(workspaceId, actorUserId)`
  - Trả DTO mô tả context đang là personal hay organization.

### WorkspaceBootstrapService

Dùng sau đăng ký/đăng nhập để tạo workspace mặc định.

- `getOrCreatePersonalWorkspace(user)`
  - Tạo personal workspace mặc định cho user nếu chưa có.

- `getOrCreateOrganizationWorkspace(owner, organizationName)`
  - Tạo workspace chung demo/business.

- `ensureOwnerMembership(workspaceId, ownerUserId)`
  - Đảm bảo owner cũng có record trong `workspace_members`.

### DevTestDataService

Chỉ chạy ở profile `local`.

- Tạo/lấy user demo.
- Tạo/lấy personal workspace.
- Tạo/lấy organization workspace.
- Tạo/lấy Shopee store cho từng workspace.
- Trả ID thật trong DB để test Swagger.

## API test Swagger

Lấy dữ liệu test:

```txt
GET /api/v1/dev/swagger-test-ids
```

Response gồm:

```json
{
  "actorUserId": "...",
  "personalWorkspaceId": "...",
  "organizationWorkspaceId": "...",
  "personalStoreId": "...",
  "organizationStoreId": "...",
  "platform": "SHOPEE",
  "dataSourceType": "MARKETPLACE_ORDER"
}
```

## Luồng tài khoản cá nhân

```txt
POST /api/v1/workspaces/{personalWorkspaceId}/etl/uploads?actorUserId={actorUserId}&storeId={personalStoreId}
POST /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}/suggest-mapping?actorUserId={actorUserId}
POST /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}/confirm-mapping?actorUserId={actorUserId}
POST /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}/run?actorUserId={actorUserId}
GET  /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}?actorUserId={actorUserId}
```

## Luồng workspace chung

```txt
POST /api/v1/workspaces/{organizationWorkspaceId}/etl/uploads?actorUserId={actorUserId}&storeId={organizationStoreId}
POST /api/v1/workspaces/{organizationWorkspaceId}/etl/jobs/{importJobId}/suggest-mapping?actorUserId={actorUserId}
POST /api/v1/workspaces/{organizationWorkspaceId}/etl/jobs/{importJobId}/confirm-mapping?actorUserId={actorUserId}
POST /api/v1/workspaces/{organizationWorkspaceId}/etl/jobs/{importJobId}/run?actorUserId={actorUserId}
GET  /api/v1/workspaces/{organizationWorkspaceId}/etl/jobs/{importJobId}?actorUserId={actorUserId}
```

## Logic bảo vệ dữ liệu

1. Upload phải kiểm tra `workspaceId + actorUserId` trước khi lưu file.
2. Nếu có `storeId`, store phải thuộc workspace đó.
3. Các bước suggest/confirm/run/status đều kiểm tra import job thuộc đúng workspace path.
4. Không được dùng `importJobId` của workspace A trong path workspace B.
5. Sau này JWT xong thì bỏ `actorUserId` khỏi request param và lấy từ `SecurityContext`.

