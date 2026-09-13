# NotifyHub

NotifyHub is a Spring Boot notification platform that provides a unified API for sending notifications through multiple channels while handling authentication, preferences, templates, asynchronous processing, retries, dead-letter queues, audit trails, rate limiting, structured logging, and observability.

The project is built as a production-oriented backend learning project using Java, Spring Boot, PostgreSQL, RabbitMQ, Redis, Docker, and GitHub Actions.

---

## Features

* JWT-based authentication and authorization
* Role-based access control
* Multi-channel notifications

  * Email
  * SMS
  * Push
* Asynchronous notification processing with RabbitMQ
* Retry handling for failed notification processing
* Dead Letter Queue (DLQ)
* Admin DLQ inspection and reprocessing
* Notification attempt audit trail
* Notification templates
* Per-user notification preferences
* Rate limiting with Redis
* Correlation IDs with `X-Correlation-ID`
* Structured JSON logging
* OpenAPI / Swagger API documentation
* Spring Boot Actuator
* PostgreSQL persistence with Spring Data JPA
* Redis integration
* Docker Compose deployment
* Testcontainers-based integration tests
* GitHub Actions CI pipeline

---

## Tech Stack

| Technology           | Purpose                                |
| -------------------- | -------------------------------------- |
| Java 17              | Application language                   |
| Spring Boot 3.3.4    | Backend framework                      |
| Spring Security      | Authentication and authorization       |
| JWT                  | Stateless authentication               |
| Spring Data JPA      | Database access                        |
| PostgreSQL 16        | Primary database                       |
| RabbitMQ             | Asynchronous messaging                 |
| Redis 7              | Rate limiting / caching infrastructure |
| Docker               | Containerization                       |
| Docker Compose       | Local multi-container deployment       |
| Testcontainers       | Integration testing                    |
| Maven                | Build and dependency management        |
| GitHub Actions       | Continuous Integration                 |
| Swagger / OpenAPI    | API documentation                      |
| Spring Boot Actuator | Health and metrics                     |
| Twilio               | SMS delivery                           |
| Mailtrap             | Email delivery/testing                 |

---

## Architecture

NotifyHub follows a layered backend architecture.

```text
                    Client
                      |
                      v
              Spring Boot API
                      |
          +-----------+-----------+
          |                       |
          v                       v
     Spring Security         Rate Limiting
          |                       |
          +-----------+-----------+
                      |
                      v
              Service Layer
                      |
          +-----------+-----------+
          |                       |
          v                       v
      PostgreSQL              RabbitMQ
                                  |
                    +-------------+-------------+
                    |             |             |
                    v             v             v
                 Email           SMS          Push
                Consumer       Consumer       Consumer
                    |
                    v
              Notification
               Attempt/Audit
                    |
                    v
                   DLQ
```

Redis is used as supporting infrastructure for rate limiting, while PostgreSQL stores the application's persistent domain data.

---

## Why These Choices

* **Spring Boot** — Provides a structured framework for building REST APIs with dependency injection, security, data access, messaging, and production-ready operational features.

* **PostgreSQL** — Used as the primary relational database for persistent application data such as users, notifications, preferences, templates, and audit records.

* **RabbitMQ** — Decouples notification requests from delivery processing. Notifications can be processed asynchronously, retried after failures, and routed to a Dead Letter Queue when processing cannot succeed.

* **Redis** — Provides fast in-memory storage for rate-limiting operations without adding unnecessary load to PostgreSQL.

* **JWT** — Enables stateless authentication for the REST API and supports role-based authorization.

* **Docker** — Packages the application and its dependencies into reproducible containers, making the development and deployment environment consistent.

* **Testcontainers** — Allows integration tests to run against real containerized infrastructure instead of relying only on mocks, increasing confidence in database and messaging integration.

* **GitHub Actions** — Automatically builds the project and runs the test suite on pushes and pull requests, providing continuous integration checks.

---

## Project Structure

```text
src/
├── main/
│   └── java/com/notifyhub/
│       ├── audit/
│       ├── auth/
│       ├── config/
│       ├── exception/
│       ├── notification/
│       ├── preference/
│       ├── ratelimit/
│       ├── template/
│       └── user/
│
└── test/
    ├── java/com/notifyhub/
    └── resources/
```

---

## Prerequisites

Install the following before running NotifyHub locally:

* Java 17
* Maven
* Docker
* Docker Compose

Verify:

```bash
java -version
mvn -version
docker --version
docker compose version
```

---

# Running NotifyHub

## Option 1: Run dependencies with Docker and application with Maven

Start PostgreSQL, RabbitMQ, and Redis:

```bash
docker compose up -d postgres rabbitmq redis
```

Then run the Spring Boot application:

```bash
mvn spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

---

## Option 2: Run the complete application with Docker Compose

Build the application JAR:

```bash
mvn clean package -DskipTests
```

Then build and start the complete stack:

```bash
docker compose up -d --build
```

Check the containers:

```bash
docker compose ps
```

Expected services:

```text
notifyhub-app
notifyhub-postgres
notifyhub-rabbitmq
notifyhub-redis
```

Stop the stack:

```bash
docker compose down
```

---

# Docker Ports

| Service                | Host Port | Container Port |
| ---------------------- | --------: | -------------: |
| NotifyHub              |      8080 |           8080 |
| PostgreSQL             |      5433 |           5432 |
| RabbitMQ               |      5672 |           5672 |
| RabbitMQ Management UI |     15672 |          15672 |
| Redis                  |      6380 |           6379 |

The PostgreSQL and Redis host ports are intentionally different from their default ports to avoid conflicts with locally installed services.

Inside Docker Compose, NotifyHub communicates with the services using their Compose service names:

```text
postgres
rabbitmq
redis
```

---

# Environment Variables

External credentials should be supplied through environment variables and should never be committed to Git.

## Database

```text
DB_URL
```

Docker Compose uses:

```text
jdbc:postgresql://postgres:5432/notifyhub
```

## RabbitMQ

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

## Redis

```text
REDIS_HOST
REDIS_PORT
```

## JWT

```text
JWT_SECRET
JWT_EXPIRATION_MS
```

The default JWT secret is intended only for local development.

For a real deployment, always provide a strong secret through the environment.

## Mailtrap

```text
MAILTRAP_USERNAME
MAILTRAP_PASSWORD
```

## Twilio

```text
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
TWILIO_PHONE_NUMBER
```

Never commit real Mailtrap or Twilio credentials to the repository.

---

# API Documentation

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Swagger provides an interactive view of the NotifyHub REST API.

---

# Health and Observability

NotifyHub exposes Spring Boot Actuator endpoints.

Health:

```bash
curl http://localhost:8080/actuator/health
```

Metrics:

```text
http://localhost:8080/actuator/metrics
```

The application also exposes:

```text
GET /api/ping
```

for a lightweight application-level connectivity check.

---

## Health Check Note

The Actuator health endpoint checks multiple dependencies, including:

* PostgreSQL
* Redis
* RabbitMQ
* Mail
* Disk space
* Application ping

Therefore, the overall health status can be `DOWN` if an external dependency such as Mailtrap is not configured correctly, even when the application itself and the database, Redis, and RabbitMQ services are running correctly.

For local development, configure Mailtrap credentials if email health needs to report `UP`.

---

# RabbitMQ Management

RabbitMQ Management UI:

```text
http://localhost:15672
```

Default local credentials configured by Docker Compose:

```text
Username: notifyhub
Password: notifyhub123
```

RabbitMQ is responsible for asynchronous notification processing.

---

# Authentication

NotifyHub uses JWT authentication.

The authentication flow is:

```text
Register
   |
   v
Login
   |
   v
JWT issued
   |
   v
Client sends:
Authorization: Bearer <token>
   |
   v
JwtAuthenticationFilter
   |
   v
Authenticated request
```

Protected endpoints require a valid JWT.

Administrative operations such as DLQ management are protected by role-based authorization.

---

# Notification Processing

Notifications are submitted through the REST API and processed asynchronously.

```text
REST Request
     |
     v
Notification Service
     |
     v
PostgreSQL
     |
     v
RabbitMQ
     |
     v
Channel Consumer
     |
     +----> Email
     |
     +----> SMS
     |
     +----> Push
```

This keeps notification delivery separate from the initial API request and allows failures to be retried independently.

---

# Retry and Dead Letter Queue

Failed notification processing can be retried using RabbitMQ listener retry configuration.

The current configuration uses:

```text
Initial interval: 2000 ms
Multiplier:       2.0
Maximum attempts: 4
```

Messages that cannot be processed successfully are routed to the Dead Letter Queue.

Administrators can inspect and reprocess DLQ notifications through the administrative API.

Notification attempts are persisted to PostgreSQL to provide an audit trail of processing activity.

---

# Correlation IDs and Logging

NotifyHub supports correlation IDs using:

```text
X-Correlation-ID
```

When a request contains a correlation ID, it is propagated through the application's processing flow.

This allows related API requests, asynchronous processing, and notification attempts to be traced more easily.

Application logs use structured JSON logging.

Example fields include:

```text
timestamp
message
logger_name
thread_name
level
correlation ID
```

---

# Testing

The project contains unit and integration tests.

Run the complete test suite:

```bash
mvn clean test
```

The integration tests use Testcontainers for external dependencies rather than relying on locally installed PostgreSQL, Redis, or RabbitMQ instances.

Testcontainers provides:

```text
PostgreSQL
Redis
RabbitMQ
```

during integration testing.

This keeps integration tests reproducible across development machines and CI environments.

---

# GitHub Actions CI

The project includes a GitHub Actions workflow that runs on:

* Pushes to `master`
* Pull requests targeting `master`

The CI pipeline:

1. Checks out the repository
2. Installs Java 17
3. Uses Maven dependency caching
4. Provides test configuration for external services
5. Runs:

```bash
mvn clean test
```

Integration tests start their required infrastructure through Testcontainers.

---

# Docker Deployment

The application uses the following Dockerfile strategy:

```text
Maven build
    |
    v
target/*.jar
    |
    v
Docker image
    |
    v
NotifyHub container
```

The Docker image uses Java 17 and exposes port `8080`.

Docker Compose builds the current application image directly from the project's Dockerfile:

```yaml
notifyhub:
  build:
    context: .
    dockerfile: Dockerfile
```

This avoids depending on an old, manually tagged application image.

---

# Persistent Data

Docker Compose creates named volumes for:

```text
notifyhub_pgdata
notifyhub_rabbitmqdata
notifyhub_redisdata
```

These volumes allow PostgreSQL, RabbitMQ, and Redis data to persist across normal container restarts.

To completely remove containers and their persistent volumes:

```bash
docker compose down -v
```

Use this carefully because it deletes the stored Docker volume data for the project.

---

# Troubleshooting

## Check container status

```bash
docker compose ps
```

## View NotifyHub logs

```bash
docker compose logs --tail=100 notifyhub
```

## View PostgreSQL logs

```bash
docker compose logs postgres
```

## View RabbitMQ logs

```bash
docker compose logs rabbitmq
```

## View Redis logs

```bash
docker compose logs redis
```

## Rebuild the application

```bash
mvn clean package -DskipTests
docker compose up -d --build
```

## Check application health

```bash
curl http://localhost:8080/actuator/health
```

## Check application ping

```bash
curl http://localhost:8080/api/ping
```

---

# Common Issues

### Mailtrap authentication failure

If Actuator reports:

```text
mail: DOWN
```

check:

```text
MAILTRAP_USERNAME
MAILTRAP_PASSWORD
```

The application can still start successfully while Mailtrap is unavailable.

---

### PostgreSQL port conflict

The project maps PostgreSQL to host port `5433`.

If port `5433` is already occupied:

```bash
docker ps
```

or:

```bash
lsof -i :5433
```

Then resolve the conflict before starting the stack.

---

### RabbitMQ connection failure

When running inside Docker Compose, NotifyHub should connect using:

```text
rabbitmq:5672
```

not:

```text
localhost:5672
```

---

### Redis connection failure

When running inside Docker Compose:

```text
REDIS_HOST=redis
REDIS_PORT=6379
```

The host-facing port is `6380`, but containers communicate using Redis's internal port `6379`.

---

# Security Notes

Do not commit secrets to Git.

The following values must remain environment-based:

```text
JWT_SECRET
MAILTRAP_USERNAME
MAILTRAP_PASSWORD
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
TWILIO_PHONE_NUMBER
```

The default JWT secret in the configuration is for local development only.

For production deployment:

* Use strong randomly generated secrets
* Store credentials in a secrets manager or secure environment configuration
* Do not expose management interfaces unnecessarily
* Use HTTPS
* Replace development defaults
* Use proper database migrations instead of relying on `ddl-auto: update`

---

# Development Notes

The application currently uses:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

This is convenient for development but should be replaced with a controlled database migration strategy such as Flyway or Liquibase for production environments.

The application also uses:

```yaml
spring.jpa.open-in-view: false
```

to keep database access within the service layer and avoid relying on the Open Session in View pattern.

---

# Current Project Status

NotifyHub has completed its development roadmap through the final deployment and documentation stage.

Completed areas include:

* Backend foundation
* JWT authentication
* Notification APIs
* Notification preferences
* Templates
* RabbitMQ asynchronous processing
* Retry handling
* Dead Letter Queue
* Audit trail
* Structured logging
* Correlation IDs
* Rate limiting
* Swagger / OpenAPI
* Actuator observability
* Dockerization
* Testcontainers integration testing
* GitHub Actions CI
* Docker Compose deployment
* Final project documentation

The project can now be built, tested, containerized, and run as a complete local multi-service application.

---

# Useful Commands

### Build

```bash
mvn clean package
```

### Test

```bash
mvn clean test
```

### Start full Docker stack

```bash
mvn clean package -DskipTests
docker compose up -d --build
```

### Check containers

```bash
docker compose ps
```

### View application logs

```bash
docker compose logs --tail=100 notifyhub
```

### Stop stack

```bash
docker compose down
```

### Stop and remove persistent data

```bash
docker compose down -v
```

---

## NotifyHub

A complete Spring Boot notification backend demonstrating authentication, asynchronous messaging, multi-channel notification delivery, reliability patterns, observability, testing, CI, and containerized deployment.
