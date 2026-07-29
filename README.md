# NotifyHub — Day 1

**Goal for today:** project scaffolding, Postgres running in Docker, `app_user` table created via JPA, and a working health check.

## What's in this commit

- `pom.xml` — Spring Boot 3.3.4, Java 17, Web + Data JPA + Validation + Actuator + PostgreSQL driver + Lombok
- `docker-compose.yml` — local Postgres 16 container (db: `notifyhub`, user/pass: `notifyhub`/`notifyhub`)
- `AppUser` entity + `AppUserRepository` — maps to the `app_user` table from the design doc
- `StatusController` — `/api/ping` proves the app can talk to Postgres through JPA
- Actuator — `/actuator/health` for infra-level health check
- One context-load test

## How to run it

1. Start Postgres:
   ```bash
   docker compose up -d
   ```
   Wait a few seconds, then confirm it's healthy:
   ```bash
   docker compose ps
   ```
   > **Note:** the container is mapped to host port **5433** (not the default 5432), specifically to avoid colliding with any Postgres you might already have installed/running locally. See the Troubleshooting section below if you still hit port or auth errors.

2. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   (or `mvn spring-boot:run` if you don't have the wrapper — see note below)

3. Verify:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8080/api/ping
   ```
   You should get `{"status":"UP", ...}` from both. `userCount` will be `0` — that's correct, we haven't built registration yet (that's Day 2).

4. Confirm the table was actually created:
   ```bash
   docker exec -it notifyhub-postgres psql -U notifyhub -d notifyhub -c "\d app_user"
   ```
   You should see columns: `id`, `email`, `password_hash`, `role`, `api_key`, `created_at`.

## Troubleshooting

**`Bind for 0.0.0.0:5432 failed: port is already allocated`**
Something else on your machine — a locally installed Postgres service, or a leftover container from another project — is already using port 5432. This project's `docker-compose.yml` now maps to host port **5433** specifically to sidestep this. If you still see a port conflict on 5433:
```bash
# See what's using a port (macOS/Linux)
lsof -i :5433
# or list all running containers
docker ps
```
Pick any free port, update it in **both** places: the `ports:` line in `docker-compose.yml` and the `spring.datasource.url` in `application.yml`.

**`FATAL: password authentication failed for user "notifyhub"`**
This is almost always a symptom of the port conflict above — Spring Boot silently connected to a *different* Postgres instance than the one this project started (one that doesn't have a `notifyhub` user). Fix the port conflict first; this error should disappear on its own. If it persists after that:
```bash
# Wipe the container + its volume and start clean
docker compose down -v
docker compose up -d
```

**Still stuck after a clean restart?**
Check the container logs directly:
```bash
docker compose logs postgres
```

## Note on the Maven wrapper

This project doesn't include the `mvnw` wrapper scripts (binary files don't transfer cleanly through this chat). Generate them once with:
```bash
mvn -N io.takari:maven:wrapper -Dmaven=3.9.9
```
Or just use your locally installed `mvn` directly — either is fine for now.

## Why these choices (for your README/interview notes)

- **`ddl-auto: update`** — fine for local development speed right now. We will replace this with proper Flyway/Liquibase migrations before Week 4 (production-hardening), because `update` is not safe for real deployments — it can silently apply unexpected schema changes.
- **`open-in-view: false`** — disables Spring's default "Open Session In View" pattern. Keeping it off from day one forces you to fetch everything you need inside the service layer (not lazily in the controller/view), which avoids a very common source of `LazyInitializationException` bugs and hidden N+1 queries later.
- **Actuator + custom `/api/ping`** — Actuator's `/actuator/health` only proves the app booted. `/api/ping` proves the JPA/Postgres wiring actually works end-to-end, which is a more meaningful "Day 1 done" signal.

## Day 1 checklist

- [x] Spring Boot project scaffolded (Maven, Java 17)
- [x] Postgres running via Docker Compose
- [x] `app_user` entity + repository, table auto-created
- [x] `/actuator/health` and `/api/ping` both return UP
- [x] One passing test (`contextLoads`)

## Next: Day 2

JWT authentication — register/login endpoints, `JwtAuthenticationFilter`, `SecurityConfig` with role-based rules, and 2–3 unit tests for the auth service.
