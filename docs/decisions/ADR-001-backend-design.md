---
title: 'ADR-001: Backend design decisions'
type: adr
status: accepted
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# ADR-001: Backend design decisions

## Context

Đề bắt buộc Spring Boot 3 và PostgreSQL.
User yêu cầu tổ chức tương tự XTND, có lịch sử commit, task và PR riêng.

## Decisions

| Lựa chọn | Lý do | Đánh đổi |
| --- | --- | --- |
| Java 17 + Spring Boot 3.5.16 | Đúng phiên bản major trong đề | Cần đánh giá support trước môi trường thực tế |
| Maven multi-module | Build độc lập, dependency module rõ ràng | Nhiều source root hơn project một module |
| Modular monolith | Duyệt booking cần transaction ACID | Module cùng deploy |
| JDBC + Flyway | SQL, khóa và exclusion constraint hiện rõ | Viết mapping thủ công |
| PostgreSQL exclusion constraint | Chặn trùng lịch ở database | Phụ thuộc btree_gist và PostgreSQL |
| Pending không giữ phòng | Yêu cầu duyệt thành công trước chiếm lịch | Người gửi có thể bị từ chối lúc duyệt |
| Toàn bộ chuỗi atomic | Không tạo lịch họp chỉ thành công một phần | Một buổi trùng chặn cả lần duyệt |
| JWT + opaque refresh token | API stateless và phiên có thể thu hồi | Access token logout còn sống tối đa 15 phút |
| Slot 1-10 với partial unique index | Giới hạn phòng có bảo vệ database | Slot là chi tiết nội bộ |
| Gitflow với PR phụ thuộc | Review theo nghiệp vụ như XTND | Cần merge theo thứ tự |

## Alternatives

JPA phù hợp CRUD, nhưng bài này phụ thuộc SQL range và kiểm soát khóa cụ thể.
Redis distributed lock không thay thế được ràng buộc database và thêm một thành phần vận hành.
Microservices và broker tăng chi phí triển khai cho một hệ thống nhỏ.
Lưu recurrence vô hạn cần scheduler và xử lý lỗi materialization, vượt nhu cầu phiên bản này.

## Sources

- [Spring Boot release](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/)
- [springdoc for Spring Boot 3](https://springdoc.org/v2/)
- [PostgreSQL range types](https://www.postgresql.org/docs/17/rangetypes.html)
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/)

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Record initial decisions |
