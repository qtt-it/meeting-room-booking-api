# Meeting room booking API

Java 17, Spring Boot 3 and PostgreSQL REST API for internal room reservations.
The service supports administrator approval, recurrence, employee invitations and availability searches.

## Quick start

```bash
cp .env.example .env
docker compose up --build -d --wait
curl --fail http://localhost:8080/actuator/health/readiness
```

Compose starts the API and PostgreSQL. Replace demo credentials before deployment.

- Swagger UI: http://localhost:8080/docs
- OpenAPI: http://localhost:8080/v3/api-docs
- Demo administrator: `admin@example.com`
- Demo password: `LocalDemo!2026ChangeMe`

```bash
curl --request POST http://localhost:8080/api/v1/auth/login \
  --header 'Content-Type: application/json' \
  --data '{"email":"admin@example.com","password":"LocalDemo!2026ChangeMe"}'
```

Send `data.accessToken` as `Authorization: Bearer <token>`.
The administrator creates employee accounts and rooms.

## Business rules

- At most 10 active rooms.
- Monday-Saturday, 09:00-18:00, `Asia/Ho_Chi_Minh`.
- Pending requests do not reserve rooms. The first successful approval wins.
- Approved occurrences cannot overlap in the same room. Adjacent meetings are allowed.
- Recurring requests contain at most 52 occurrences within 366 days.
- Approval is atomic for the entire series. One conflict leaves the request pending.
- Organizers and administrators edit unstarted series or cancel future occurrences.
- Editing an approved series releases its reservations and requires approval again.
- Conflict responses include alternative rooms or later times when available.
- Employees receive specific invitations and can accept or decline.

Read the [specification](docs/specs/booking-system.md) for explicit implementation choices.

## Structure

```text
apps/meeting-api/       Composition root, security, configuration and migrations
libs/foundation/       Shared actor, errors, response envelope and request IDs
libs/identity/         Accounts, authentication and refresh sessions
libs/room/             Room catalog and capacity rules
libs/booking/          Requests, recurrence, approval, invitations and availability
  domain/src/          Entities and pure Java rules
  application/src/     Use cases and repository ports
  infra/db/src/        PostgreSQL adapters
  infra/http/src/      REST controllers
docs/                   Specification, architecture, API, decisions and operations
.planning/backlog/      Business stories, acceptance criteria and tasks
infra/                  Isolated database for tests
tools/bruno/            Versioned API requests
.github/workflows/      Build, tests and container checks
```

Maven builds five modules. Each feature uses separate source roots for its architectural layers.

## Local development

Install JDK 17+ and Docker. Maven Wrapper downloads Maven on first use.

```bash
docker compose up -d postgres
export DATABASE_PASSWORD=local_database_password
export JWT_SECRET=local-development-key-replace-before-any-deployment-2026
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com
export BOOTSTRAP_ADMIN_PASSWORD='LocalDemo!2026ChangeMe'
./mvnw verify
./mvnw package -DskipTests
java -jar apps/meeting-api/target/meeting-api.jar
```

Java does not load `.env` automatically. Export variables when running directly.
Compose reads `.env` automatically.

## Tests

```bash
./mvnw spotless:apply
./mvnw verify
docker compose -f infra/compose.test.yml up -d --wait
./mvnw verify -Pintegration
docker compose -f infra/compose.test.yml down
```

Integration tests use the isolated `meeting_booking_test` database on port 55432.
The fixture removes test data between tests.
See the [testing guide](docs/guides/testing.md).
Open [the Bruno collection](tools/bruno/README.md) to run all 25 API requests in order.
The collection writes demo data; use a disposable development database.

## Decisions and documentation

The modular monolith uses JDBC and Flyway.
PostgreSQL exclusion constraints protect approved bookings against concurrent overlaps.
Access tokens expire after 15 minutes. Opaque refresh tokens rotate and remain revocable.

Read the [documentation index](docs/README.md), [architecture](docs/architecture.md) and [decisions](docs/decisions/ADR-001-backend-design.md).
The [backlog](.planning/README.md) records business branches and verification.
Each business change has its own commit and pull request. Merge commits preserve the assessment history.
See [merged pull requests](https://github.com/qtt-it/meeting-room-booking-api/pulls?q=is%3Apr+is%3Amerged) and [CI runs](https://github.com/qtt-it/meeting-room-booking-api/actions/workflows/ci.yml).

Email delivery, email password recovery, monthly recurrence and single-occurrence edits are outside this version.
