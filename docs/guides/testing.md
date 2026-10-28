---
title: 'Testing guide'
type: guide
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Testing guide

## Prerequisites

Cần Java 17 và PostgreSQL riêng cho test.
Test tích hợp chỉ chạy khi database tên chính xác `meeting_booking_test`, vì fixture xóa dữ liệu giữa các test.

## Commands

```bash
./mvnw verify
docker compose -f infra/compose.test.yml up -d --wait
./mvnw verify -Pintegration
docker compose -f infra/compose.test.yml down
```

Override kết nối bằng `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD`.
Mặc định dùng PostgreSQL tại localhost:55432.
Test không dùng H2 vì H2 không kiểm tra được GiST, row lock và exclusion constraint.

## Coverage theo hành vi

| Layer | Nội dung |
| --- | --- |
| Unit | Nửa khoảng thời gian, giờ hoạt động, Chủ nhật, horizon, recurrence |
| HTTP foundation | Request ID an toàn và dọn MDC |
| Integration | Login, refresh, logout, password change, RBAC, room limit |
| Booking integration | Pending, approval race, rollback chuỗi, version, capacity, invitation |
| Database | Exclusion constraint kể cả khi ghi SQL trực tiếp |
| Container smoke | Health, login, profile và room list sau Compose startup |
| API collection | 25 request chạy theo thứ tự trên stack Docker riêng: auth, room, recurring booking, approval, RSVP, cancellation, rejection |

Tests chạy bằng Surefire cho `*Test`, Failsafe cho `*IT` khi bật profile `integration`.
Không dùng test transaction bao ngoài request vì cần quan sát commit thật.

## Kết quả local

Đã chạy unit và integration test trên Java 17 với PostgreSQL 14.22 tạm, vì Docker daemon local chưa chạy.
Kết quả local: 24 unit test, 28 integration test và 25 request Bruno đều đạt.
CI dùng PostgreSQL 17, đúng image của Compose.
Kết quả CI và container smoke cần đọc trực tiếp tại PR, không suy luận từ unit test.

## Reports

## Delivery evidence

[Full CI run](https://github.com/qtt-it/meeting-room-booking-api/actions/runs/40642049729) kiểm tra commit `0cac3f0`:

- Java 17 và PostgreSQL 17: 24 unit test và 28 integration test.
- Build image thật, Compose startup và readiness.
- 25 request Bruno, gồm login/refresh/logout, user, room, booking lặp, duyệt, RSVP, hủy và từ chối.
- Test report và API collection report được giữ trong 14 ngày.

Commit tài liệu sau đó cũng phải qua CI ở chính head của PR trước khi merge.

## Report locations

Đọc `target/surefire-reports` và `target/failsafe-reports` trong từng module.
CI lưu report trong artifact trong 14 ngày.
Report Bruno loại bỏ header và request/response body để tránh lưu token hoặc password.
Job container dùng database riêng, không kết nối hệ thống bên ngoài.
Action và CLI được pin phiên bản; Dependabot đề xuất cập nhật.
Khi thêm endpoint hoặc rule, thêm test hồi quy và request Bruno cùng thay đổi.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial testing guide |
