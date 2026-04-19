# therapist-api

Spring Boot API for therapist booking workflows.

## API Documentation

- Controller reference: `docs/API_CONTROLLER_REFERENCE.md`

## Prerequisites

- Java 17
- Docker Desktop (for Postgres and RabbitMQ)
- Windows PowerShell (commands below)

## Configuration

This project loads configuration from:

1. `src/main/resources/application.yml`
2. Local `.env` file (imported automatically by Spring Boot)
3. Environment variables from your shell/CI

Important `.env` keys:

- `SERVER_PORT` - API HTTP port (default `8080`)
- `POSTGRES_PORT` - host port mapped to Postgres container (`5432` inside container)
- `PGADMIN_PORT` - host port mapped to pgAdmin web UI (`80` inside container)
- `PGADMIN_DEFAULT_EMAIL` - pgAdmin login email
- `PGADMIN_DEFAULT_PASSWORD` - pgAdmin login password
- `RABBITMQ_AMQP_PORT` - host AMQP port (`5672` inside container)
- `RABBITMQ_MANAGEMENT_PORT` - host RabbitMQ UI port (`15672` inside container)

Current local defaults in `.env`:

- `SERVER_PORT=8082`
- `POSTGRES_PORT=5434`
- `PGADMIN_PORT=5051`
- `PGADMIN_DEFAULT_EMAIL=admin@local.dev`
- `PGADMIN_DEFAULT_PASSWORD=admin`
- `RABBITMQ_AMQP_PORT=5673`
- `RABBITMQ_MANAGEMENT_PORT=15673`

## Run Locally

### 1. Start infrastructure

```powershell
docker compose up -d postgres pgadmin rabbitmq
```

pgAdmin URL:

- `http://localhost:5051` (or whatever `PGADMIN_PORT` is set to)

pgAdmin connection target for this project:

- Host: `postgres`
- Port: `5432`
- Database: `therapist_api`
- Username: `postgres`
- Password: `postgres`

### 2. Start the API

```powershell
.\gradlew.bat bootRun
```

API URL:

- `http://localhost:8082` (or whatever `SERVER_PORT` is set to)

## Verify Port Conflicts (Windows)

If startup fails with "Port XXXX was already in use":

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen |
  Select-Object LocalAddress, LocalPort, OwningProcess
Get-Process -Id <PID>
```

Then either:

- stop that process, or
- change `SERVER_PORT` in `.env` and restart the app

## Industrial-Grade Port Strategy

- Keep each local service on a dedicated, documented port.
- Make ports environment-driven (`SERVER_PORT`, `POSTGRES_PORT`, etc.), never hardcoded.
- Reserve `8080` for a primary gateway/auth service if your team already uses it.
- Use per-environment overrides in CI/CD via environment variables, not code changes.
- Keep `.env` for local development only; production values should come from secret/config management.

## Test

```powershell
.\gradlew.bat test
```
