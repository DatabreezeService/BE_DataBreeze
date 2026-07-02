# DataBreeze Backend Agent Guide

This guide is for agents working inside `BE_DataBreeze`. Read it before changing backend code, API behavior, ETL logic, billing logic, or persistence.

Before making product or architecture decisions, also read `docs/PRODUCT_MEMORY_AND_LONG_TERM_STACK.md`. That file preserves the long-term product memory, stack decision, user model, module priority, and build sequence for DataBreeze.

## Backend Purpose

`BE_DataBreeze` is the active backend for DataBreeze. It is a Spring Boot API that currently focuses on Shopee Vietnam import workflows and business analytics. The backend owns authentication, workspace access, file ingestion, mapping, import jobs, processed commerce data, dashboards, costs, expenses, subscriptions, admin operations, and early payment integration.

The backend should be treated as a modular monolith. Do not split services out unless there is a measurable need.

## Current Stack

Current implementation:

- Java 17 in `pom.xml`.
- Spring Boot.
- Spring MVC/Web.
- Spring Security.
- Spring Data JPA.
- PostgreSQL default configuration.
- Apache POI for XLS/XLSX parsing.
- Apache Commons CSV for CSV parsing.
- Lombok.
- Springdoc/OpenAPI.
- Thymeleaf email templates.
- Local filesystem upload storage.

Long-term target:

- Java 21.
- Spring Boot modular monolith.
- PostgreSQL as source of truth.
- S3-compatible object storage for files.
- Redis for support concerns only.
- Spring Batch when import jobs need chunking, retry, restartability, and long-running job control.
- Flyway or Liquibase migrations.

See `../../docs/LONG_TERM_STACK.md` for the workspace-level stack rationale, and `LONG_TERM_STACK.md` in this folder for backend-specific stack notes.

## Package Map

```text
src/main/java/databreeze/
|-- api/          # REST controllers and exception handler
|-- config/       # Security, OpenAPI, Jackson, mail/rest/payment config
|-- dto/          # Request/response objects; controllers should use these
|-- entity/       # JPA entities
|-- enums/        # Active enum package used by the application
|-- repository/   # Spring Data repositories
|-- security/     # JWT filter, principal helpers, admin checks
|-- service/      # Business service interfaces
`-- service/*/impl/
```

The project also contains `src/main/java/databreeze/entity/enums`. This appears to be a stale duplicate of `databreeze.enums`. New code should use `databreeze.enums` unless the project is intentionally refactored.

The project contains both `dto/shopee` and `dto/shoppee`. The correctly spelled and actively imported package is `dto/shopee`.

## High-Level Data Flow

```text
Frontend / Swagger request
  -> DTO request
  -> Controller
  -> Service interface
  -> Service implementation
  -> Repository
  -> Entity / PostgreSQL
  -> DTO response
  -> Frontend
```

Controllers should not expose JPA entities directly.

## Core Domain Concepts

### User

Stored in `User`. Users can authenticate through email/password or Google-style login. JWTs identify the current user.

### Workspace

Stored in `Workspace`. Workspaces are the tenant boundary. A user may have a personal workspace and may join organization workspaces.

### Workspace Member

Stored in `WorkspaceMember`. Organization permissions come from membership role. Personal workspace access is owner-only.

### Store

Stored in `Store`. Represents a shop/platform account inside a workspace.

### Upload

Stored in `Upload`. Tracks uploaded file metadata and storage key.

### Import Job

Stored in `ImportJob`. Tracks mapping/import status, row counts, error message, and error report storage key.

### Raw Import Row

Stored in `RawImportRow`. Preserves source row data, status, warnings, errors, and normalized previews.

### Mapping

Stored in `ImportColumnMapping`. Maps a source column name to a target schema field. User-confirmed mappings are required before import.

### Orders and Items

Stored in `Order`, `OrderItem`, and `Product`. Shopee imports upsert orders/products and replace order items per imported order.

### Cost and Expense Data

Stored in `ProductCost` and `OperatingExpense`. These feed profit calculations.

### Dashboard Aggregates

Stored in daily aggregate entities such as `RevenueDaily` and `ProfitDaily`, while several dashboard endpoints currently calculate directly from orders/items/expenses.

## Main API Areas

### Auth

Controller: `api/AuthController.java`

Endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/google`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/resend-otp`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/verify-reset-otp`
- `POST /api/v1/auth/reset-password`

Important services:

- `AuthServiceImpl`
- `JwtServiceImpl`
- `OtpServiceImpl`
- `EmailServiceImpl`
- `GoogleTokenVerifierImpl`

Security note: Google token verification should eventually validate expected audience/client ID and email verification. Current implementation should not be considered fully hardened.

### Workspace

Controllers:

- `api/workspace/MeWorkspaceController.java`
- `api/workspace/WorkspaceController.java`
- `api/workspace/OrganizationController.java`

Important services:

- `WorkspaceAccessServiceImpl`
- `WorkspaceBootstrapServiceImpl`
- `WorkspaceCommandServiceImpl`
- `WorkspaceMemberServiceImpl`
- `WorkspaceQueryServiceImpl`

All workspace-scoped business operations should call `WorkspaceAccessService` before touching workspace data.

### Stores

Controller: `api/store/StoreController.java`

Service: `StoreServiceImpl`

Stores are workspace-scoped and are limited by subscription plan.

### ETL

Controller: `api/etl/EtlController.java`

Important services:

- `EtlImportServiceImpl`
- `FileParsingServiceImpl`
- `RawRowServiceImpl`
- `TargetSchemaServiceImpl`
- `MappingSuggestionServiceImpl`
- `ShopeeOrderImportServiceImpl`
- `ImportErrorReportServiceImpl`

Core endpoints:

- `GET /api/v1/workspaces/{workspaceId}/etl/uploads`
- `POST /api/v1/workspaces/{workspaceId}/etl/uploads`
- `GET /api/v1/workspaces/{workspaceId}/etl/uploads/{uploadId}`
- `GET /api/v1/workspaces/{workspaceId}/etl/jobs`
- `POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/suggest-mapping`
- `POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/confirm-mapping`
- `POST /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/run`
- `GET /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}`
- `GET /api/v1/workspaces/{workspaceId}/etl/jobs/{importJobId}/error-report`

### Dashboard and Processed Data

Controllers:

- `api/DashboardController.java`
- `api/ProcessedDataController.java`

Services:

- `DashboardQueryServiceImpl`
- `ProcessedDataQueryServiceImpl`
- `ShopeeAnalyticsServiceImpl`

Dashboard endpoints summarize revenue, fees, COGS, ad spend, operating expenses, profit, daily trends, top products, and data quality.

### Costs and Expenses

Controllers:

- `api/ProductCostController.java`
- `api/OperatingExpenseController.java`

Services:

- `ProductCostServiceImpl`
- `OperatingExpenseServiceImpl`

Product costs can be applied to existing order items and optionally recalculate dashboard aggregates.

### Insights

Controller: `api/InsightController.java`

Service: `BusinessInsightServiceImpl`

Insights are currently rule-based. They detect missing data, negative profit, low margin, high refund rate, revenue drops, and negative-profit SKUs.

### Billing and Usage

Controllers:

- `api/billing/PlanController.java`
- `api/billing/SubscriptionController.java`

Services:

- `BillingPlanCatalog`
- `SubscriptionServiceImpl`
- `UsageMeterServiceImpl`

Usage counters currently cover uploads, imported rows, AI mapping count/tokens, and insight generation count.

### Admin

Controllers:

- `api/AdminUserController.java`
- `api/AdminAuditLogController.java`

Admin endpoints call `AdminAccess.requireAdmin`.

### payOS

Controllers:

- `api/PayosController.java`
- `api/PayosWebhookController.java`

Service:

- `PayosService`

Current payOS support is early-stage. The webhook endpoint echoes payload and does not verify signatures. Do not consider it production-ready.

## ETL Flow Details

The current Shopee MVP flow is:

```text
uploadAndAnalyze
  -> require import access
  -> require store belongs to workspace
  -> validate platform/dataSourceType
  -> parse CSV/XLSX
  -> record upload usage
  -> save file locally
  -> create Upload
  -> create ImportJob
  -> save RawImportRow records
  -> return headers and sample rows

suggestMapping
  -> require import access
  -> rebuild ParsedFile from raw rows
  -> load target schema fields
  -> run rule mapping and optional AI mapping
  -> persist suggested mappings
  -> return missing required fields

confirmMapping
  -> require import access
  -> validate required target fields
  -> persist user-confirmed mappings
  -> move job to VALIDATING or WAITING_FOR_MAPPING

runImport
  -> require import access
  -> require confirmed mapping
  -> import rows into products/orders/order_items
  -> mark raw rows IMPORTED/WARNING/INVALID
  -> record imported rows
  -> recalculate daily dashboard data
  -> generate row-error report if needed
```

Only `SourcePlatform.SHOPEE` with `DataSourceType.MARKETPLACE_ORDER` is currently supported in the MVP import path.

## Mapping Rules

`MappingRuleBook` handles Vietnamese/English aliases for Shopee order fields. It normalizes headers through `FieldNormalizer`.

Required target fields currently include:

- `external_order_id`
- `sku`

The import service also validates:

- `external_order_id`
- `sku`
- `product_name`
- positive `quantity`

Agents changing mapping/import behavior must keep required field validation in sync between schema, confirm-mapping, and row import.

## Storage

Current upload storage uses:

```properties
app.storage.local-dir=./storage/uploads
```

Long-term storage should use an abstraction that can support S3-compatible object storage.

The repository currently tracks files under `storage/uploads/`. Treat those as existing fixtures/runtime artifacts, not a pattern to continue.

## Security Notes

Before production:

- Remove committed SMTP password fallback and rotate the credential.
- Replace default JWT secret.
- Remove committed API-key placeholders that look like real secrets.
- Move secrets to environment variables or a secret manager.
- Harden Google login validation.
- Verify payOS webhook signatures.
- Decide whether auth should use short-lived access tokens plus refresh tokens, secure cookies, or OIDC provider integration.
- Add rate limiting for auth, OTP, upload, and AI endpoints.

## Known Gaps and Mismatches

- `api/dev/SwaggerTestController.java` is empty, but docs mention `GET /api/v1/dev/swagger-test-ids`.
- `DevTestDataServiceImpl` still returns old `actorUserId` Swagger instructions even though controllers use JWT principal now.
- Only one generated test exists, and it is in package `com.example.demo` while the app package is `databreeze`.
- There is no real test coverage for ETL, auth, billing, dashboard, or workspace permissions.
- Runtime artifacts are tracked under `storage/uploads`.
- A Word lock file is tracked under `docs`.
- Duplicate enum packages exist.
- Duplicate `shopee`/`shoppee` DTO packages exist.
- `application.properties` and profile files contain unsafe defaults for production.
- There is no Flyway/Liquibase migration strategy yet.
- Some Vietnamese text in source/docs is mojibake and should be cleaned carefully.

## Extension Rules For Agents

When adding a new data source:

1. Add source/platform enums only in the active enum package.
2. Add target schema fields or migration-backed schema definitions.
3. Add parser/mapping aliases.
4. Add import service logic behind a service interface.
5. Preserve raw rows and row-level errors.
6. Add dashboard calculation logic only after imported data shape is clear.
7. Add tests before broadening the API.

When adding dashboard features:

1. Check workspace/store access.
2. Prefer repository queries over loading all rows once data grows.
3. Keep date range behavior explicit.
4. Include data quality messaging when results may be incomplete.

When adding billing limits:

1. Store durable counters in PostgreSQL.
2. Use Redis only for short-lived throttles or locks.
3. Make limit errors user-readable.
4. Keep plan catalog and UI plan display in sync.

When changing auth:

1. Keep `CurrentUser.requireUserId` or equivalent principal enforcement.
2. Never reintroduce client-supplied `actorUserId` as an authority source.
3. Keep admin checks explicit.
4. Add tests for suspended/deleted users and role-gated endpoints.

## Verification Expectations

Before claiming backend work is done:

- Run compile/tests when environment permits.
- Add or update focused tests for changed behavior.
- Check that no secrets or generated upload files were added.
- Check that workspace access is enforced.
- Check that DTOs, service interfaces, and controllers remain consistent.
- Check that OpenAPI/Swagger docs match implemented endpoints.
