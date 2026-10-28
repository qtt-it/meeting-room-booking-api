---
title: 'Backend architecture'
type: architecture
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Backend architecture

## Context

A modular monolith serves approximately 200 employees and 10 rooms.
Approval requires one ACID transaction. Separate services or a message broker are unnecessary at this scale.

## Modules

| Module | Responsibility | Dependencies |
| --- | --- | --- |
| foundation | Actor, errors, response envelope, request correlation | None |
| identity | Accounts, passwords, sessions | foundation |
| room | Catalog, active room limit, metadata | foundation |
| booking | Requests, recurrence, approval, invitations, availability | identity, room, foundation |
| meeting-api | Composition, security, configuration, migrations | Feature modules |

Feature modules contain domain, application, database adapter and HTTP adapter source roots.
Maven build-helper registers these roots.
Domain code does not import JDBC, Spring MVC or database adapters.

## Request flow

Security validates the JWT, reads the current account and creates an Actor.
Controllers validate request shape and delegate to application services.
Services check authorization, execute domain rules and persist changes in transactions.
Adapters execute parameterized SQL. One exception handler records failures and returns the error envelope.
An injected Clock makes time rules deterministic in tests.

## Concurrent approvals

1. Lock the booking row with FOR UPDATE.
2. Check the administrator role, pending state and expected version.
3. Lock the room row with FOR UPDATE.
4. Read approved occurrences that overlap the requested windows.
5. Return 409 and alternatives on conflict.
6. Update the header and all occurrences in one transaction.
7. Let PostgreSQL enforce the exclusion constraint before commit.

Approvals for the same room serialize. Different rooms remain independent.
Room updates use the same room lock, so capacity cannot change between validation and approval.

## Room limit

Catalog changes lock a row in system_lock.
Create and reactivate allocate a free slot from 1 through 10.
A partial unique index on active slots also enforces the limit in the database.

The room module declares a RoomUsage port.
The booking module implements it to prevent room changes that invalidate approved reservations.

## Sessions

JWT access tokens use HS256 with issuer, audience and expiration validation.
Access tokens expire after 15 minutes.
Refresh tokens contain 32 random bytes. The database stores SHA-256 hashes and a seven-day expiry.
Sessions also record the account security version.

A family row serializes rotation, reuse and logout.
Refresh then locks the session, revokes the old token and issues its replacement in the same family.
Reuse revokes the whole family. That transaction commits despite the 401 response.
Every authenticated request rechecks the active account and security version.
Password changes increment the security version and revoke refresh sessions.
Logout revokes the refresh family. Existing access tokens retain their original expiration.

## Trade-offs

The service materializes at most 52 occurrences when a request is submitted.
JDBC makes SQL and locks explicit. Flyway owns schema changes.
Booking pages batch-load occurrences and attendees for up to 100 headers.
The query count stays constant as the page grows.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial architecture |
