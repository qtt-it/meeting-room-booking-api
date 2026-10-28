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

Một công ty với khoảng 10 phòng không cần microservices hoặc message broker.
Modular monolith giữ transaction duyệt phòng trong một database và một tiến trình.

## Module và trách nhiệm

| Module | Trách nhiệm | Phụ thuộc nghiệp vụ |
| --- | --- | --- |
| foundation | Actor, lỗi có mã, envelope, request ID | Không |
| identity | Tài khoản, password và phiên đăng nhập | foundation |
| room | Danh mục phòng, giới hạn phòng, metadata | foundation |
| booking | Booking, lịch lặp, duyệt, lời mời, calendar, tìm phòng | identity, room, foundation |
| meeting-api | Compose module, security filter, config, migration | Các module trên |

Mỗi module nghiệp vụ có `domain`, `application`, `infra/db` và `infra/http`, như cách XTND tổ chức thư viện.
Maven build-helper khai báo các source root. Domain không import JDBC, Spring MVC hoặc repository adapter.

## Luồng request

Security filter xác thực JWT, đọc trạng thái tài khoản hiện tại và tạo Actor.
Controller kiểm tra shape và giới hạn input.
Application service kiểm tra quyền, lấy dữ liệu, gọi domain policy và lưu trong transaction.
Repository adapter thực hiện câu SQL có tham số.
Global exception handler ghi lỗi một lần và trả error envelope.

Chỉ composition root cấu hình token secret và các dependency toàn ứng dụng.
Clock được inject để kiểm tra thời gian trong test.

## Duyệt cạnh tranh

1. Khóa row booking bằng `FOR UPDATE`.
2. Kiểm tra role, trạng thái pending và version.
3. Khóa row phòng bằng `FOR UPDATE`.
4. Đọc lại tất cả lịch đã duyệt giao thời gian.
5. Nếu trùng, trả 409 và gợi ý từ dữ liệu hiện tại.
6. Đổi trạng thái header và tất cả occurrence trong cùng transaction.
7. PostgreSQL kiểm tra exclusion constraint trước khi commit.

Các lần duyệt cùng phòng tuần tự hóa tại row phòng.
Các phòng khác vẫn có thể duyệt độc lập.
Room update dùng cùng khóa phòng nên không thể đổi sức chứa giữa lúc kiểm tra và duyệt.

## Giới hạn phòng

Room catalog có mutex row trong `system_lock`.
Create/reactivate khóa row này rồi cấp một slot còn trống từ 1 đến 10.
Partial unique index trên slot đang hoạt động bảo vệ giới hạn ở database.

## Identity

JWT dùng HS256, issuer và audience cố định. Access token hết hạn sau 15 phút.
Refresh token là 32 byte ngẫu nhiên, lưu SHA-256 hash và hết hạn sau bảy ngày.
Refresh khóa session, thu hồi token cũ và cấp token tiếp theo trong cùng family.
Refresh reuse thu hồi cả family trong transaction được commit dù API trả 401.

API đọc lại tài khoản và security version mỗi request.
Disable có hiệu lực ngay. Đổi password tăng security version và thu hồi các refresh session.
Logout thu hồi refresh family. Access token đã phát vẫn sống đến hạn.

## Trade-offs

Các occurrence được tạo ngay, giới hạn 52 buổi. Không có scheduler materialize vô hạn.
JDBC giữ SQL và khóa transaction rõ ràng. Flyway là nguồn schema duy nhất.
List booking tải child records theo từng booking. Page tối đa 100, phù hợp quy mô đề.
Có thể chuyển sang truy vấn batch nếu số booking tăng.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial architecture |
