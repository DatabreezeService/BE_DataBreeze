# DataBreeze Product Requirements

This document preserves the product meaning behind DataBreeze so backend agents do not treat the codebase as just a Shopee import demo. The current implementation is Shopee-focused, but the long-term product is broader.

## Product Vision

DataBreeze is an AI-assisted Vietnamese-first data pipeline and business insight workspace. It helps SMEs, online sellers, and technical teams turn messy files into clean dashboards without writing code or hiring a data team.

The product should become a trusted workflow layer between raw business exports and decision-making.

## Core Promise

```text
Upload messy files.
Map columns safely.
Clean and validate data.
See profit dashboards and business warnings.
Reuse the workflow next time.
```

## Why This Product Exists

Many Vietnamese sellers and SMEs operate with fragmented data:

- Marketplace exports.
- Ads reports.
- Cost spreadsheets.
- Expense files.
- Manual reconciliation sheets.

Generic tools are often too technical, too expensive, too broad, or not localized for Vietnamese business terminology. DataBreeze should win by being practical, localized, and workflow-specific.

## Primary User Segments

### Solo Seller

The first product experience should work for a single seller with one to three shops. This user wants speed and clarity, not technical configuration.

Needs:

- Simple onboarding.
- Store setup.
- Shopee/TikTok/Ads file upload.
- Clear dashboard.
- Missing cost warnings.
- "What should I fix?" guidance.

### Multi-Shop Seller

This user operates several shops or channels and needs comparison.

Needs:

- Store filters.
- All-store dashboard.
- Per-store profit.
- Repeatable mapping templates.
- Upload history.

### SME / Team

This user needs team access and business controls.

Needs:

- Organization workspace.
- Member roles.
- Audit history.
- Plan/usage visibility.
- Safer handling of financial data.

### Developer / Agency

This user repeatedly builds imports, scripts, and dashboards for clients.

Needs:

- Reusable templates.
- Predictable APIs.
- Clean error reports.
- Less ad-hoc ETL work.
- Extensible source support.

## Core Product Objects

### User

The person who logs in.

### Workspace

The business space containing stores, uploads, dashboards, members, and billing. Workspaces can be personal or organization-based.

### Store

A shop or account on a platform such as Shopee, TikTok Shop, or another commerce source.

### Data Source

The origin/type of uploaded data, such as Shopee orders, TikTok settlement, Google Ads, or expense files.

### Upload

A single uploaded file.

### Import Job

The processing run for an upload.

### Mapping Template

Reusable mapping between source file columns and DataBreeze fields.

### Dashboard

The business view of imported and calculated data.

## MVP Scope

The current MVP backend focuses on:

- Email/password auth and JWT.
- Personal/organization workspace model.
- Stores.
- Shopee Vietnam order CSV/XLSX uploads.
- Rule-based mapping suggestion.
- Optional AI mapping support.
- User-confirmed mapping.
- Raw row persistence.
- Import into orders, order items, and products.
- Dashboard summary/daily/top products.
- Product cost entry and COGS application.
- Operating expenses.
- Rule-based insights.
- Usage and subscription plan limits.

## Long-Term Source Scope

Future source support can include:

- Shopee orders.
- TikTok Shop orders/settlements.
- Google Ads.
- Meta Ads.
- TikTok Ads.
- Manual product cost sheets.
- Manual operating expense sheets.
- Inventory and stock sheets.
- Agency/client report templates.

Do not add all sources at once. Each source should have a clear import flow, mapping rules, validation behavior, and dashboard impact.

## Core Workflow Requirements

### First-Time Setup

The user signs up, receives a personal workspace, and can add the first store.

The empty dashboard should point to one action: upload the first file.

### Upload and Mapping

The upload flow is the heart of the product.

Requirements:

- User selects store and data source type.
- User uploads CSV/XLSX.
- Backend parses headers and sample rows.
- System suggests mappings.
- Required fields are clearly shown.
- User confirms or edits mappings.
- Mapping can later become reusable as a template.
- Import cannot run without confirmed mappings.

### Validation and Error Handling

Validation should be useful, not scary.

The product should show:

- Total rows.
- Successful rows.
- Warning rows.
- Failed rows.
- Missing required columns.
- Missing COGS.
- Invalid dates.
- Duplicate or invalid orders where applicable.

Users should be able to download error reports when rows fail or warn.

### Dashboard

The dashboard should answer:

- How much revenue did I make?
- How much did I lose to discounts, refunds, fees, shipping, ads, expenses, and COGS?
- What is my gross profit?
- What is my net profit?
- Which SKUs are best or worst?
- Which stores perform better?
- Is the selected date range missing important data?

### Insights

Insights should be grounded in imported data.

Early rule-based insights:

- Missing product costs.
- Negative net profit.
- Low margin.
- High refund rate.
- Revenue drop.
- Loss-making SKU.

Future AI insights should explain business context but must not hide calculation logic.

## Pricing and Limits

The backend currently seeds these plan concepts:

| Plan | Intended fit |
|---|---|
| Free Trial | Try ETL, mapping, and basic insight safely. |
| Starter | Small Shopee seller starting profit visibility. |
| Growth | Growing seller/team with weekly analysis. |
| Business | SME team with multiple stores and priority support. |

Usage dimensions:

- Upload count.
- Imported row count.
- Max file size.
- Store count.
- Member count.
- AI mapping count/token usage.
- Insight generation count.

Future pricing should remain tied to real value: rows processed, stores, team size, and insight/AI usage.

## Non-Goals For Early Product

- Do not build a generic BI platform.
- Do not build every integration at once.
- Do not build full enterprise RBAC before solo seller workflow works.
- Do not prioritize AI-generated advice over reliable import and profit calculations.
- Do not require users to understand database schemas.

## Product Quality Bar

DataBreeze succeeds only if users return because it saves real hours.

For every feature, ask:

- Does this reduce manual spreadsheet work?
- Does this make profit clearer?
- Does this handle Vietnamese file mess better?
- Does this preserve trust and auditability?
- Can a non-technical seller understand what happened?
