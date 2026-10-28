---
title: 'PostgreSQL data model'
type: reference
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# PostgreSQL data model

Schema do Flyway quản lý trong `apps/meeting-api/src/main/resources/db/migration/`.
Timestamp dùng `timestamptz`, ID dùng UUID, tên bảng/cột dùng `snake_case`.

## Bảng

| Bảng | Mục đích | Khóa/quan hệ |
| --- | --- | --- |
| system_lock | Tuần tự hóa thao tác catalog | identity, room |
| user_account | Tài khoản, role, BCrypt hash, version | Email chuẩn hóa unique |
| refresh_session | Phiên refresh xoay vòng | user, family, hash unique |
| refresh_family | Serialize refresh, reuse and logout | User and revocation timestamp |
| activity_audit | Account, session and room mutations | Actor, action, resource, timestamp |
| room | Tên, địa điểm, tầng, sức chứa, thiết bị | Unique slot/name khi active |
| booking | Header chuỗi, organizer, trạng thái, recurrence | Room và user |
| booking_occurrence | Từng buổi trong chuỗi | Composite FK giữ room khớp header |
| booking_attendee | Người được mời, phản hồi | PK booking + user |
| booking_audit | Actor, action, lý do, timestamp | Booking |

Occurrence và attendee thuộc booking. Audit và refresh session là các record lịch sử, không dùng optimistic version riêng.
Room và user dùng điều kiện `WHERE version = :expected`.
Booking khóa row rồi so version trước khi cập nhật.

## Chống trùng

```sql
EXCLUDE USING gist (
  room_id WITH =,
  tstzrange(starts_at, ends_at, '[)') WITH &&
) WHERE (status = 'approved')
```

Extension `btree_gist` hỗ trợ equality cho UUID cùng range trong GiST.
Pending không nằm trong constraint.
Calendar và availability chỉ đọc occurrence approved.

Không dùng `SELECT COUNT` rồi insert làm bảo vệ duy nhất cho booking.
Transaction cần giữ ràng buộc đúng cả khi có nhiều instance API.

## Index

- Email và refresh token hash để xác thực.
- Refresh family/user để thu hồi phiên.
- Room slot/name active để kiểm soát catalog.
- Organizer/status + created time cho list.
- Occurrence booking để tải chuỗi.
- Room + starts time cho calendar.
- Attendee user để liệt kê lời mời.
- Audit booking + created time để truy vết.

## Migration và triển khai

Database mới chạy migration trước khi bootstrap admin.
Không sửa migration đã áp dụng. Tạo version mới cho thay đổi sau đó.
Compose dùng role sở hữu database cho demo. Hạ tầng thật nên tách migrator và runtime role.
Migrator cần quyền cài `btree_gist`, hoặc DBA cài extension trước.

Flyway `clean` bị vô hiệu hóa.
Sau khi ghi dữ liệu, rollback bằng application image trước đó nếu schema còn tương thích.
Thay đổi phá hủy cần migration bù hoặc restore bản backup, xem runbook.

## Nguồn

[PostgreSQL range constraints](https://www.postgresql.org/docs/17/rangetypes.html#RANGETYPES-CONSTRAINT) mô tả ràng buộc chống giao khoảng thời gian.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial model and concurrency constraints |
