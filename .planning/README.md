# Delivery backlog

Tổ chức story và task theo cách XTND. Mỗi story ghi acceptance criteria, task, bằng chứng test và review còn lại.

| Story | Branch | Business scope |
| --- | --- | --- |
| [MB-001](backlog/stories/MB-001.md) | feature/MB-001-project-foundation | Project foundation |
| [MB-002](backlog/stories/MB-002.md) | feature/MB-002-identity-and-access | Employee identity and access |
| [MB-003](backlog/stories/MB-003.md) | feature/MB-003-room-management | Room catalog and capacity |
| [MB-004](backlog/stories/MB-004.md) | feature/MB-004-booking-and-recurrence | Booking requests and recurrence |
| [MB-005](backlog/stories/MB-005.md) | feature/MB-005-booking-approval | Approval and cancellation |
| [MB-006](backlog/stories/MB-006.md) | feature/MB-006-invitations-and-availability | Invitations and alternative rooms |
| [MB-007](backlog/stories/MB-007.md) | feature/MB-007-devops-and-documentation | DevOps and handoff documentation |

PR phụ thuộc theo thứ tự MB-001 → MB-002 → MB-003 → MB-004 → MB-005 → MB-006 → MB-007.
PR đầu vào develop, PR tiếp theo dựa trên feature trước để diff chỉ chứa phần mới.
Sau khi merge PR trước, retarget PR tiếp theo sang develop.
Giữ từng commit. Review và merge do chủ repo thực hiện.

## Definition of done

- Acceptance criteria được kiểm tra.
- Source và test cùng commit nghiệp vụ.
- Migration và docs cập nhật khi có thay đổi.
- PR ghi bằng chứng kiểm tra và giới hạn.
- Code review hoàn tất trước merge.
