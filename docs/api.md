---
title: 'REST API reference'
type: reference
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# REST API reference

Base path: `/api/v1`. JSON dùng camelCase. Timestamp cần offset hoặc UTC, ví dụ `2026-11-03T10:00:00+07:00`.
Thay thời gian ví dụ bằng ngày tương lai khi thực hiện.
Swagger tại `/docs` và OpenAPI tại `/v3/api-docs`.

## Endpoint và quyền

| Method | Path | Quyền |
| --- | --- | --- |
| POST | /auth/login | Public |
| POST | /auth/refresh | Public, cần refresh token hợp lệ |
| POST | /auth/logout | Public, cần refresh token để xác định phiên |
| GET | /users/me | Đăng nhập |
| PUT | /users/me/password | Chính user |
| GET | /users/directory | Đăng nhập, chỉ tài khoản active |
| GET, POST | /users | Admin |
| PUT | /users/{id} | Admin |
| GET | /rooms | Đăng nhập |
| GET | /rooms/{id} | Đăng nhập, phòng inactive cần admin |
| POST, PUT | /rooms, /rooms/{id} | Admin |
| GET | /rooms/availability | Đăng nhập |
| GET | /rooms/{id}/calendar | Đăng nhập |
| POST | /bookings | Đăng nhập |
| GET | /bookings | mine/invited, all cần admin |
| GET | /bookings/{id} | Người tạo, được mời hoặc admin |
| PUT | /bookings/{id} | Người tạo hoặc admin |
| POST | /bookings/{id}/approval | Admin |
| POST | /bookings/{id}/rejection | Admin |
| POST | /bookings/{id}/cancellation | Người tạo hoặc admin |
| PUT | /bookings/{id}/invitation-response | Chính người được mời |

## Authentication

Login: `{"email":"admin@example.com","password":"LocalDemo!2026ChangeMe"}`.
Refresh/logout: `{"refreshToken":"<opaque token>"}`.
Đổi password: `{"currentPassword":"...","newPassword":"..."}`.

Login/refresh trả `accessToken`, `refreshToken`, `tokenType`, `expiresIn` và `user`.
Header access: `Authorization: Bearer <accessToken>`.
Token response dùng `Cache-Control: no-store`.

Tạo user cần `email`, `displayName`, `password`, `role` (`user` hoặc `admin`).
Sửa user cần `displayName`, `role`, `isActive` và `version`.
Password dài ít nhất 12 ký tự, nhiều nhất 72 byte UTF-8.

## Room

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

PUT cần thêm `isActive` và `version`.
GET collection nhận `includeInactive=true` cho admin và hỗ trợ `page`, `pageSize`.

## Booking

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

Bỏ `recurrence` hoặc gửi null cho booking một lần.
Attendee list luôn cần có, dùng `[]` khi không mời ai.
PUT cần toàn bộ payload và thêm `version`.
API trả các occurrence và trạng thái từng occurrence.
POST tạo tài nguyên trả 201 cùng header Location.

Duyệt: `{"version":0}`.
Từ chối: `{"version":0,"reason":"Room maintenance"}`.
Hủy: `{"version":0,"reason":"Meeting postponed"}`, reason tùy chọn.
RSVP: `{"response":"accepted"}` hoặc `declined`.

List nhận `scope=mine|invited|all` và `status=pending|approved|rejected|cancelled`.
Mặc định `scope=mine`. Admin dùng `scope=all&status=pending` để xem hàng chờ.

## Availability và calendar

`GET /rooms/availability?startsAt=...&endsAt=...&minCapacity=5&location=HQ&floor=2&equipment=projector,whiteboard`

Thiết bị là điều kiện AND. Capacity tính cả organizer.
Calendar nhận `from`, `to`, `page`, `pageSize`, tối đa 31 ngày.
Chỉ trả room ID và thời gian bận đã duyệt.

Encode ký tự `+` thành `%2B` khi truyền offset trong query, hoặc dùng timestamp UTC với `Z`.

## Envelope và pagination

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

Page bắt đầu từ 1. Page size mặc định 20, tối đa 100.
Áp dụng cho user list, directory, room list, booking list và calendar.
Room list phân trang cả phòng active và archive.

## Errors

| HTTP | Mã tiêu biểu | Nghĩa |
| --- | --- | --- |
| 400 | Validation.InvalidRequest | JSON, UUID hoặc input shape sai |
| 401 | Authentication.Required / InvalidCredentials | Chưa đăng nhập, token không hợp lệ |
| 403 | Authorization.Forbidden | Thiếu quyền |
| 404 | Resource.NotFound | Không tồn tại |
| 409 | Concurrency.StaleVersion | Version cũ |
| 409 | Booking.ScheduleConflict | Trùng lịch, có gợi ý |
| 409 | Room.LimitReached | Đã có 10 phòng hoạt động |
| 422 | BusinessRule.Violated | Vi phạm quy tắc nghiệp vụ |
| 429 | Authentication.RateLimited | Quá 20 lần login/IP/phút |
| 500 | Internal.Unexpected | Lỗi bất ngờ, không lộ nội bộ |

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
