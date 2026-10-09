# Complete Bracket Guide

This document walks you through **everything from an empty machine to a running
Bracket application**, whether you operate it in the *dev*, *default*, or *prod*
stage, or inside a Docker container.

> The content here is specific to the Bracket repository. If you opened this
> file from a fork or a copy, adjust the git remote names and DB credentials to
> match your own environment.

---

## Table of contents

1. [Project overview](#1-project-overview)
2. [Prerequisites & tooling versions](#2-prerequisites--tooling-versions)
3. [Installing the toolchain](#3-installing-the-toolchain)
4. [Cloning the repository](#4-cloning-the-repository)
5. [Verifying the toolchain](#5-verifying-the-toolchain)
6. [Configuring the environment](#6-configuring-the-environment)
7. [Understanding the folder layout](#7-understanding-the-folder-layout)
8. [Running the database (PostgreSQL via Docker)](#8-running-the-database-postgresql-via-docker)
9. [DEV stage — hot reload](#9-dev-stage--hot-reload)
10. [Default stage — packaged jar](#10-default-stage--packaged-jar)
11. [PROD stage — packaged jar + Flyway](#11-prod-stage--packaged-jar--flyway)
12. [Dedicated test database & running unit tests](#12-dedicated-test-database--running-unit-tests)
13. [Build & run as a Docker image](#13-build--run-as-a-docker-image)
14. [Makefile target cheat-sheet](#14-makefile-target-cheat-sheet)
15. [Adding entities & new schemas](#15-adding-entities--new-schemas)
16. [Common troubleshooting](#16-common-troubleshooting)
17. [Reset & uninstall](#17-reset--uninstall)

---

## 1. Project overview

Bracket is a **Quarkus 3.40.1** application (Java 21) built on:

| Technology | Role |
|---|---|
| Quarkus + RESTEasy Reactive + Jackson | HTTP API |
| Hibernate ORM with Panache | JPA persistence |
| PostgreSQL 16 | Main database |
| Flyway | Schema migrations for the prod stage |
| SmallRye JWT | Authentication (ready to be wired once you add a signing key) |
| Mailer (Agroal) | Outgoing e-mail |
| Maven Wrapper (`mvnw`) | Build without a globally installed Maven |
| Docker Compose | Runs Postgres & the application container |
| Makefile | Orchestrates the day-to-day commands |

Three runtime stages are supported:

| Stage | How to run | Flyway | Hibernate generation |
|---|---|---|---|
| **dev** | `make dev` (Quarkus dev mode) | **OFF** | `update` + `create_namespaces=true` |
| **default** | `make start` | ON | `update` |
| **prod** | `make start-prod` | ON | `validate` |

All application tables live in the **`bracket`** schema — **not** `public`.

---

## 2. Prerequisites & tooling versions

| Tool | Minimum version | Note |
|---|---|---|
| OS | Linux (Debian/Ubuntu), macOS 12+, or Windows + WSL2 | This guide assumes Linux/WSL; macOS is nearly identical |
| JDK | **21 LTS** (Temurin recommended) | Project uses `<maven.compiler.release>21</maven.compiler.release>` |
| Apache Maven | Optional (provided via `./mvnw`) | The wrapper downloads Maven automatically |
| Docker Engine | 24+ with **Compose v2** (`docker compose` subcommand, not `docker-compose`) | Required for Postgres & the app image |
| Git | 2.30+ | Clone the repo |
| GNU Make | 4.0+ | Runs the Makefile targets |
| `bash` | 4.0+ | Used by `schema-init.sh` |
| Optional: IntelliJ IDEA | 2026.x | Primary IDE used by this project's author |
| Optional: `postgresql-client` | 16 | For direct `psql` access from the host |

---

## 3. Installing the toolchain

### 3.1 Linux (Debian 13 / Ubuntu 24.04)

```bash
# Refresh the package index
sudo apt update && sudo apt upgrade -y

# Base tooling
sudo apt install -y git curl wget unzip build-essential ca-certificates gnupg

# JDK 21 Temurin from the Adoptium repository
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo tee /etc/apt/keyrings/adoptium.asc >/dev/null
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo $VERSION_CODENAME) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-21-jdk

# Docker Engine + Compose plugin (from Docker's official repository)
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io \
                    docker-buildx-plugin docker-compose-plugin

# Add your user to the docker group (log out and back in after this)
sudo usermod -aG docker "$USER"

# PostgreSQL client (optional, useful for quick checks)
sudo apt install -y postgresql-client

# Verify docker works without sudo (after logging out & in)
docker run --rm hello-world
```

### 3.2 macOS (Apple Silicon / Intel)

```bash
# Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# JDK 21
brew install --cask temurin@21

# Docker Desktop (ships with Compose v2)
brew install --cask docker
open -a Docker   # wait until the menu bar shows "Docker is running"

# Assorted helpers
brew install git make postgresql@16
```

### 3.3 Windows (WSL2, recommended)

Install **Docker Desktop** with the *WSL2 backend*, enable WSL integration for
your Ubuntu distro, then follow §3.1 **inside the WSL shell**. Do **not** use
MinGW/Cygwin — `schema-init.sh` and the Makefile assume a Linux environment.

---

## 4. Cloning the repository

```bash
mkdir -p ~/workspace && cd ~/workspace
git clone <bracket-remote-url> bracket
cd bracket

# Make sure you are on the main branch
git checkout master   # or main, depending on your remote
```

After the clone, the project folder contains:

```
bracket/
├── pom.xml
├── Makefile
├── mvnw, mvnw.cmd, .mvn/
├── .gitignore
├── .gitattributes
├── README.md
├── docs/HELP.md                 ← this file
├── .github/workflows/ci.yml
└── src/
    ├── main/
    │   ├── docker/
    │   │   ├── Dockerfile.jvm
    │   │   ├── Dockerfile.native
    │   │   ├── docker-compose.yaml   ← unified: database + database-test + app
    │   │   ├── .env                  ← credentials & ports (gitignored)
    │   │   └── schema/
    │   │       ├── script/schema-init.sh
    │   │       └── migrations/*.sql
    │   ├── java/com/awn/bracket/…
    │   └── resources/
    │       ├── application.yml          ← base (always loaded)
    │       ├── application-dev.yml      ← DEV profile overrides
    │       └── application-prod.yml     ← PROD profile overrides
    └── test/
        ├── java/com/awn/bracket/…
        └── resources/application.yml    ← TEST profile (self-contained)
```

> **Note:** `src/main/docker/.env` is listed in `.gitignore`. The project
> scaffold creates it automatically; if you cloned an empty repository and the
> file is missing, copy it from the sample `.env` in this repository or create
> it manually (see §6).

---

## 5. Verifying the toolchain

Run each command once before you continue. Anything marked ❌ means the tool
was not installed correctly.

```bash
java -version              # openjdk version "21.x"
./mvnw -v                  # Apache Maven 3.9.x
docker --version           # Docker version 24+
docker compose version     # Docker Compose version v2.x
make --version             # GNU Make 4.x
git --version              # git version 2.x
```

If `./mvnw` refuses to run (`Permission denied`):

```bash
chmod +x ./mvnw
```

---

## 6. Configuring the environment

Every runtime value is read from **`src/main/docker/.env`** (gitignored so
credentials never end up in the repository). The default content is:

```dotenv
COMPOSE_PROJECT_NAME=bracket

# Dev database (port 5432)
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=bracket
POSTGRES_USER=bracket
POSTGRES_PASSWORD=bracket

# Schema bootstrap
APP_SCHEMA=bracket
APP_EXTRA_SCHEMAS=
APP_MIGRATE_ON_START=true

# Application
HTTP_PORT=8080
APP_IMAGE=bracket:latest
QUARKUS_PROFILE=prod

# Pool
DB_POOL_MIN=5
DB_POOL_MAX=20

# Separate test database (port 5433)
POSTGRES_TEST_HOST=localhost
POSTGRES_TEST_PORT=5433
POSTGRES_TEST_DB=bracket_test
POSTGRES_TEST_USER=bracket
POSTGRES_TEST_PASSWORD=bracket
```

If `.env` does not exist yet, create it with the defaults:

```bash
cat > src/main/docker/.env <<'ENV'
COMPOSE_PROJECT_NAME=bracket
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=bracket
POSTGRES_USER=bracket
POSTGRES_PASSWORD=bracket
APP_SCHEMA=bracket
APP_EXTRA_SCHEMAS=
APP_MIGRATE_ON_START=true
HTTP_PORT=8080
APP_IMAGE=bracket:latest
QUARKUS_PROFILE=prod
DB_POOL_MIN=5
DB_POOL_MAX=20
POSTGRES_TEST_HOST=localhost
POSTGRES_TEST_PORT=5433
POSTGRES_TEST_DB=bracket_test
POSTGRES_TEST_USER=bracket
POSTGRES_TEST_PASSWORD=bracket
ENV
```

For **production**, do not rely on the local `.env` — set the same variables
through your host environment or a CI secret manager instead.

---

## 7. Understanding the folder layout

| Path | Contents | When to change |
|---|---|---|
| `src/main/resources/application.yml` | Quarkus **base** config (loaded for every stage) | Every time you add an extension |
| `src/main/resources/application-dev.yml` | DEV overrides (Flyway OFF, Hibernate `update`, SQL log ON) | When dev behaviour changes |
| `src/main/resources/application-prod.yml` | PROD overrides (Flyway ON, Hibernate `validate`, Swagger OFF) | When prod behaviour changes |
| `src/test/resources/application.yml` | TEST config, self-contained (points to `database-test` on 5433) | When test infra changes |
| `src/main/docker/schema/script/schema-init.sh` | PostgreSQL schema bootstrap (create `bracket`, set `search_path`, lock `public`, apply migrations) | Rarely; only when the schema strategy changes |
| `src/main/docker/schema/migrations/*.sql` | **Dev-stage migrations** — source of DDL while Flyway is OFF | Whenever you add a table/column for dev |
| `src/main/resources/db/migration/` | **Prod-stage migrations** (Flyway) | Every release that changes the production schema |
| `src/main/docker/.env` | Local credentials & ports | Rarely; gitignored |
| `Makefile` | Build & run orchestration | When you add a new profile |
| `.github/workflows/ci.yml` | Unit-test CI (pure Java, no Docker) | When you add a CI step |
| `src/main/docker/docker-compose.yaml` | Unified compose: `database` (dev), `database-test` (test), `app` (container) | Rarely; when the topology changes |

---

## 8. Running the database (PostgreSQL via Docker)

```bash
make docker-up
```

What this command does:

1. Starts the `bracket-db` container (image `postgres:16-alpine`) on host port
   `5432`.
2. Because the `bracket_database` volume is empty, Docker executes
   `/docker-entrypoint-initdb.d/schema-init.sh`, which:
   - creates the **`bracket`** schema and pins it as the app role's `search_path`,
   - prevents `public` from being used as the default (`REVOKE ALL ON SCHEMA public FROM PUBLIC`),
   - grants `CREATE ON DATABASE` so Hibernate is allowed to create additional schemas,
   - applies every `src/main/docker/schema/migrations/*.sql` file in order (`sort -V`).

Verify:

```bash
docker compose -f src/main/docker/docker-compose.yaml \
               --env-file src/main/docker/.env ps

# Inspect the database
docker exec -it bracket-db psql -U bracket -d bracket -c "\dn"
docker exec -it bracket-db psql -U bracket -d bracket -c "\dt bracket.*"
```

You should see:
- the `bracket` schema exists (and it is **not** `public` that acts as the default),
- the migration-produced tables (`schema_bootstrap`, …) are already there.

Other commands:

| Command | Effect |
|---|---|
| `make docker-down` | Stop the container — **data stays** in the volume |
| `make docker-refresh` | Wipe the volume + start fresh → schema-init.sh & migrations run again |
| `make docker-logs` | Tail the Postgres log |

---

## 9. DEV stage — hot reload

```bash
make dev
```

This is the same as `./mvnw quarkus:dev -Dquarkus.profile=dev`. What is active
in dev:

| Config | Dev value |
|---|---|
| Flyway | **OFF** (`quarkus.flyway.enabled: false`) |
| Hibernate generation | `update` (never drops what migrations produced) |
| Hibernate hint `hbm2ddl.create_namespaces` | `true` → schemas referenced by `@Table(schema="x")` are auto-created |
| SQL logging | ON (`sql: true`, `bind-params: true`) |
| Dev UI | <http://localhost:8080/q/dev> |
| Swagger UI | <http://localhost:8080/q/swagger-ui> |

Edit `.java` / `.yml`, save, and the application will restart within 1–3
seconds. Exit with `Ctrl+C`.

To change the dev table structure:

```bash
# 1. Write/edit a new SQL file under src/main/docker/schema/migrations/
#    (e.g. 001_create_users.sql)
# 2. Wipe + rebuild the dev DB
make docker-refresh
# 3. Start again
make dev
```

---

## 10. Default stage — packaged jar

```bash
make start
```

Execution order:
1. `./mvnw -DskipTests package` — builds `target/quarkus-app/quarkus-run.jar`.
2. Loads variables from `src/main/docker/.env`.
3. Runs `java -Dquarkus.profile=default -jar target/quarkus-app/quarkus-run.jar`
   in the background; the PID is written to `.app.pid`, output to `app.log`.

Companion commands:

```bash
make status         # show the active pid
make logs           # tail -f app.log
make stop           # SIGTERM (SIGKILL as fallback), then delete the pid file
make refresh        # stop + build + start in one command
```

What is active in the default stage: Flyway ON (`migrate-at-start=true`),
Hibernate `update` (safe for schema evolution), log level `INFO`.

---

## 11. PROD stage — packaged jar + Flyway

```bash
make start-prod
```

Differences from the default stage:

| Config | Prod value |
|---|---|
| Hibernate generation | `validate` — will **fail to start** if entities ≠ database |
| Flyway | ON, `clean-disabled: true` |
| SQL logging | OFF |
| Swagger UI | OFF unless `SWAGGER_UI_ENABLED=true` |
| JDBC URL default host | `database` (compose service name) — override `POSTGRES_HOST` when running on the host |

**Required**: make sure the production credentials are set **before** starting,
for example through a systemd unit or `export`:

```bash
export POSTGRES_HOST=prod-db.internal
export POSTGRES_DB=bracket
export POSTGRES_USER=bracket_app
export POSTGRES_PASSWORD='strong-secret'
export JWT_PRIVATE_KEY_FILE=file:/etc/bracket/privateKey.pem
make start-prod
```

Flyway migrations are read from `src/main/resources/db/migration/` — make sure
that directory is populated (`V1__init.sql`, `V2__xxx.sql`, …).

---

## 12. Dedicated test database & running unit tests

Bracket ships a **second, isolated PostgreSQL service** inside the same
compose file (`database-test`) so you can test queries against exactly the
same structure as the app, without ever polluting the dev DB.

| Parameter | Dev | Test |
|---|---|---|
| Compose service | `database` | `database-test` |
| Compose file | `docker-compose.yaml` (unified) | `docker-compose.yaml` (unified) |
| Project | `bracket` | `bracket` |
| Container | `bracket-db` | `bracket-test-db` |
| Host port | 5432 | **5433** |
| Database | `bracket` | `bracket_test` |
| Volume | `bracket_database` | `bracket_database-test` |
| Network | `bracket-net` | `bracket-net` |
| Script & migrations | identical (`schema/script/schema-init.sh` + `schema/migrations/*.sql`) |

Commands:

```bash
make docker-test-up        # start the test DB (first boot → schema-init runs)
make docker-test-refresh   # wipe test volume + re-run migrations from scratch
make docker-test-cli       # enter psql:  \dn, \dt, SELECT …
make test                  # ./mvnw -B -DskipITs clean test (%test profile active)
make docker-test-down      # stop the test DB, volume is preserved
```

The `src/test/resources/application.yml` file (activated automatically during
`@QuarkusTest`) already points to `bracket_test:5433`, so tests run without
ever starting the dev DB. The connection pool is tolerant
(`foreground-initial-size: 0`, `initialization-fail-timeout: -1`) so CI —
which has no Docker — can still boot the Quarkus app when the unit tests do
not touch the database.

---

## 13. Build & run as a Docker image

```bash
make docker-build     # mvnw package + docker build -f Dockerfile.jvm
make docker-start     # compose up: database + app (QUARKUS_PROFILE=prod)
make docker-stop      # stop the app container only
```

Built image: `bracket:latest`. The app container is named `bracket-app`,
listens on port `8080`, joins the `bracket-net` network, and waits for the
`database` service to become *healthy* before starting.

To run the image directly without compose:

```bash
docker run --rm \
  -e QUARKUS_PROFILE=prod \
  -e POSTGRES_HOST=host.docker.internal \
  -e POSTGRES_DB=bracket \
  -e POSTGRES_USER=bracket \
  -e POSTGRES_PASSWORD=bracket \
  -p 8080:8080 \
  bracket:latest
```

For a **native binary** (requires GraalVM or `quarkus.native.container-build`):

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
./target/bracket-1.0-SNAPSHOT-runner
```

---

## 14. Makefile target cheat-sheet

Run `make help` at any time to see this list.

```text
Project lifecycle
  build              mvnw -DskipTests package
  clean              mvnw clean + delete .app.pid / app.log
  dev                DEV stage, hot reload, Flyway OFF
  start              DEFAULT stage, profile=default (background)
  start-prod         PROD stage, profile=prod (background)
  stop               Stop the background app
  refresh            stop → build → start
  status             Show pid & log info
  logs               tail -f app.log

Docker – dev infra (main Postgres)
  docker-up          Start postgres + run schema-init.sh once
  docker-down        Stop, volume is preserved
  docker-refresh     Wipe volume + start fresh (re-run migrations)
  docker-logs        Tail the DB log

Docker – application
  docker-build       Build image bracket:latest
  docker-start       Up database + app (QUARKUS_PROFILE from .env)
  docker-stop        Stop the app container only

Docker – test DB (port 5433)
  docker-test-up        Start the test DB
  docker-test-down      Stop the test DB
  docker-test-refresh   Wipe test volume + re-run migrations
  docker-test-cli       psql into bracket_test

Testing
  test                mvnw -B -DskipITs clean test
```

---

## 15. Adding entities & new schemas

Say you add an `AuditLog` entity annotated with
`@Table(schema = "audit", name = "log")`.

1. Write the entity class under `src/main/java/com/awn/bracket/…`.
2. **Dev**: run `make dev`. Because `hbm2ddl.create_namespaces=true` is already
   set in `%dev` (`application.yml`), Hibernate will automatically emit
   `CREATE SCHEMA audit;` followed by `CREATE TABLE audit.log (…)`. Verify
   with:
   ```bash
   docker exec -it bracket-db psql -U bracket -d bracket -c "\dn"
   docker exec -it bracket-db psql -U bracket -d bracket -c "\dt audit.*"
   ```
3. If you want the `audit` schema to exist **before** the app starts (e.g. to
   add grants / tablespaces / etc.), register it in `.env`:
   ```
   APP_EXTRA_SCHEMAS=audit,billing
   ```
   then run `make docker-refresh`.
4. Add a `NNN_create_audit_log.sql` file to
   `src/main/docker/schema/migrations/` so the test DB receives the same result.
5. **Prod**: write the equivalent Flyway migration at
   `src/main/resources/db/migration/V{n}__create_audit_log.sql`. Without it,
   prod will fail to start because
   `%prod.hibernate-orm.database.generation=validate`.

---

## 16. Common troubleshooting

### `make: Permission denied` when starting dev

```bash
chmod +x mvnw
chmod +x src/main/docker/schema/script/schema-init.sh
```

### `Bind for 0.0.0.0:5432 failed: port is already allocated`

Another Postgres is already running on the host. Change the port in `.env`:

```dotenv
POSTGRES_PORT=54320
```

Then `make docker-down && make docker-up`. If the test DB conflicts, change
`POSTGRES_TEST_PORT` instead.

### `relation "xxx" does not exist` under %prod

Flyway does not yet have the relevant migration. Add a file at
`src/main/resources/db/migration/V{n}__….sql` and restart. If the migration
exists but the error persists, the target schema is likely wrong — check
`quarkus.flyway.schemas` (`bracket`).

### `schema "audit" does not exist` in dev

Usually caused by an entity using `@Table(schema="audit")` when the DB was
created **before** the entity existed. Quick fix: run `make docker-refresh` so
migrations + `hbm2ddl.create_namespaces` rerun from scratch, or add
`APP_EXTRA_SCHEMAS=audit` to `.env`.

### `public` schema still accepts new tables

Make sure these statements have already run in the DB:

```sql
ALTER ROLE bracket IN DATABASE bracket SET search_path = bracket;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO bracket;
```

Check with `\du` and `SHOW search_path;` in psql. If `search_path` is still
`"$user", public`, rerun `make docker-refresh`.

### App container crashes with `Connection refused database:5432`

Everything (dev DB, test DB, app) now lives in **one** compose file, so make
sure you start the app through compose (not `docker run`) — the app service
resolves `POSTGRES_HOST=database` via the shared `bracket-net` network:

```bash
make docker-start     # builds the image + `docker compose up -d app`
```

If you start the image manually with `docker run`, set `POSTGRES_HOST` to
`host.docker.internal` or the LAN IP so the container can reach the host's
Postgres.

### Tests fail because no DB is reachable

For ordinary unit tests, the `%test` profile is tolerant. For tests that
actually touch the database, start the test DB first:

```bash
make docker-test-up
make test
```

---

## 17. Reset & uninstall

**Reset every local dataset** (dev + test) without deleting any code:

```bash
make stop || true
make docker-down
make docker-test-down
# Remove the volumes explicitly
docker volume rm bracket_database bracket-test_database-test || true
```

**Stop the containers & delete the app images**:

```bash
docker compose -p bracket      down --rmdir --images
docker compose -p bracket-test down --rmdir --images
docker rmi bracket:latest
```

**Reset the project to a fresh state** (Maven + artefacts):

```bash
make clean
rm -rf target .mvn/wrapper/maven-wrapper.jar app.log .app.pid
```

You can then start over from §6.

---

## Further reading

- Quarkus documentation: <https://quarkus.io/guides/>
- Flyway guide: <https://quarkus.io/guides/flyway>
- Hibernate ORM with Panache guide: <https://quarkus.io/guides/hibernate-orm-panache>
- Dev Services vs docker-compose guide: <https://quarkus.io/guides/databases-dev-services>

If you improve the workflow described here, update this file directly
(`docs/HELP.md`) so the next person's onboarding benefits as well.
