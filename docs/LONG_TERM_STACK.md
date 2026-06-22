# Backend Long-Term Stack Notes

This backend-specific stack note complements the workspace-level `docs/LONG_TERM_STACK.md`.

## Validated Backend Direction

The backend should remain a Java/Spring Boot modular monolith for a long time.

Recommended backend stack:

```text
Java 21
Spring Boot
Spring MVC
Spring Security
Spring Data JPA
PostgreSQL
Flyway or Liquibase
Apache POI / Commons CSV for file parsing
Spring Batch when ETL outgrows synchronous services
S3-compatible object storage
Redis for support concerns
Spring Actuator + Micrometer
JUnit + Spring Boot Test + Testcontainers
```

## Why This Fits DataBreeze

DataBreeze has durable business state, tenant boundaries, file imports, jobs, financial calculations, and audit requirements. Spring Boot and PostgreSQL fit those needs better than a trendier all-in-one stack.

The backend should optimize for:

- Correctness.
- Traceability.
- Tenant isolation.
- Testability.
- Durable job state.
- Clear business APIs.
- Maintainable extension to new source platforms.

## Java 21 Upgrade

The current `pom.xml` uses Java 17. Java 17 is acceptable for now. Java 21 is the better long-term target because it is a modern LTS baseline and pairs well with future Spring versions.

Do not upgrade casually. When upgrading:

1. Update Java toolchain/runtime.
2. Confirm Spring Boot compatibility.
3. Run tests.
4. Fix compiler warnings/errors.
5. Update CI image.
6. Document required local Java version.

## Spring Batch Timing

Do not introduce Spring Batch just because it exists.

Add Spring Batch when DataBreeze needs:

- Import restart after failure.
- Chunk-based processing for large files.
- Parallel file/import processing.
- Retry/skip policies.
- Durable job repository.
- Job explorer/admin visibility.

Until then, the current service-oriented ETL is easier to understand and iterate on.

## PostgreSQL Rules

PostgreSQL owns durable state.

Use it for:

- Users and auth identities.
- Workspaces and memberships.
- Stores.
- Uploads.
- Import jobs.
- Column mappings.
- Raw rows.
- Orders and order items.
- Costs and expenses.
- Dashboard aggregates.
- Usage counters.
- Subscriptions.
- Audit logs.

Future agents should add indexes for workspace/date/store/platform query patterns before adding a separate analytics database.

## Redis Rules

Redis is optional support infrastructure, not a required MVP blocker.

Use Redis for:

- Caching stable lookup/reference data.
- Short-lived rate limits.
- Distributed locks around duplicate imports.
- Temporary progress updates.
- Lightweight coordination.

Do not use Redis as the only copy of financial records, import state, dashboard values, billing counters, or audit trails.

## File Storage Rules

Local file storage is acceptable for development only.

Long-term:

- Define a storage service interface.
- Support local storage for dev.
- Support S3-compatible object storage for production.
- Store metadata in PostgreSQL.
- Store bytes in object storage.
- Do not expose raw object keys as security boundaries.

## Migration Rules

`spring.jpa.hibernate.ddl-auto=update` is convenient for MVP but not safe as the production migration strategy.

Before production:

- Add Flyway or Liquibase.
- Generate baseline migrations from current entity state.
- Stop relying on Hibernate `ddl-auto=update`.
- Use migrations for target schemas and plan catalog if those become product-managed data.

## Testing Stack

Recommended backend tests:

- Unit tests for mapping, parsing, amount/date normalization, and insight rules.
- Service tests for ETL flow with fixtures.
- Controller tests for auth and permission failures.
- Repository/integration tests with Testcontainers PostgreSQL.
- Billing limit tests.
- Dashboard calculation tests.

The current test coverage is not enough for production.

## Observability

Near term:

- Spring Actuator.
- Structured logs.
- Request IDs.
- Import job IDs in logs.
- Error report links in job responses.

Later:

- Micrometer metrics.
- Prometheus/Grafana dashboards.
- Tracing for long import flows.
- Alerting on failed imports, payment webhook errors, and high API error rates.
