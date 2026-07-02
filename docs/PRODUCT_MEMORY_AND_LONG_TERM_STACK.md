# DataBreeze Product Memory And Long-Term Stack

This document is the durable reset note for DataBreeze. Read it when the project context feels fuzzy, when planning new backend work, or when deciding whether a technical choice fits the long-term product.

## One-Sentence Memory

DataBreeze is a Vietnamese-first profit intelligence platform that helps sellers and SMEs upload messy business files, map and validate the data, and understand real profit by SKU, shop, campaign, and time period.

## What DataBreeze Is

DataBreeze is a profit workspace for businesses whose data lives in exports, spreadsheets, and marketplace reports.

It is designed for users who sell through channels such as Shopee, TikTok Shop, Google Ads, and manual cost or expense sheets. These users often know their revenue, but they do not easily know their real profit after fees, refunds, discounts, ads, operating expenses, and missing cost data.

DataBreeze sits between raw business files and decision-making:

```text
Messy files in -> mapped and cleaned -> validated records -> trusted profit dashboard out
```

It is not mainly an accounting system, a POS system, or a generic BI tool.

## Core Product Promise

DataBreeze should help a seller answer:

- Which SKUs actually make money?
- Which shop performs best?
- Which campaign is profitable after costs?
- Where are fees, refunds, ad spend, or operating expenses eating margin?
- Which uploaded files imported cleanly, partially, or failed?
- Which rows, columns, or SKUs need attention before the dashboard can be trusted?
- What is the next action the seller should take?

The product wins only if it saves real spreadsheet work and makes profit clearer.

## Main MVP Feature

The first and most important feature is the SMB Profit Dashboard.

This dashboard is the center of the product because it proves the full loop:

1. A user can upload real business files.
2. DataBreeze can map messy columns into a consistent model.
3. The system can validate the data and show what is missing.
4. The backend can calculate trusted profit.
5. The frontend can show the user what happened and what to fix next.

Everything else should grow from this loop.

## Product Modules

The broader product direction includes five modules:

| Module | Meaning | Priority |
|---|---|---|
| SMB Profit Dashboard | Core dashboard for revenue, costs, fees, margins, and profit | 1 |
| Finance Reconciliation | Trust layer for sales, fees, payouts, costs, refunds, and expenses | 2 |
| Inventory Forecaster | Future support for stock and demand decisions | 3 |
| Compliance Reporter | Future reporting output from cleaned business data | 4 |
| Seller Ops Hub | Operational workflows for sellers and small teams | 5 |

The priority order matters. Do not build broad seller operations, advanced reporting, or inventory prediction before the profit dashboard and reconciliation loop are solid.

## Primary Users

### Solo Sellers

Solo sellers may run one to three shops and manage files themselves. They need speed, clarity, and a low-friction first experience.

They need:

- Simple sign-up.
- A personal workspace by default.
- Store setup.
- File upload.
- Mapping guidance.
- Missing cost warnings.
- A dashboard that answers what to fix next.

### Multi-Shop Sellers

Multi-shop sellers need comparison and repeatability.

They need:

- Several shops inside one workspace.
- Store filters.
- All-store views.
- Repeatable mapping templates.
- Upload history.
- Usage that scales with real workload instead of one subscription per shop.

### SMEs And Small Teams

SMEs need team-ready structure without enterprise heaviness too early.

They need:

- Organization workspaces.
- Member roles later.
- Auditability.
- Billing and usage visibility.
- Trustworthy financial data handling.

### Agencies Or Technical Helpers

Some users may repeatedly clean data or build dashboards for businesses.

They need:

- Reusable templates.
- Predictable APIs.
- Clear error reports.
- Extensible import support.

They are useful later, but the initial product should still feel solo-seller friendly.

## Account And Workspace Model

The account model should support solo users and teams from the beginning.

Core objects:

- User: the person who logs in.
- Workspace or organization: the tenant boundary where business data lives.
- Store or shop: a sales channel/account inside a workspace.
- Upload: a file the user submitted.
- Import job: the processing run for an upload.
- Mapping template: reusable source-column to DataBreeze-field mapping.
- Dashboard: the business view produced from imported and calculated data.

Important rule:

```text
One workspace/business can contain multiple shops.
```

A solo user should get a personal workspace automatically. If that user later creates or joins a team workspace, both models should still work.

## Core User Flow

The main flow should remain easy to remember:

```text
User logs in
  -> enters a workspace
  -> adds or selects a store
  -> uploads CSV/XLSX files
  -> maps messy columns
  -> validates rows and required fields
  -> imports clean data
  -> sees profit and warnings
  -> fixes missing costs or bad rows
  -> returns to the dashboard with more trust
```

The frontend should make this feel guided:

```text
Upload -> Map columns -> Check data -> Update dashboard
```

The backend should make this durable:

```text
Upload metadata -> raw rows -> confirmed mapping -> import job -> processed commerce data -> dashboard calculations
```

## Long-Term Stack Decision

The best long-term stack for DataBreeze is intentionally boring, stable, and scalable:

```text
React + TypeScript + Vite
Java 21 + Spring Boot
PostgreSQL
Spring Batch
Redis
S3-compatible object storage
Docker
GitHub Actions
AWS later when scale or compliance justifies it
```

## Recommended Stack By Layer

| Layer | Long-term choice | Why |
|---|---|---|
| Frontend | React + TypeScript + Vite | Best fit for a logged-in dashboard app backed by a separate API |
| Component system | Tailwind CSS + reusable primitives | Maintainable frontend implementation |
| Routing | React Router | Clear SPA navigation for dashboard workflows |
| Server state | TanStack Query | Strong fit for uploads, jobs, dashboards, costs, usage, and cache invalidation |
| Backend | Java 21 + Spring Boot | Stable, maintainable, enterprise-friendly |
| Backend shape | Modular monolith | Simpler than microservices and enough for a long time |
| Batch/ETL | Spring Batch when needed | Good fit for restartable, chunked, large file imports |
| Database | PostgreSQL | Strong system of record for SaaS and reporting |
| Cache/support | Redis | Useful for caching, progress, locks, and queue-like coordination |
| File storage | S3-compatible object storage | Needed for uploaded files, processed files, and error reports |
| Auth | Spring Security with JWT/session strategy | Fits Spring and backend-owned business access rules |
| Infra | Docker + managed services | Clean operational model without overbuilding |
| CI/CD | GitHub Actions | Standard automation path |
| Monitoring | Logs first, Prometheus/Grafana later | Scales with maturity |

## Why This Stack Fits

DataBreeze is not mainly a content website and not a toy SaaS. It is a structured business system with:

- Multi-tenant workspaces.
- Auth and membership.
- File uploads.
- Column mapping.
- Validation.
- Import jobs.
- Durable business records.
- Cost and expense data.
- Dashboard calculations.
- Subscription and usage limits.
- Future audit and compliance needs.

The React + Spring Boot + PostgreSQL split keeps responsibilities clear:

- The frontend owns the product experience.
- The backend owns business rules, security, ingestion, and calculations.
- PostgreSQL owns durable structured state.
- Object storage owns file bytes.
- Redis supports coordination and caching but does not become the source of truth.

## Frontend Direction

Use React for the product app, not Next.js by default.

Reason:

DataBreeze is primarily a logged-in dashboard application. It does not need an SEO-first full-stack web framework for the core product. The backend already exists as Spring Boot, so duplicating backend responsibilities in a frontend framework would add complexity without much value.

React + Vite is the better fit for:

- Dashboard screens.
- Upload and mapping flows.
- Client-side routing.
- Server-state-heavy API integration.
- Fast iteration.
- Clear separation from the backend.

Next.js can still be considered later for a separate marketing site, blog, documentation site, or SEO-heavy public content. It should not drive the product app architecture unless the product requirements change.

## Backend Direction

Keep the backend as a Java/Spring Boot modular monolith for a long time.

Spring Boot is a strong fit because DataBreeze needs:

- Validation-heavy APIs.
- Tenant and workspace access rules.
- Authentication and authorization.
- File parsing and import workflows.
- PostgreSQL integration.
- Payment and billing integration.
- Scheduled or batch processing.
- Clear service boundaries.
- Enterprise-friendly maintainability.

Do not split into microservices early. Microservices should wait until there is a measured scaling, ownership, deployment, or isolation need.

## Data And Job Architecture

Because DataBreeze is file-based and ETL-heavy, the architecture should explicitly include:

- PostgreSQL as the system of record.
- Spring services for early import logic.
- Spring Batch when imports need restartability, chunking, retries, or durable job repositories.
- Redis for temporary coordination, progress, locks, and caching.
- Object storage for uploaded files, generated error reports, and processed files.

Do not force everything into simple request-response APIs once file sizes and job duration grow.

## PostgreSQL Responsibilities

PostgreSQL should store durable business state:

- Users.
- Workspaces.
- Memberships.
- Stores.
- Upload metadata.
- Import jobs.
- Raw row records or row references.
- Confirmed mappings.
- Orders and order items.
- Products and SKUs.
- Product costs.
- Operating expenses.
- Dashboard aggregates.
- Usage counters.
- Subscriptions.
- Audit logs.

Do not store important financial records only in Redis, browser state, object storage metadata, or logs.

## Redis Responsibilities

Redis is support infrastructure, not the durable source of truth.

Use Redis for:

- Short-lived cache.
- Import progress snapshots.
- Duplicate-import locks.
- Rate limits.
- Lightweight coordination.
- Queue-like support if the project does not yet need heavier job infrastructure.

Do not use Redis as the only record of:

- Imported rows.
- Financial calculations.
- Billing counters.
- Audit history.
- Completed import status.

## Object Storage Responsibilities

Object storage should hold file bytes:

- Original uploads.
- Generated error reports.
- Processed export files.
- Future report artifacts.

PostgreSQL should store metadata and permissions around those files.

Long term, the app should use a storage abstraction so local development can use local disk and production can use S3-compatible storage.

## Infrastructure Path

Start simple:

```text
React app
Spring Boot API
PostgreSQL
Local/S3-compatible file storage
Docker for repeatable runtime
GitHub Actions for CI
```

Grow when needed:

```text
Managed PostgreSQL
Managed Redis
S3-compatible object storage
Containerized Spring Boot deployment
Separate frontend hosting
Structured logs
Metrics
Alerts
Backups
```

AWS is a sensible long-term destination when scale, reliability, compliance, or enterprise customers justify it. Do not overbuild AWS infrastructure before the product loop is proven.

## Pricing Memory

The pricing structure discussed:

| Plan | Price idea | Intended fit |
|---|---:|---|
| Free | 0 VND | Try the product safely |
| Starter | 199k VND/month | Solo or small seller |
| Pro | 399k VND/month | Growing seller with more data |
| Enterprise | Custom or 1M+ VND/month | Team, SME, or higher-touch support |

Pricing should scale with real product value:

- Rows processed.
- Upload count.
- Store count.
- Workspace/member count.
- File size.
- Mapping/AI usage.
- Advanced reconciliation/reporting features.

Avoid pricing that forces one subscription per shop for a solo seller managing multiple shops. A workspace should be able to contain multiple stores.

## Frontend Responsibilities

The frontend is responsible for presenting the product workflow and integrating with backend APIs.

Frontend build priority:

1. Product shell and navigation.
2. Dashboard empty and data states.
3. Upload history and new upload.
4. Column mapping.
5. Validation results.
6. Missing product costs.
7. Expenses.
8. Insights.
9. Plan and usage.
10. Settings and team controls later.

## What The Backend Must Remember

The backend is responsible for correctness and trust.

It should:

- Enforce workspace access on every workspace-scoped operation.
- Store raw import context enough to explain what happened.
- Require confirmed mappings before import.
- Validate required fields before updating business records.
- Preserve row-level warnings and errors where useful.
- Keep financial calculations auditable.
- Keep usage and subscription counters durable.
- Treat AI as assistance, not authority.
- Prefer clear service boundaries inside the modular monolith.

Backend priority order:

1. Auth and workspace access.
2. Store model.
3. Upload and parse files.
4. Mapping suggestion and confirmation.
5. Validation and row error reporting.
6. Import orders/products/items.
7. Costs and expenses.
8. Dashboard calculations.
9. Insights.
10. Billing/usage limits.
11. New sources after the Shopee loop works.

## Near-Term Build Sequence

The sensible build sequence is:

1. Make sign-up/login and personal workspace reliable.
2. Let a user add a store.
3. Let a user upload a Shopee file.
4. Parse headers and sample rows.
5. Suggest mappings.
6. Let the user confirm mappings.
7. Validate rows.
8. Import rows into a durable model.
9. Show dashboard revenue, costs, fees, gross profit, net profit, margin, and top SKUs.
10. Surface missing COGS and let the user fix it.
11. Recalculate profit after cost fixes.
12. Add operating expenses.
13. Add finance reconciliation depth.
14. Add more sources only after the loop is understandable.

## Long-Term Vision

DataBreeze can grow from profit visibility into a broader seller/SME operating workspace:

```text
Profit visibility
  -> trust and reconciliation
  -> forecasting and reporting
  -> operations and team workflows
  -> enterprise controls
```

The product should grow outward from trusted imported data, not from trend features.

## Decision Rule

When deciding what to build next, ask:

1. Does this make real profit clearer?
2. Does this reduce spreadsheet labor?
3. Does this make messy Vietnamese business files easier to trust?
4. Does this preserve workspace and financial correctness?
5. Does this help the first seller workflow before adding broad platform complexity?

If the answer is no, it is probably not the next priority.
