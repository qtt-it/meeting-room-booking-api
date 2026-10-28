---
title: 'ADR-002: Clean architecture and hexagonal boundaries'
type: adr
status: accepted
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
decision_date: 2026-10-28
decision_makers: ['@qtt-it']
capture_source: manual
---

# ADR-002: Clean architecture and hexagonal boundaries

## Context

The initial modular monolith isolates SQL adapters but still places Spring annotations in application services.
Booking also depends directly on concrete identity and room services.
These dependencies make another driver or a framework-free test harder to introduce.

The HTTP and database contracts already have consumers.
This change must preserve endpoints, JSON fields, migrations, approval ordering and refresh-family revocation.

## Decision

Keep the modular monolith and apply the dependency rule inside each feature.

- Domain classes depend only on Java and the shared core.
- Application ports, models, services and use cases do not import Spring, JDBC, Jackson or HTTP adapters.
- Each use case has one public operation: `execute(OperationContext, input)`.
- Input ports use typed handlers. Controllers depend on these ports, not application implementations.
- Output ports describe repositories and external module reads.
- Booking owns its room and employee ports. Composition adapters translate provider models into booking-owned snapshots.
- Application services coordinate the existing workflows. Domain factories and aggregate guards own booking construction and lifecycle invariants.
- The composition root constructs services, strategies and use cases with explicit `@Bean` providers.
- Infrastructure decorates use cases with transactions. Commands use write transactions. Queries use read-only repeatable-read transactions.
- Refresh reuse commits family revocation and its audit event before propagating the authentication failure.
- The HTTP boundary maps semantic failure kinds into status codes.
- `@CurrentOperation` resolves the verified actor and request ID at the HTTP boundary.
- `@AdminOnly` provides method-level protection. Application authorization remains mandatory for other drivers.

The five Maven modules remain compilation units.
ArchUnit enforces the finer package boundaries in CI.
Layer packages are not separate Maven artifacts.

## Java package layout

```text
libs/<feature>/
  domain/src/main/java/.../domain/
  application/src/main/java/.../application/
    model/
    port/in/
    port/out/
    usecase/
    service/
  infra/db/src/main/java/.../infra/db/
  infra/http/src/main/java/.../infra/http/
apps/meeting-api/
  config/
  infra/module/
  infra/transaction/
```

Java reserves the word `interface`.
The `port/in` and `port/out` packages express that responsibility without an invalid package name.

## Consequences

Use cases and domain policies run without a Spring container.
Drivers cannot silently bypass transaction policy through a controller-to-service import.
Consumer-owned ports prevent booking from importing another feature's domain entities.

The design adds small handler classes and explicit provider registration.
These classes define typed entry points and apply driver-independent context and transaction policy.
Application services remain internal workflow implementations.
The composition root must register every new handler and select its transaction mode.

## Alternatives considered

| Alternative | Benefit | Reason not selected |
| --- | --- | --- |
| Keep annotations in application services | Fewer files | Retains framework coupling |
| Split every layer into a Maven artifact | Compile-time classpath isolation | Too much build structure for three feature contexts |
| Direct calls between concrete feature services | Shorter calls | Couples consumers to provider implementations and internal models |
| Add a generic Unit of Work framework | Central transaction API | Spring already owns the PostgreSQL transaction implementation |
| Add a broker, saga or outbox now | Asynchronous integration | No external event consumer or delivery requirement exists |

## Verification and rollback

ArchitectureTest checks framework isolation, inward dependencies, context boundaries, input ports, single-operation handlers and module cycles.
Unit tests cover aggregate factories, state guards, recurrence strategies, room specifications, context resolution and transaction behavior.
The existing PostgreSQL and container API suites verify contract compatibility.

No migration is required.
Restore the previous application image to reverse this refactor.

## Related

- [Architecture](../architecture.md)
- [Design patterns](../reference/design-patterns.md)
- [Backend decisions](ADR-001-backend-design.md)

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Record framework-free core and explicit adapter boundaries |
