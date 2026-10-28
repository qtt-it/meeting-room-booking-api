---
title: 'Applied design patterns'
type: reference
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Applied design patterns

Use a pattern where it defines a real boundary or protects a business rule.
Do not add unused extension points.

| Pattern | Implementation | Purpose |
| --- | --- | --- |
| Ports and adapters | UseCase, feature input handlers, repository and integration ports | Separate drivers and persistence from workflows |
| Dependency inversion | BookingRooms and BookingEmployees | The consuming context owns the contract |
| Use case | CreateBookingUseCase, ApproveBookingUseCase and the other typed handlers | One explicit operation with request context and typed input/output |
| Decorator | TransactionalUseCase | Add transaction behavior without coupling the core to Spring |
| Composition root and factory | UseCaseConfiguration and explicit bean methods | Construct implementations and select policy in one place |
| Repository | IdentityRepository, RoomRepository and BookingRepository | Hide JDBC persistence behind application-owned contracts |
| Aggregate and value object | Booking, TimeWindow, Recurrence and BookingDraft | Protect identity, time windows and state invariants |
| Factory | BookingFactory | Create pending aggregates and rebuild an edited series consistently |
| Strategy | DailyRecurrenceStrategy and WeeklyRecurrenceStrategy | Keep recurrence algorithms separate and selectable |
| Specification | RoomRequirements | Reuse capacity, location, floor and equipment matching in search and recommendations |
| Authorizer | BookingAuthorizer | Apply organizer and invitee permissions without framework state |
| Mapper | BookingMapper and integration adapters | Expose stable views instead of domain aggregates |
| Framework annotation | CurrentOperation and AdminOnly | Resolve request context and declare HTTP method security |
| Middleware | RequestIdFilter and LoginRateLimitFilter | Apply request correlation and admission rules before controllers |
| Central error boundary | GlobalExceptionHandler | Map semantic failures to sanitized HTTP responses and log unexpected failures once |

## Transaction semantics

The decorator is the transaction entry point.
A JDBC repository called through a handler participates in the same PostgreSQL transaction.
Read-only handlers use a consistent snapshot for rows and counts.

An ordinary failure rolls back the command.
RefreshReuseException is the one explicit exception: the revocation and audit must commit before the caller receives HTTP 401.
Do not extend that exception rule to other failures.

## Context and authorization

The HTTP resolver obtains the actor from Spring Security after JWT validation.
The middleware supplies a bounded request ID.
A use case receives both as an immutable OperationContext.
It never reads a thread-local security context.

AdminOnly does not replace application permission checks.
Another driver must receive the same authorization behavior through the use case.

## Deliberate omissions

There is no distributed transaction, event broker, cache of availability or generic repository framework.
Approval needs one local ACID transaction, and availability must be rechecked at approval.
Add an outbox only with a real external delivery requirement and its recovery policy.

The system is single-company.
It does not invent tenant identifiers, platform permissions or cloud-specific deployment resources.

## Sources

- [ArchUnit guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Spring method security](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html)

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Describe implemented patterns and their limits |
