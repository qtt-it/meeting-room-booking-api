# Delivery backlog

Each story records acceptance criteria, implementation tasks, verification and review status.

| Story | Branch | Scope |
| --- | --- | --- |
| [MB-001](backlog/stories/MB-001.md) | feature/MB-001-project-foundation | Project foundation |
| [MB-002](backlog/stories/MB-002.md) | feature/MB-002-identity-and-access | Employee identity and access |
| [MB-003](backlog/stories/MB-003.md) | feature/MB-003-room-management | Room catalog and capacity |
| [MB-004](backlog/stories/MB-004.md) | feature/MB-004-booking-and-recurrence | Booking requests and recurrence |
| [MB-005](backlog/stories/MB-005.md) | feature/MB-005-booking-approval | Approval and cancellation |
| [MB-006](backlog/stories/MB-006.md) | feature/MB-006-invitations-and-availability | Invitations and alternative rooms |
| [MB-007](backlog/stories/MB-007.md) | feature/MB-007-devops-and-documentation | DevOps and handoff documentation |

Integrate MB-001 through MB-007 in order.
Each feature targets develop after its prerequisite is merged.
Use merge commits to preserve individual changes.
Promote develop to main after the complete verification suite passes.

## Definition of done

- Verify acceptance criteria with relevant checks.
- Include code and tests in the same business commit.
- Update migrations and documentation with behavior changes.
- Record findings, fixes and verification in the pull request.
- Complete the structured review before merging.
