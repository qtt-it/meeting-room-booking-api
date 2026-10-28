---
title: 'Engineering and infrastructure conventions'
type: reference
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Engineering and infrastructure conventions

Use apps for application composition and libs for feature modules.
Separate domain rules, application orchestration, database adapters and HTTP adapters.
Keep business stories, acceptance criteria and API examples under version control.

## Infrastructure

| Area | Convention |
| --- | --- |
| CI | Format, compile, unit tests and database integration |
| Containers | Build the real image and exercise Compose startup |
| Database tests | Use PostgreSQL instead of an approximate in-memory store |
| Deployment | Immutable images and externally configured secrets |
| Observability | Structured logs, health probes and restricted metrics |
| Recovery | Backups and a documented rollback procedure |
| Git | Business branches and pull requests that preserve commits |

Compose provides the local environment.
No cloud account, DNS, certificate or production target is assumed.
Choose a deployment environment before adding provider-specific infrastructure.

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial conventions |
