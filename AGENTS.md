# Development instructions

Use Java 17, Spring Boot 3 and PostgreSQL.
Preserve public API fields and applied Flyway migrations unless a reviewed contract change requires otherwise.

## Architecture

Keep domain and application code framework-free.
Use application/model, application/port/in, application/port/out, application/usecase and application/service.
Each use case exposes execute(OperationContext, input).
Pass context explicitly. Do not read Spring Security or MDC from the core.
Controllers consume input ports and return application views.
Only composition adapters connect different feature implementations.
Wire providers and transaction decorators in the application composition root.

Domain factories and aggregates own creation and lifecycle invariants.
Repositories implement application-owned ports.
Use typed failures. Map them to HTTP only at the boundary.
Never bypass the approved-occurrence exclusion constraint or weaken room locks.

## Tests and style

Run ./mvnw spotless:apply and ./mvnw verify.
Run ./mvnw verify -Pintegration against the isolated meeting_booking_test database.
Add domain, handler, adapter and integration tests at the layer that changes.
Keep architecture, documentation and API collection checks active.
Write documentation, comments, test names and commit messages in English.
Keep README files and local documentation links current.
Use final dependencies, constructor injection and immutable records where appropriate.
Comments explain a non-obvious reason, not the line below them.
Do not add ticket identifiers or project comparisons to code comments.

## Delivery

Use focused business commits and pull requests.
Record acceptance criteria, findings, verification, migration impact and rollback.
Merge only after review and successful checks at the exact head.
Do not auto-merge a dependency upgrade outside Java 17 or Spring Boot 3.
Never overwrite another contributor's local changes.
Never deploy, migrate or modify a production database without the required authorization and recovery plan.
