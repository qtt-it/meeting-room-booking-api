---
title: 'REST API reference'
type: reference
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# REST API reference

The base path is `/api/v1`. JSON uses camelCase.
Timestamps require an offset or UTC, for example `2026-11-03T10:00:00+07:00`.
Use a future business date when you run the examples.
Swagger is at `/docs`. OpenAPI is at `/v3/api-docs`.
The prod profile disables these public documentation endpoints.
API request bodies are limited to 64 KiB, including unknown-length streams.

## Endpoints and permissions

| Method | Path | Permission |
| --- | --- | --- |
| POST | /auth/login | Public |
| POST | /auth/refresh | Public, requires a valid refresh token |
| POST | /auth/logout | Public, the refresh token identifies the session |
| GET | /users/me | Authenticated employee |
| PUT | /users/me/password | Account owner |
| GET | /users/directory | Authenticated employee, active accounts only |
| GET, POST | /users | Administrator |
| PUT | /users/{id} | Administrator |
| GET | /rooms | Authenticated employee |
| GET | /rooms/{id} | Authenticated employee, inactive rooms require an administrator |
| POST, PUT | /rooms, /rooms/{id} | Administrator |
| GET | /rooms/availability | Authenticated employee |
| GET | /rooms/{id}/calendar | Authenticated employee |
| POST | /bookings | Authenticated employee |
| GET | /bookings | mine/invited, all requires an administrator |
| GET | /bookings/{id} | Organizer, invitee or administrator |
| PUT | /bookings/{id} | Organizer or administrator |
| POST | /bookings/{id}/approval | Administrator |
| POST | /bookings/{id}/rejection | Administrator |
| POST | /bookings/{id}/cancellation | Organizer or administrator |
| PUT | /bookings/{id}/invitation-response | Invited employee |

## Authentication

Login: `{"email":"admin@example.com","password":"LocalDemo!2026ChangeMe"}`.
Refresh/logout: `{"refreshToken":"<opaque token>"}`.
Password change: `{"currentPassword":"...","newPassword":"..."}`.

Login and refresh return `accessToken`, `refreshToken`, `tokenType`, `expiresIn` and `user`.
Send `Authorization: Bearer <accessToken>`.
Token responses set `Cache-Control: no-store`.

Account creation requires `email`, `displayName`, `password` and `role` (`user` or `admin`).
Account updates require `displayName`, `role`, `isActive` and `version`.
Passwords require at least 12 characters and at most 72 UTF-8 bytes.

## Rooms

```json
{
  "name": "Lotus",
  "description": "Meeting room",
  "location": "HQ",
  "floor": "2",
  "capacity": 12,
  "equipment": ["projector", "whiteboard"]
}
```

PUT also requires `isActive` and `version`.
The collection supports `page` and `pageSize`.
Administrators can request `includeInactive=true`.

## Bookings

```json
{
  "roomId": "<room UUID>",
  "title": "Weekly planning",
  "description": "Team meeting",
  "startsAt": "2026-11-03T10:00:00+07:00",
  "endsAt": "2026-11-03T11:00:00+07:00",
  "attendeeIds": ["<employee UUID>"],
  "recurrence": {"frequency": "weekly", "interval": 1, "count": 4}
}
```

Omit `recurrence` or send null for a single meeting.
Always provide `attendeeIds`. Use `[]` when there are no invitees.
PUT requires the full payload and the current `version`.
Responses include each occurrence and its status.
Resource creation returns HTTP 201 with a Location header.

Approval: `{"version":0}`.
Rejection: `{"version":0,"reason":"Room maintenance"}`.
Cancellation: `{"version":0,"reason":"Meeting postponed"}`. The reason is optional.
RSVP: `{"response":"accepted"}` or `{"response":"declined"}`.

Lists accept `scope=mine|invited|all` and `status=pending|approved|rejected|cancelled`.
The default scope is `mine`. Administrators use `scope=all&status=pending` for the approval queue.

## Availability and calendars

`GET /rooms/availability?startsAt=...&endsAt=...&minCapacity=5&location=HQ&floor=2&equipment=projector,whiteboard`

Equipment filters use AND semantics. Capacity includes the organizer.
Calendars accept `from`, `to`, `page` and `pageSize`, with a maximum range of 31 days.
They return only room IDs and approved busy periods.

Encode `+` as `%2B` in query offsets, or use UTC timestamps with `Z`.

## Response envelope and pagination

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": [],
  "meta": {
    "requestId": "request-123",
    "timestamp": "2026-10-28T00:00:00Z",
    "pagination": {"page": 1, "pageSize": 20, "totalItems": 0, "totalPages": 0}
  }
}
```

Pages start at 1. The default page size is 20, with a maximum of 100.
Pagination applies to accounts, the directory, rooms, bookings and calendars.
The room catalog paginates active and archived rooms.

## Errors

| HTTP | Example code | Meaning |
| --- | --- | --- |
| 400 | Validation.InvalidRequest | Invalid JSON, UUID or request shape |
| 401 | Authentication.Required / Authentication.InvalidCredentials | Missing authentication or invalid credentials |
| 403 | Authorization.Forbidden | Permission denied |
| 404 | Resource.NotFound | Resource does not exist |
| 409 | Concurrency.StaleVersion | Outdated version |
| 409 | Booking.ScheduleConflict | Schedule conflict, with alternatives |
| 409 | Room.LimitReached | Ten active rooms already exist |
| 422 | BusinessRule.Violated | Business rule violation |
| 429 | Authentication.RateLimited | More than 20 login attempts per IP per minute |
| 413 | Request.PayloadTooLarge | The API body exceeds 64 KiB, checked before authentication |
| 500 | Internal.Unexpected | Unexpected failure, without internal details |

```json
{
  "success": false,
  "message": "The room is unavailable for one or more occurrences",
  "error": {
    "code": "Booking.ScheduleConflict",
    "message": "The room is unavailable for one or more occurrences",
    "details": {
      "unavailableOccurrences": [{"startsAt":"...","endsAt":"..."}],
      "suggestions": [{"type":"other_room","roomId":"...","roomName":"Lotus","occurrences":[]}]
    }
  },
  "meta": {"requestId":"...","timestamp":"..."}
}
```

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial API contract |
| 2026-10-28 | @qtt-it | Standardize all reference prose in English |
| 2026-10-28 | @qtt-it | Document the body limit and production documentation policy |
