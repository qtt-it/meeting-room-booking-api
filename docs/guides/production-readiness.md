---
title: 'Production readiness'
type: guide
status: active
author: '@qtt-it'
created: 2026-10-28
updated: 2026-10-28
---

# Production readiness

This repository includes application guardrails, not a deployed production environment.
The default Compose stack is for local evaluation only.
Do not reuse its credentials, database role or networking policy for real data.

## Production profile

Set SPRING_PROFILES_ACTIVE=prod when deploying the application image.
The profile disables public OpenAPI and bootstrap by default.
Startup validation runs before application singletons and database migrations.
Errors name the invalid setting, never its value.

| Setting | Requirement |
| --- | --- |
| DATABASE_URL | Single-host PostgreSQL JDBC URL with sslmode=verify-full |
| DATABASE_USERNAME | Dedicated runtime role, not a superuser or schema owner |
| DATABASE_PASSWORD | Private secret, at least 16 UTF-8 bytes |
| JWT_SECRET | Private signing secret, at least 32 UTF-8 bytes |
| CORS_ALLOWED_ORIGINS | Explicit HTTPS origins without paths, credentials or wildcards |
| FLYWAY_ENABLED | true for the controlled migration startup, false when migrations run separately |
| MIGRATION_USERNAME, MIGRATION_PASSWORD | Separate DDL role and secret when Flyway is enabled |
| BOOTSTRAP_ADMIN_ENABLED | false normally, true only for initial administrator provisioning |
| BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD | Supply only during explicit initial provisioning |

The validator rejects known local demo secrets, weak minimum lengths and insecure CORS origins.
Length checks cannot prove secret entropy.
Generate secrets with a cryptographic generator and store them in the selected secret manager.
Never pass production secrets in shell history, Git, a Docker build argument or a PR.

Use a URL shaped like this, with the real host and mounted CA certificate:

```text
jdbc:postgresql://database.internal:5432/meeting_booking?sslmode=verify-full&sslrootcert=/run/secrets/postgres-ca.crt
```

The guard rejects duplicate URL parameters, URL credentials and custom SSL factories or hostname verifiers.
The JDBC driver must validate the real certificate chain and hostname.
Only a real connection can prove that the certificate and database are configured correctly.
The guard also rejects separate Hikari URL/credential overrides and known TLS overrides in pool or migration properties.
Do not introduce alternate connection settings without reviewing the validator.
Custom multi-host URLs require a separate reviewed configuration change.

## Database ownership and migrations

The runtime account must not own tables, create extensions or run DDL.
A database administrator provisions the database and btree_gist extension.
The migration role owns application objects and applies the reviewed Flyway scripts.
Grant the runtime role CONNECT, schema USAGE, table SELECT/INSERT/UPDATE/DELETE and sequence USAGE/SELECT as required.
Apply matching default privileges for new objects.
Grant only the migration role access to Flyway schema history.

Role names alone do not prove least privilege.
Verify grants from PostgreSQL catalogs before deployment.
The application validates that configured runtime and migration usernames differ.
It does not create roles or inspect effective database grants.

Run migrations once as a controlled release step.
If using startup migration, start the controlled instance with FLYWAY_ENABLED=true and migration credentials.
Wait for readiness and verify the migration result.
Then remove migration credentials and set FLYWAY_ENABLED=false for normal instances.
Do not disable migrations against an empty or incompatible schema.

Provision the initial administrator with BOOTSTRAP_ADMIN_ENABLED=true and private credentials.
The account catalog must be empty. Provisioning never resets an existing password.
After provisioning, disable bootstrap and remove both credential variables.
The production guard rejects unused bootstrap credentials.

## HTTP and capacity boundaries

The application bounds API request bodies to 64 KiB, including streams without Content-Length.
Oversized bodies receive HTTP 413 with Request.PayloadTooLarge and a request ID.
The filter buffers at most 65,537 bytes per request before passing accepted bodies to blocking MVC handlers.
It never logs body content.
The limit applies before authentication, so an oversized unauthenticated API request can receive 413 instead of 401.

The body limit does not prevent slow uploads or bound every connection.
Ingress must enforce body size, upload/read deadlines, connection limits and a shared login rate limit.
The in-process login limiter is not distributed.
Forwarded headers remain untrusted. Configure ingress source handling deliberately before enabling proxy trust.

## Release checklist

- [ ] Assign an operator, incident contact, availability target and recovery targets.
- [ ] Select the deployment environment, private database and TLS hostname.
- [ ] Use an immutable reviewed image and current-head CI evidence.
- [ ] Scan the built image and dependencies. Review relevant advisories.
- [ ] Configure private secrets, runtime grants and the migration procedure.
- [ ] Confirm ingress TLS, authentication abuse controls and resource limits.
- [ ] Send structured logs and metrics to the selected monitoring system.
- [ ] Configure actionable alerts and verify their delivery.
- [ ] Schedule encrypted backups and test restoration into a separate database.
- [ ] Perform load and concurrency tests against representative capacity.
- [ ] Exercise readiness, graceful shutdown and application rollback.
- [ ] Record approval for the actual deployment and any database changes.

These environment-specific checks are not marked complete by unit tests or a successful local Compose run.
There is no cloud account, DNS, certificate, alert destination or backup service provisioned by this repository.

## Dependency policy

Keep Java 17 and Spring Boot 3 for this delivery.
Dependabot proposes compatible updates and does not automatically move the Boot, API documentation or container stack to a new major version.
Review patch and minor updates too. Automation is not approval.
Already-open upgrade PRs still require separate review and must not bypass the technology constraints.

## Sources

- [Spring Boot 3.5 configuration properties](https://docs.spring.io/spring-boot/3.5/appendix/application-properties/index.html)
- [PostgreSQL JDBC SSL configuration](https://jdbc.postgresql.org/documentation/ssl/)

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Add production profile requirements and environment release gates |
