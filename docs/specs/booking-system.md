---
title: 'Meeting room booking specification'
type: spec
status: implemented
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Meeting room booking specification

## Context

The assessment requires room management, booking, calendars and availability through a REST API.
Required technologies are Java 17+, Spring Boot 3, PostgreSQL and Docker Compose.

## Confirmed rules

| Topic | Rule |
| --- | --- |
| Approval | An administrator must approve every booking |
| Priority | The first successful approval reserves the room |
| Edit/cancel | The organizer and administrators can perform these actions |
| Room limit | At most ten active rooms |
| Recurrence | Supported |
| Participants | Invite specific employee accounts |
| Business hours | Monday-Saturday, 09:00-18:00, Vietnam time |
| Overlap protection | Approved occurrences cannot overlap in the same room |
| Search | Capacity, location, floor, equipment and time |
| Authentication | Login, refresh, logout and user/admin roles |
| Conflicts | Reject the operation and return alternatives |

The approximate scale is 200 employees. The application does not impose a hard employee limit.

## Additional implementation choices

These choices resolve details that the assessment does not specify.

- Only administrators create internal accounts. Self-registration and email password recovery are outside this version.
- Administrators may approve their own bookings.
- Cancellation has no advance-notice requirement beyond a future occurrence start.
- Times use whole minutes, with a minimum duration of one minute within one day.
- Windows are half-open: `[startsAt, endsAt)`. 10:00-11:00 and 11:00-12:00 do not overlap.
- There are no buffers or public-holiday rules. Sunday is closed.
- Daily recurrence counts Monday-Saturday business days. Weekly recurrence retains the weekday and local time.
- The interval is 1-12 and the count is 2-52. The last occurrence must end within 366 days.
- Approval covers the entire series. One conflict leaves the whole request pending without partial reservations.
- Submission and approval both check conflicts against approved occurrences.
- Edits replace the entire series before its first start. Editing an approved booking releases its reservations and requires approval again.
- Cancellation changes only occurrences that have not started. Past and ongoing occurrences retain their history.
- An invitation alone never grants edit, cancel or approval permission.
- Capacity includes the organizer and all invitees, including invitees who decline.
- One invitation response applies to the whole series. Personal employee calendar conflicts are not checked.
- Email, push notifications, monthly recurrence and single-occurrence edits are outside this version.
- The API does not physically delete rooms, accounts or bookings.

## States and actions

| State | Action | Result |
| --- | --- | --- |
| pending | Administrator approves | approved only if the entire series is available |
| pending | Administrator rejects with a reason | rejected |
| pending | Organizer/administrator edits | pending, version increments |
| approved | Organizer/administrator edits before the first start | pending, version increments, previous reservations are released |
| pending or approved | Organizer/administrator cancels | cancelled, future occurrences are canceled |
| rejected or cancelled | Edit or approve | HTTP 422 |

Booking mutations record audit events in the same transaction.
Edit, approval, rejection and cancellation require the current version.
An outdated version returns HTTP 409.

## Recommendations

The API first proposes up to three other rooms with sufficient capacity and all original room equipment.
It then searches later times in 15-minute steps, up to 180 minutes later on the same day.
It returns at most five suggestions. Every suggestion must fit the entire series.

The list may be empty when no suitable alternative exists.
Suggestions do not reserve rooms.
The client submits or edits a booking, then requests approval.

## Privacy

Employees can read active rooms and approved busy periods.
The public calendar omits titles, descriptions, attendees and organizer identities.
Only the organizer, invited employees and administrators can read booking details.
The employee directory returns active accounts and the information needed for invitations.

## Acceptance criteria

1. Overlapping pending requests can coexist.
2. Concurrent approval of the same room and time produces only one success.
3. A failed series approval never retains partial reservations.
4. Adjacent meetings and simultaneous meetings in different rooms are valid.
5. Employees cannot approve bookings or edit another organizer's booking.
6. Requests outside Monday-Saturday or 09:00-18:00 return HTTP 422.
7. Creating or activating an eleventh room fails.
8. Refresh tokens are single-use. Reuse revokes the entire family.
9. API responses omit password hashes. The database stores password hashes only.
10. Docker Compose starts the API and PostgreSQL with healthchecks.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Record confirmed requirements and implementation choices |
| 2026-10-28 | @qtt-it | Standardize all specification prose in English |
