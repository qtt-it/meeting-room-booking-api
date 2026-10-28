---
title: 'Meeting room booking specification'
type: spec
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Meeting room booking specification

## Context

Đề DKI yêu cầu REST API quản lý phòng, đặt phòng, xem lịch và tìm phòng trống.
Công nghệ bắt buộc là Java 17+, Spring Boot 3, PostgreSQL và Docker Compose.

## Quy tắc được xác nhận

| Chủ đề | Quy tắc |
| --- | --- |
| Duyệt | Booking cần admin duyệt |
| Thứ tự | Yêu cầu được duyệt thành công trước chiếm phòng |
| Sửa/hủy | Người tạo và admin được thực hiện |
| Số phòng | Tối đa 10 phòng đang hoạt động |
| Lịch lặp | Có hỗ trợ |
| Người tham gia | Mời tài khoản nhân viên cụ thể |
| Giờ hoạt động | Thứ Hai-thứ Bảy, 09:00-18:00, giờ Việt Nam |
| Chống trùng | Không cho hai booking đã duyệt trùng phòng và giao thời gian |
| Tìm kiếm | Sức chứa, địa điểm, tầng, thiết bị và thời gian |
| Auth | Đăng nhập, refresh, logout và phân quyền user/admin |
| Xung đột | Từ chối cùng gợi ý thay thế |

Không giới hạn cứng số nhân viên ở 200. Đây là quy mô mô tả trong đề.

## Lựa chọn bổ sung cho phiên bản này

Những lựa chọn dưới đây cụ thể hóa các điểm đề chưa quy định.

- Chỉ admin tạo tài khoản nội bộ. Không có tự đăng ký hoặc khôi phục password qua email.
- Admin được duyệt booking của chính mình.
- Không áp dụng thời hạn báo trước khi hủy, ngoài điều kiện buổi họp chưa bắt đầu.
- Thời gian theo phút, tối thiểu một phút và trong cùng ngày.
- Khoảng thời gian là `[startsAt, endsAt)`. 10:00-11:00 và 11:00-12:00 không trùng.
- Không có buffer hoặc lịch nghỉ lễ. Chủ nhật đóng cửa.
- Lặp `daily` đếm ngày làm việc T2-T7. Lặp `weekly` giữ nguyên thứ và giờ.
- `interval` từ 1 đến 12, `count` từ 2 đến 52. Buổi cuối phải trong 366 ngày.
- Admin duyệt cả chuỗi. Một buổi trùng làm toàn bộ lần duyệt thất bại, chuỗi vẫn `pending`.
- API kiểm tra xung đột với lịch đã duyệt cả khi gửi yêu cầu và khi duyệt.
- Sửa toàn bộ chuỗi trước buổi đầu. Sửa booking đã duyệt giải phóng lịch cũ và yêu cầu duyệt lại.
- Hủy chuỗi chỉ hủy buổi chưa bắt đầu. Giữ nguyên lịch sử và buổi đang diễn ra.
- Thành viên tham gia không được sửa, hủy hoặc duyệt chỉ vì nhận lời mời.
- Tính sức chứa gồm người tổ chức và tất cả người được mời, kể cả người từ chối.
- Lời mời chung cho cả chuỗi. Không kiểm tra xung đột lịch cá nhân của người tham gia.
- Chưa có email, thông báo đẩy, monthly recurrence hoặc chỉnh riêng một buổi trong chuỗi.
- Không xóa vật lý phòng, tài khoản hoặc booking qua API.

## Trạng thái và hành động

| Trạng thái | Hành động | Kết quả |
| --- | --- | --- |
| pending | Admin duyệt | approved nếu cả chuỗi còn trống |
| pending | Admin từ chối, có lý do | rejected |
| pending | Chủ booking/admin sửa | pending, tăng version |
| approved | Chủ booking/admin sửa trước buổi đầu | pending, tăng version, bỏ lịch duyệt cũ |
| pending hoặc approved | Chủ booking/admin hủy | cancelled, hủy các buổi tương lai |
| rejected hoặc cancelled | Sửa hoặc duyệt | 422 |

Mọi thay đổi booking ghi audit cùng transaction.
Hành động sửa, duyệt, từ chối và hủy cần `version` hiện tại. Version cũ nhận `409`.

## Gợi ý

API ưu tiên tối đa ba phòng khác đủ chỗ và có toàn bộ thiết bị của phòng ban đầu.
Sau đó tìm giờ muộn hơn trong cùng ngày, theo bước 15 phút, tối đa 180 phút.
Tổng số gợi ý tối đa năm. Mỗi gợi ý phải thỏa toàn bộ chuỗi.

Gợi ý có thể rỗng khi không còn lựa chọn phù hợp. Gợi ý không giữ phòng.
Client gửi booking mới hoặc sửa booking rồi xin duyệt.

## Quyền riêng tư

Tất cả nhân viên xem phòng và khoảng bận đã duyệt.
Lịch công khai không chứa tiêu đề, nội dung, danh sách người tham gia hoặc người tổ chức.
Chỉ chủ booking, người được mời và admin đọc chi tiết booking.
User xem directory chỉ nhận tài khoản đang hoạt động và thông tin cần để mời.

## Acceptance criteria

1. Hai booking pending giao lịch có thể cùng tồn tại.
2. Duyệt đồng thời hai booking trùng phòng chỉ có một lần thành công.
3. Duyệt chuỗi thất bại không giữ lại một phần lịch.
4. Buổi liền kề nhau và cùng giờ ở hai phòng khác nhau đều hợp lệ.
5. User không duyệt booking và không sửa booking của người khác.
6. Booking ngoài T2-T7 hoặc 09:00-18:00 nhận 422.
7. Không tạo hoặc kích hoạt phòng thứ 11.
8. Refresh token chỉ được dùng một lần. Dùng lại thu hồi cả family.
9. Password không xuất hiện trong API và database chỉ giữ password hash.
10. Docker Compose khởi chạy API và PostgreSQL cùng healthcheck.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Record confirmed requirements and implementation choices |
