---
title: Git workflow
type: guide
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Git workflow

## Branches

main contains the delivered version. develop integrates reviewed business changes.
Each story uses a feature/MB-NNN-title branch.
Merge prerequisite work before opening the next feature against develop.

## Commits

Use focused Conventional Commits, such as feat(identity): add rotating refresh sessions.
Stage explicit files and run relevant checks before committing.
Use merge commits to preserve the assessment history.

## Review and merge

Each pull request links its story and issue.
Describe the behavior, review focus, verification, migration impact and rollback.
Run the structured review, resolve blocking findings and wait for the current head checks.
Merge only the reviewed head. Do not bypass branch protections.
The repository owner authorizes this delivery workflow to create, review and merge the implementation pull requests.
Promote develop to main after complete verification.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial review and delivery workflow |
