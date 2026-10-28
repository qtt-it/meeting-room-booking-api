---
title: 'Testing guide'
type: guide
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Testing guide

## Prerequisites

Use Java 17 and a dedicated PostgreSQL database.
Integration tests require the exact database name `meeting_booking_test`.
The fixture deletes data between tests.

## Commands

```bash
./mvnw verify
docker compose -f infra/compose.test.yml up -d --wait
./mvnw verify -Pintegration
docker compose -f infra/compose.test.yml down
```

Override the connection with `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` and `TEST_DATABASE_PASSWORD`.
The default database endpoint is localhost:55432.
Tests do not use H2 because it does not verify PostgreSQL GiST indexes, row locks and exclusion constraints.

## Behavioral coverage

| Layer | Coverage |
| --- | --- |
| Unit | Half-open windows, business hours, Sundays, horizon and recurrence |
| Documentation | English language markers, composed/decomposed Unicode and local Markdown links |
| HTTP foundation | Safe request IDs and MDC cleanup |
| Architecture | Framework-free cores, independent input ports, feature isolation and cycle checks |
| Application | Transaction commit/rollback, explicit actor checks and redacted credential diagnostics |
| Domain policies | Aggregate creation/lifecycle, recurrence strategy registration and room specifications |
| Runtime configuration | Actual prod profile defaults, fail-fast TLS/secrets/CORS checks and bootstrap control |
| HTTP admission | Known/unknown-length body limits, exact boundary and correlated 413 responses |
| Integration | Login, refresh, logout, password change, roles and room limits |
| Booking integration | Pending requests, approval races, atomic series rollback, versions, capacity and invitations |
| Database | Exclusion constraint, including direct SQL writes |
| Container smoke | Health, login, profile and room catalog after Compose startup |
| API collection | 25 ordered requests against a disposable stack: auth, rooms, recurrence, approval, RSVP, cancellation and rejection |

Surefire runs `*Test`. Failsafe runs `*IT` when the `integration` profile is active.
Tests do not wrap HTTP requests in an outer test transaction.
They verify actual committed state.

## Local results

The initial delivery passed Java 17 tests against a temporary PostgreSQL 14.22 cluster.
The local Docker daemon was unavailable.
The baseline passed 24 unit tests, 28 integration tests and all 25 Bruno requests.
CI uses PostgreSQL 17, matching Compose.
Read the current PR checks for the latest result. Unit success does not prove container startup.

## Delivery evidence

[The baseline CI run](https://github.com/qtt-it/meeting-room-booking-api/actions/runs/40642049729) verifies commit `0cac3f0`:

- Java 17 and PostgreSQL 17: 24 unit tests and 28 integration tests.
- Actual image build, Compose startup and readiness.
- All 25 API requests, including auth, rooms, recurrence, approval, RSVP, cancellation and rejection.
- Test reports and API collection reports retained for 14 days.

Later changes must also pass CI at the exact PR head before merge.

[The architecture CI run](https://github.com/qtt-it/meeting-room-booking-api/actions/runs/40657587996) verifies commit `298d0b6`:

- 56 unit tests and 29 PostgreSQL integration tests.
- Nine executable architecture rules.
- Image build, Compose readiness and all 25 API requests.

[The runtime-hardening CI run](https://github.com/qtt-it/meeting-room-booking-api/actions/runs/40658707027) verifies commit `1758fa6`:

- 70 unit tests and 30 PostgreSQL integration tests.
- Image build, Compose readiness and all 25 API requests.
- Actual prod-profile defaults and known/unknown-length body regressions.

A local packaged prod-profile check also reached readiness with a separate runtime role over verified TLS 1.3.
The public OpenAPI request returned 401, and a real chunked oversized request returned the correlated 413 response.
That check used an isolated PostgreSQL 14.22 database and a temporary local certificate, not a production environment.

## Report locations

Read `target/surefire-reports` and `target/failsafe-reports` in each module.
CI retains reports for 14 days.
Bruno reports omit all headers and request/response bodies to avoid storing credentials.
The container job uses a disposable database, without access to an external application database.
Actions and the CLI use pinned revisions or versions. Dependabot proposes updates.
Add regression tests and API requests when you change an endpoint or a rule.

DocumentationTest scans Markdown across the repository, including README files and planning records.
Its language check detects accented markers, including decomposed Unicode.
Editorial review is still required for unaccented prose.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial testing guide |
| 2026-10-28 | @qtt-it | Standardize all test guidance in English |
| 2026-10-28 | @qtt-it | Record architecture evidence and runtime-hardening coverage |
| 2026-10-28 | @qtt-it | Record completed hardening CI and packaged-profile verification |
