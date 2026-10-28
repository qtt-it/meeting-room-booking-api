---
title: 'Git workflow'
type: guide
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Git workflow

## Branches

`main` là nhánh phát hành. `develop` là nhánh tích hợp.
Mỗi story có branch `feature/MB-NNN-title`.
PR chỉ chứa thay đổi của nghiệp vụ tương ứng so với base đã nêu.

Các PR đầu dựa nhau vì repo mới chưa có nền tảng.
Merge theo thứ tự trong [.planning/README.md](../../.planning/README.md).
Sau khi merge PR trước, retarget PR tiếp theo sang develop và kiểm tra diff.
Không merge tự động, không squash lịch sử coding test.

## Commits

Dùng Conventional Commits, ví dụ `feat(identity): add rotating refresh sessions`.
Mỗi commit chứa một hành vi hoặc một thay đổi nền tảng có thể kiểm tra.
Stage theo tên file. Chạy test liên quan và kiểm tra staged diff trước commit.

## PR

Liên kết story hoặc GitHub issue.
Ghi hành vi sau thay đổi, điểm review, test thực tế, giới hạn và migration.
Các lựa chọn tồn tại lâu dài nằm trong docs/spec hoặc ADR.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial Gitflow and review process |
