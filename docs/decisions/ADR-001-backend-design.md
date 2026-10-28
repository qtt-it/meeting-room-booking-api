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

The assessment requires Java 17+, Spring Boot 3, PostgreSQL and Docker Compose.
The repository must preserve commit history and support business-focused reviews.

## Decisions

| Decision | Reason | Trade-off |
| --- | --- | --- |
| Java 17 and Spring Boot 3.5.16 | Match the required framework major | Assess support before real deployment |
| Maven multi-module | Explicit dependencies and independent feature modules | Several source roots need configuration |
| Modular monolith | Keep approval within one ACID transaction | Modules deploy together |
| JDBC and Flyway | Make SQL, locks and schema changes visible | Manual row mapping |
| PostgreSQL exclusion constraint | Prevent concurrent approved overlaps in the database | Requires btree_gist |
| Pending requests do not reserve rooms | The first successful approval wins | Availability can change before review |
| Atomic series approval | Avoid partially accepted recurring meetings | One conflict blocks the whole series |
| JWT and opaque refresh tokens | Bounded access tokens with revocable sessions | Logout does not immediately expire access tokens |
| Active room slots 1-10 | Enforce the room limit in the database | Internal slot allocation |
| Business branches and merge commits | Focus reviews and preserve history | Integrate dependent work in order |

## Alternatives

JPA simplifies CRUD, but explicit range queries and transaction locks are central to this service.
Redis locks add infrastructure and cannot replace database constraints.
Microservices and a broker add deployment work without a requirement at this scale.
Unbounded recurrence requires scheduling and recovery for materialization failures.

## Sources

- [Spring Boot release](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/)
- [springdoc for Spring Boot 3](https://springdoc.org/v2/)
- [PostgreSQL ranges](https://www.postgresql.org/docs/17/rangetypes.html)
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/)

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial decisions |
