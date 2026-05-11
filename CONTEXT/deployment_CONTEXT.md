# Deployment Context – therapist-api

Last updated: 2026-05-11

---

## 1. How the Two Deployment Modes Differ

### Mode A: Local development (current default on dev machine)

```
[Docker Compose]          [Host machine]
  postgres  :5432  ─────►  host:5434
  pgadmin   :80    ─────►  host:5051
  rabbitmq  :5672  ─────►  host:5673
  rabbitmq  :15672 ─────►  host:15673

                            .\gradlew.bat bootRun  (Spring Boot runs on HOST)
                            host:8082
```

- Docker Compose only manages **infrastructure** (Postgres, pgAdmin, RabbitMQ).
- The Spring Boot app is **not** in a container — it runs directly on the Windows host via `.\gradlew.bat bootRun`.
- `build.gradle` reads `.env` and injects all env vars into the `bootRun` JVM process automatically.
- The app connects to Postgres and RabbitMQ via `localhost` at the **host-mapped ports** (e.g. `localhost:5434`).
- `.env` is **git-ignored**. It lives only on the developer's machine.

### Mode B: Azure VM / server deployment (full Docker)

```
[Docker Compose – single bridge network: backend]
  postgres  :5432  (internal only, no host port needed)
  rabbitmq  :5672  (internal only)
  api       :8082  ─────►  host:8082  (exposed to internet)
```

- ALL services including the Spring Boot app run as Docker containers.
- Inter-container communication uses **container names** as hostnames (`postgres`, `rabbitmq`), not `localhost`.
- The app connects to `postgres:5432` and `rabbitmq:5672` on the internal Docker bridge network.
- A **Dockerfile** is required to build the app image (see Section 3).
- `.env` is git-ignored, so it must be **manually created on the VM** with the correct Docker hostnames.

---

## 2. What the `.env` File Must Contain (Per Environment)

`.env` is never committed. It must be created manually on each machine.

### Local development `.env` (current values on dev machine)

```env
SERVER_PORT=8082
POSTGRES_PORT=5434
PGADMIN_PORT=5051
PGADMIN_DEFAULT_EMAIL=admin@therapist.com
PGADMIN_DEFAULT_PASSWORD=admin
RABBITMQ_AMQP_PORT=5673
RABBITMQ_MANAGEMENT_PORT=15673

JWT_PUBLIC_KEY=<RSA public key>
JWT_SIGNING_KID=mhsa-key-1
JWT_ISSUER=mhsa-auth
JWT_AUDIENCE=mhsa-api

VIDEO_PROVIDER=jitsi

ZOOM_APP_KEY=<key>
ZOOM_APP_SECRET=<secret>
```

### Azure VM `.env` (required additions for Docker deployment)

Same as above, **plus** these overrides so the app container can reach other containers by name:

```env
# Docker-specific: override host/port to use container names
DB_HOST=postgres
DB_PORT=5432
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672

# Infrastructure ports (used only by docker-compose, not by the app container)
POSTGRES_PORT=5433
PGADMIN_PORT=5050
RABBITMQ_AMQP_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

SERVER_PORT=8082

JWT_PUBLIC_KEY=<same RSA public key>
JWT_SIGNING_KID=mhsa-key-1
JWT_ISSUER=mhsa-auth
JWT_AUDIENCE=mhsa-api

VIDEO_PROVIDER=jitsi

ZOOM_APP_KEY=<key>
ZOOM_APP_SECRET=<secret>
```

**Why `DB_HOST=postgres`?**
`application.yml` resolves the datasource URL as:
```
jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:${POSTGRES_PORT:5433}}/...
```
Without `DB_HOST`, it defaults to `localhost`, which inside a container refers to the container itself — not the Postgres container. Setting `DB_HOST=postgres` makes the app resolve to the Postgres container name on the Docker bridge network.

---

## 3. Dockerfile (Required for Azure VM / Mode B)

The Dockerfile does NOT currently exist in the repo (it was reverted). It must be created at the repo root.

```dockerfile
# Stage 1: Build
FROM gradle:8.6-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-XX:+UseSerialGC", "-jar", "app.jar"]
```

Memory flags (`-Xmx512m`) are important on the Azure VM to prevent OOM when multiple containers are running.

---

## 4. docker-compose.yml Changes Required for Azure VM

The current `docker-compose.yml` only has infrastructure. To run everything via Docker, add an `api` service:

```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-therapist_api}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    ports:
      - "${POSTGRES_PORT:-5433}:5432"
    networks:
      - backend

  pgadmin:
    image: dpage/pgadmin4:8
    environment:
      PGADMIN_DEFAULT_EMAIL: ${PGADMIN_DEFAULT_EMAIL:-admin@local.dev}
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_DEFAULT_PASSWORD:-admin}
    ports:
      - "${PGADMIN_PORT:-5050}:80"
    depends_on:
      - postgres
    networks:
      - backend

  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "${RABBITMQ_AMQP_PORT:-5672}:5672"
      - "${RABBITMQ_MANAGEMENT_PORT:-15672}:15672"
    networks:
      - backend

  api:
    build: .
    ports:
      - "${SERVER_PORT:-8082}:${SERVER_PORT:-8082}"
    env_file:
      - .env
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - backend
    restart: unless-stopped

networks:
  backend:
    driver: bridge
```

**Important:** When running locally (Mode A), do NOT start the `api` service — it would conflict with `.\gradlew.bat bootRun`:
```powershell
# Local: only start infrastructure
docker compose up -d postgres pgadmin rabbitmq

# Azure VM: start everything
docker compose up -d
```

---

## 5. Step-by-Step: Clone and Run on Azure VM

```bash
# 1. Clone the repository
git clone https://github.com/22125027-22125037-Thesis-August-2026/therapist-api
cd therapist-api

# 2. Create the .env file (NOT in git — must be done manually)
nano .env
# Paste the Azure VM .env content from Section 2

# 3. Create the Dockerfile (NOT in repo currently — must be added)
# Either commit it to the repo, or create it manually from Section 3

# 4. Build and start all services
docker compose up -d --build

# 5. Verify all containers are running
docker compose ps

# 6. Check app logs
docker compose logs -f api
```

The app will be available at `http://<VM_PUBLIC_IP>:8082`.

---

## 6. Why Full Docker is Better on the VM (vs bootRun)

| Concern | bootRun on host | Full Docker |
|---|---|---|
| Java version | Must install JDK 17 manually | Bundled in image |
| Memory limits | No enforcement | `-Xmx512m` prevents OOM |
| Service networking | Uses `localhost` + host ports | Uses container names on bridge network |
| Startup on reboot | Manual | `restart: unless-stopped` |
| `.env` loading | `build.gradle` injects into JVM | `env_file` in docker-compose |

---

## 7. application.yml — Environment Variable Fallback Chain

Key properties and their resolution order (first defined value wins):

| Config key | 1st (explicit) | 2nd (Docker alias) | 3rd (default) |
|---|---|---|---|
| DB host | `DB_HOST` | — | `localhost` |
| DB port | `DB_PORT` | `POSTGRES_PORT` | `5433` |
| DB name | `DB_NAME` | `POSTGRES_DB` | `therapist_api` |
| DB user | `DB_USERNAME` | `POSTGRES_USER` | `postgres` |
| DB pass | `DB_PASSWORD` | `POSTGRES_PASSWORD` | `postgres` |
| RabbitMQ host | `RABBITMQ_HOST` | — | `localhost` |
| RabbitMQ port | `RABBITMQ_PORT` | `RABBITMQ_AMQP_PORT` | `5672` |
| Server port | `SERVER_PORT` | — | `8080` |

This two-tier design means the **same `.env` style** works for both local and Docker, with only `DB_HOST`, `DB_PORT`, `RABBITMQ_HOST`, `RABBITMQ_PORT` needing to differ.

---

## 8. What is NOT in the Repository (Must Be Created Manually)

| Item | Why missing | Where to get it |
|---|---|---|
| `.env` | git-ignored (contains secrets) | Copy from dev machine, adjust Docker host values |
| `Dockerfile` | Was reverted | Create from Section 3 template above |
| JWT RSA key pair | Security — never commit | From auth service team / shared secrets store |
| Zoom credentials | Security | From Zoom developer portal |
