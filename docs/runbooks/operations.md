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

| Variable | Purpose |
| --- | --- |
| DATABASE_URL | JDBC URL for direct application startup |
| DATABASE_NAME | Compose database name |
| DATABASE_USERNAME, DATABASE_PASSWORD | Database credentials |
| JWT_SECRET | At least 32 UTF-8 bytes, shared across API instances |
| BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD | Create an administrator only when the account catalog is empty |
| DOCS_ENABLED | Disable Swagger when API documentation must not be public |
| CORS_ALLOWED_ORIGINS | Comma-separated allowed origins |
| API_PORT, DATABASE_PORT | Compose host ports |

Compose provides demonstration defaults for local startup.
Git ignores `.env`.
Use a secret manager, private credentials and TLS outside local development.
Generate a signing secret with `openssl rand -hex 32`.

Bootstrap never replaces an existing password.
Remove bootstrap credentials from deployment configuration after initial account creation.

## Health and logs

- Liveness: `/actuator/health/liveness`.
- Readiness: `/actuator/health/readiness`, including database connectivity.
- Only administrators access `/actuator/prometheus` and `/actuator/info`.
- ECS JSON logs go to stdout and include requestId.
- Never log passwords, access tokens or refresh tokens.
- Never enable request-body diagnostics for real data.

The Bruno collection creates accounts, rooms and bookings.
Run it only against a disposable development database, never as a production healthcheck.

The login limit is 20 attempts per IP per minute in one API instance.
Configure ingress limits for a reverse proxy or multiple replicas.
Ingress must bound JSON body sizes, request timeouts and concurrent connections.
The application does not trust client-supplied Forwarded or X-Forwarded-For headers.

## Startup and shutdown

```bash
docker compose up --build -d --wait
docker compose ps
docker compose logs --tail 100 api
docker compose down
```

The `down` command retains the volume. Never use `down -v` for data that must survive.
The API runs as a non-root user with a read-only filesystem. Only /tmp is writable.
Graceful shutdown allows requests and connections to finish.

## Backup and restore

Schedule pg_dump backups and verify restoration into a separate database.
This command writes to an explicit backup file.
Choose a new filename when an earlier backup must be retained.

```bash
mkdir -p .local/backups
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > .local/backups/meeting-booking.dump
```

Restore only into a separate empty database.
Verify authentication, room counts and exclusion constraints after restoration.
Never overwrite the active database without an approved recovery plan.

## Deployment and rollback

1. Run CI, container smoke and the API collection against a disposable database.
2. Create a backup.
3. Run migrations with the migration role.
4. Start the new image and wait for readiness.
5. Verify authentication, availability and booking approval.
6. For an application fault, restore the previous image while the schema remains compatible.
7. For a schema fault, use a reviewed compensating migration or restore according to the recovery plan.

Compose supports local development and assessment.
The repository does not provision cloud resources, DNS or certificates.
Before production deployment, select the environment, hostname, secret manager, backup policy and TLS ingress.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Invalid JWT_SECRET | At least 32 UTF-8 bytes |
| Database starts but the API is not ready | Flyway logs, connection configuration and btree_gist permissions |
| A changed bootstrap password does not change login | The account already exists. Use the password endpoint |
| Approval returns 409 | Another approval reserved the room, or the version changed |
| Recurrence returns 422 | Sunday, invalid business hours or the booking horizon |
| Login returns 429 behind a proxy | Clients share one remote address. Apply limits at ingress |

## History

| Date | Author | Change |
| --- | --- | --- |
| 2026-10-28 | @qtt-it | Initial operations runbook |
| 2026-10-28 | @qtt-it | Standardize all operational guidance in English |
