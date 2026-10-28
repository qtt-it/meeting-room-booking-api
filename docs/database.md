---
title: 'PostgreSQL data model'
type: reference
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# PostgreSQL data model

Flyway manages the schema in `apps/meeting-api/src/main/resources/db/migration/`.
Timestamps use `timestamptz`. IDs use UUIDs. Table and column names use `snake_case`.

## Tables

| Table | Purpose | Keys and relations |
| --- | --- | --- |
| system_lock | Serialize catalog changes | identity, room |
| user_account | Account, role, BCrypt hash and version | Unique normalized email |
| refresh_session | Rotating refresh session | User, family and unique token hash |
| refresh_family | Serialize refresh, reuse and logout | User and revocation timestamp |
| activity_audit | Account, session and room mutations | Actor, action, resource and timestamp |
| room | Name, location, floor, capacity and equipment | Unique active slot and name |
| booking | Series header, organizer, status and recurrence | Room and user |
| booking_occurrence | Individual meeting in a series | Composite foreign key keeps its room consistent with the header |
| booking_attendee | Invited employee and response | Primary key: booking and user |
| booking_audit | Actor, action, reason and timestamp | Booking |

Occurrences and attendees belong to a booking.
Audit and refresh session records do not have separate optimistic versions.
Room and account updates use `WHERE version = :expected`.
Booking updates lock the row and compare its version before a mutation.

## Overlap protection

```sql
EXCLUDE USING gist (
  room_id WITH =,
  tstzrange(starts_at, ends_at, '[)') WITH &&
) WHERE (status = 'approved')
```

The `btree_gist` extension supports UUID equality alongside time ranges in a GiST index.
Pending occurrences are excluded from this constraint.
Calendar and availability queries read approved occurrences only.

A count followed by an insert is not sufficient protection.
Transactions and database constraints must protect the rule across all API instances.

## Indexes

- Email and refresh token hash support authentication.
- Refresh family and user support session revocation.
- Active room slot and name enforce catalog limits.
- Organizer/status and creation time support booking lists.
- Booking occurrence foreign keys support series reads.
- Room and start time support calendars.
- Attendee user supports invitation lists.
- Booking audit and creation time support investigations.

## Migrations and deployment

Migrations run before the initial administrator bootstrap.
Never edit an applied migration. Add a new version for later changes.
The demo Compose stack uses the database owner.
Production infrastructure must separate migration and runtime roles.
The migration role needs permission to install `btree_gist`, or a database administrator must install it first.

Flyway `clean` is disabled.
Restore the previous application image only while the schema remains compatible.
Destructive changes require a compensating migration or a verified backup restore.
See the [operations runbook](runbooks/operations.md).

## Sources

[PostgreSQL range constraints](https://www.postgresql.org/docs/17/rangetypes.html#RANGETYPES-CONSTRAINT) describe database-level overlap protection.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial model and concurrency constraints |
| 2026-10-28 | @qtt-it | Standardize all data model prose in English |
