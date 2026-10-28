---
title: 'Operations runbook'
type: runbook
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Operations runbook

## Configuration

| Biến | Mục đích |
| --- | --- |
| DATABASE_URL | JDBC URL khi chạy app trực tiếp |
| DATABASE_NAME | Tên database cho Compose |
| DATABASE_USERNAME, DATABASE_PASSWORD | Kết nối database |
| JWT_SECRET | Ít nhất 32 byte UTF-8, cùng giá trị giữa các instance |
| BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD | Chỉ tạo admin khi database chưa có user |
| DOCS_ENABLED | Tắt Swagger ở môi trường không công khai docs |
| CORS_ALLOWED_ORIGINS | Danh sách origin phân cách bằng dấu phẩy |
| API_PORT, DATABASE_PORT | Port host của Compose |

Compose có giá trị demo để `docker compose up` chạy trực tiếp.
File `.env` nằm trong gitignore.
Dùng secret manager, mật khẩu riêng và TLS khi triển khai ra ngoài local.
JWT secret có thể tạo bằng `openssl rand -hex 32`.

Bootstrap không ghi đè password đã tồn tại.
Sau lần khởi tạo, bỏ bootstrap password khỏi cấu hình triển khai.

## Health và logs

- Liveness: `/actuator/health/liveness`.
- Readiness: `/actuator/health/readiness`, gồm kết nối database.
- Chỉ admin truy cập `/actuator/prometheus` và `/actuator/info`.
- Logs dạng ECS JSON ra stdout, có requestId.
- Không ghi password, access token hoặc refresh token vào log ứng dụng.
- Không bật log request body khi xử lý dữ liệu thật.

Login rate limit là 20 request/IP/phút trong một instance.
Khi có reverse proxy hoặc nhiều replica, cấu hình giới hạn tại ingress.
App không tự tin header Forwarded/X-Forwarded-For của client.

## Khởi chạy và dừng

```bash
docker compose up --build -d --wait
docker compose ps
docker compose logs --tail 100 api
docker compose down
```

`down` giữ volume. Không dùng `down -v` với dữ liệu cần giữ.
Container API chạy non-root, filesystem read-only, chỉ /tmp được ghi.
App shutdown có thời gian để đóng connection và hoàn tất request.

## Backup và restore

Dùng pg_dump định kỳ và thử restore trên database riêng.
Lệnh dưới ghi vào file backup được chọn rõ ràng.

```bash
mkdir -p .local/backups
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > .local/backups/meeting-booking.dump
```

Restore phải dùng một database rỗng riêng. Kiểm tra login, room count và exclusion constraint sau restore.
Không ghi đè database đang hoạt động khi chưa có kế hoạch chuyển đổi.

## Deploy và rollback

1. Chạy CI và container smoke.
2. Chụp backup.
3. Chạy migration với role migrator.
4. Chạy image mới và chờ readiness.
5. Kiểm tra login, tìm phòng và duyệt booking.
6. Nếu lỗi app, chuyển về image trước khi schema còn tương thích.
7. Nếu lỗi schema, dùng migration bù hoặc restore, theo thay đổi cụ thể.

Compose phục vụ local và đánh giá. Repo chưa tạo tài nguyên AWS, DNS hoặc chứng chỉ.
Trước triển khai thật, chọn môi trường, hostname, secret manager, backup và ingress TLS.

## Sự cố thường gặp

| Hiện tượng | Kiểm tra |
| --- | --- |
| JWT_SECRET không hợp lệ | Độ dài ít nhất 32 byte |
| DB khởi động nhưng API chưa ready | Log Flyway, thông tin kết nối, quyền btree_gist |
| Đổi bootstrap password không đổi login | Tài khoản tồn tại, dùng endpoint đổi password |
| 409 lúc duyệt | Yêu cầu khác đã chiếm lịch hoặc version đã đổi |
| 422 lịch lặp | Chủ nhật, giờ ngoài quy định hoặc quá horizon |
| Login 429 sau proxy | Nhiều client có cùng remote address, giới hạn cần đặt tại ingress |

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial operations runbook |
