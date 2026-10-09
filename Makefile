# =============================================================================
# Bracket – developer & operator Makefile
# -----------------------------------------------------------------------------
# Run stages (Quarkus profiles):
#   make dev          -> stage DEV   : mvnw quarkus:dev, hot reload, Flyway OFF
#   make start        -> stage BIASA : packaged jar, Quarkus profile "default"
#   make start-prod   -> stage PROD  : packaged jar, Quarkus profile "prod"
#
# Docker lifecycle (all services live in ONE compose file: docker-compose.yaml):
#   make docker-up / docker-down / docker-refresh   -> `database` service
#   make docker-test-up / docker-test-down / ...    -> `database-test` service
#   make docker-build / docker-start / docker-stop  -> `app` service
#
# Project lifecycle:
#   make build / refresh / stop / status / logs / clean
# =============================================================================

SHELL             := /bin/bash
APP_NAME          := bracket
MVN               := ./mvnw
DOCKER_DIR        := src/main/docker
COMPOSE_FILE      := $(DOCKER_DIR)/docker-compose.yaml
ENV_FILE          := $(DOCKER_DIR)/.env
JAR               := target/quarkus-app/quarkus-run.jar
PID_FILE          := .app.pid
LOG_FILE          := app.log
INIT_SCRIPT       := $(DOCKER_DIR)/schema/script/schema-init.sh
MIGRATIONS_DIR    := $(DOCKER_DIR)/schema/migrations

# Compose project name (matches COMPOSE_PROJECT_NAME in .env). Used to
# compute the actual docker volume names so `make docker-refresh` only wipes
# the target service's volume, not the sibling's.
COMPOSE_PROJECT   := bracket

COMPOSE           := docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE)

# Volume names as Docker sees them (project-scoped, `<project>_<define-name>`).
VOL_DB            := $(COMPOSE_PROJECT)_database
VOL_TEST          := $(COMPOSE_PROJECT)_database-test

.DEFAULT_GOAL := help

# -----------------------------------------------------------------------------
# help
# -----------------------------------------------------------------------------
.PHONY: help
help:
	@echo ""
	@echo "Bracket – available targets"
	@echo "---------------------------"
	@echo "  help              Show this message"
	@echo "  dev               Run in DEV   stage (quarkus:dev, hot reload, Flyway OFF)"
	@echo "  start             Run in BIASA stage (packaged jar, profile=default)"
	@echo "  start-prod        Run in PROD  stage (packaged jar, profile=prod)"
	@echo "  stop              Stop the background app started by start / start-prod"
	@echo "  status            Show whether the app is running"
	@echo "  logs              Tail app.log"
	@echo "  build             Package the app (mvnw -DskipTests package)"
	@echo "  refresh           stop -> build -> start (default stage)"
	@echo "  clean             mvnw clean + remove local artefacts"
	@echo ""
	@echo "  docker-up         Start PostgreSQL 'database'   (schema + migrations run once)"
	@echo "  docker-down       Stop  PostgreSQL 'database'   (volume preserved)"
	@echo "  docker-refresh    Wipe  'database' volume + restart"
	@echo "  docker-logs       Tail  'database' log"
	@echo ""
	@echo "  docker-test-up        Start PostgreSQL 'database-test' (host port 5433)"
	@echo "  docker-test-down      Stop  'database-test'            (volume preserved)"
	@echo "  docker-test-refresh   Wipe  'database-test' volume + restart"
	@echo "  docker-test-cli       psql into 'database-test'"
	@echo ""
	@echo "  docker-build      Build the app Docker image ($(APP_NAME):latest)"
	@echo "  docker-start      Start 'app' container (waits for 'database' healthy)"
	@echo "  docker-stop       Stop  'app' container (leaves databases running)"
	@echo ""
	@echo "  test              Run java unit tests (./mvnw test, %test -> 'database-test')"
	@echo ""
	@echo "Dev-stage schema source: $(MIGRATIONS_DIR)/*.sql"
	@echo "  (applied by $(INIT_SCRIPT); Flyway is disabled in dev via application-dev.yml)"
	@echo ""

# -----------------------------------------------------------------------------
# Maven build
# -----------------------------------------------------------------------------
.PHONY: build
build:
	$(MVN) -DskipTests package

.PHONY: clean
clean:
	$(MVN) clean
	@rm -f $(PID_FILE) $(LOG_FILE)

# -----------------------------------------------------------------------------
# Stage: dev  (hot reload + Flyway OFF + hbm2ddl.create_namespaces=true)
# -----------------------------------------------------------------------------
.PHONY: dev
dev: ensure-init-perms
	@echo ">>> starting $(APP_NAME) in DEV stage (profile=dev, Flyway disabled)"
	$(MVN) quarkus:dev -Dquarkus.profile=dev

# -----------------------------------------------------------------------------
# Stage: biasa / default  (packaged jar, profile=default)
# -----------------------------------------------------------------------------
.PHONY: start
start: build
	@if [ -f $(PID_FILE) ] && kill -0 $$(cat $(PID_FILE)) 2>/dev/null; then \
		echo "app already running (pid=$$(cat $(PID_FILE))) — run 'make stop' first"; exit 1; \
	fi
	@echo ">>> starting $(APP_NAME) in BIASA stage (profile=default)"
	@set -a; [ -f $(ENV_FILE) ] && . $(ENV_FILE); set +a; \
		nohup java -Dquarkus.profile=default -jar $(JAR) >> $(LOG_FILE) 2>&1 & \
		echo $$! > $(PID_FILE)
	@sleep 2 && echo "pid=$$(cat $(PID_FILE)) log=$(LOG_FILE)"

# -----------------------------------------------------------------------------
# Stage: prod  (packaged jar, profile=prod)
# -----------------------------------------------------------------------------
.PHONY: start-prod
start-prod: build
	@if [ -f $(PID_FILE) ] && kill -0 $$(cat $(PID_FILE)) 2>/dev/null; then \
		echo "app already running (pid=$$(cat $(PID_FILE))) — run 'make stop' first"; exit 1; \
	fi
	@echo ">>> starting $(APP_NAME) in PROD stage (profile=prod)"
	@set -a; [ -f $(ENV_FILE) ] && . $(ENV_FILE); set +a; \
		nohup java -Dquarkus.profile=prod -jar $(JAR) >> $(LOG_FILE) 2>&1 & \
		echo $$! > $(PID_FILE)
	@sleep 2 && echo "pid=$$(cat $(PID_FILE)) log=$(LOG_FILE)"

# -----------------------------------------------------------------------------
# Stop the background app
# -----------------------------------------------------------------------------
.PHONY: stop
stop:
	@if [ -f $(PID_FILE) ] && kill -0 $$(cat $(PID_FILE)) 2>/dev/null; then \
		pid=$$(cat $(PID_FILE)); \
		echo ">>> stopping $(APP_NAME) (pid=$$pid)"; \
		kill $$pid; \
		for i in 1 2 3 4 5 6 7 8 9 10; do \
			kill -0 $$pid 2>/dev/null || break; \
			sleep 0.5; \
		done; \
		kill -9 $$pid 2>/dev/null || true; \
		rm -f $(PID_FILE); \
		echo "stopped"; \
	else \
		echo "no running $(APP_NAME) (missing or stale $(PID_FILE))"; \
		rm -f $(PID_FILE); \
	fi

# -----------------------------------------------------------------------------
# Full refresh: stop app -> rebuild -> start again in default stage
# -----------------------------------------------------------------------------
.PHONY: refresh
refresh: stop build start

# -----------------------------------------------------------------------------
# Status & logs
# -----------------------------------------------------------------------------
.PHONY: status
status:
	@if [ -f $(PID_FILE) ] && kill -0 $$(cat $(PID_FILE)) 2>/dev/null; then \
		echo "$(APP_NAME) is RUNNING (pid=$$(cat $(PID_FILE)))"; \
	else \
		echo "$(APP_NAME) is NOT running"; \
	fi

.PHONY: logs
logs:
	@tail -n 200 -f $(LOG_FILE)

# =============================================================================
# Docker – service 'database' (dev PostgreSQL, host port 5432)
# =============================================================================
.PHONY: ensure-init-perms
ensure-init-perms:
	@[ -x $(INIT_SCRIPT) ] || chmod +x $(INIT_SCRIPT)

.PHONY: docker-up
docker-up: ensure-init-perms
	@echo ">>> starting 'database' service"
	$(COMPOSE) up -d database

.PHONY: docker-down
docker-down:
	@echo ">>> stopping 'database' service (data volume preserved)"
	$(COMPOSE) stop database

.PHONY: docker-refresh
docker-refresh: ensure-init-perms
	@echo ">>> wiping 'database' volume and restarting (schema-init re-runs)"
	$(COMPOSE) stop database
	$(COMPOSE) rm -f database
	docker volume rm $(VOL_DB) 2>/dev/null || true
	$(COMPOSE) up -d database
	@echo ">>> waiting for 'database' to become healthy"
	@for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do \
		if $(COMPOSE) ps database | grep -q 'healthy'; then echo "database healthy"; break; fi; \
		sleep 2; \
	done
	@echo "--- schema bootstrap log ---"
	@$(COMPOSE) logs --tail 50 database

.PHONY: docker-logs
docker-logs:
	$(COMPOSE) logs -f database

# =============================================================================
# Docker – service 'database-test' (dedicated test PostgreSQL, host port 5433)
# Uses the SAME schema-init.sh + schema/migrations/*.sql as `database`, so
# queries tested against `bracket_test` behave exactly like in the app.
# =============================================================================
.PHONY: docker-test-up
docker-test-up: ensure-init-perms
	@echo ">>> starting 'database-test' service (host port $${POSTGRES_TEST_PORT:-5433})"
	$(COMPOSE) up -d database-test

.PHONY: docker-test-down
docker-test-down:
	@echo ">>> stopping 'database-test' service (test volume preserved)"
	$(COMPOSE) stop database-test

.PHONY: docker-test-refresh
docker-test-refresh: ensure-init-perms
	@echo ">>> wiping 'database-test' volume and restarting (schema + migrations re-run)"
	$(COMPOSE) stop database-test
	$(COMPOSE) rm -f database-test
	docker volume rm $(VOL_TEST) 2>/dev/null || true
	$(COMPOSE) up -d database-test
	@echo ">>> waiting for 'database-test' to accept connections"
	@for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do \
		$(COMPOSE) exec -T database-test pg_isready -U bracket >/dev/null 2>&1 && echo "test db ready" && break; \
		sleep 2; \
	done
	@echo "--- test schema bootstrap log ---"
	@$(COMPOSE) logs --tail 50 database-test

.PHONY: docker-test-cli
docker-test-cli:
	$(COMPOSE) exec database-test psql -U $${POSTGRES_TEST_USER:-bracket} -d $${POSTGRES_TEST_DB:-bracket_test}

# =============================================================================
# Docker – service 'app' (packaged jar container)
# =============================================================================
.PHONY: docker-build
docker-build: build
	@echo ">>> building docker image $(APP_NAME):latest"
	docker build -f $(DOCKER_DIR)/Dockerfile.jvm -t $(APP_NAME):latest .

.PHONY: docker-start
docker-start: docker-build
	@echo ">>> starting 'app' service (compose handles depends_on 'database' healthy)"
	$(COMPOSE) up -d app

.PHONY: docker-stop
docker-stop:
	@echo ">>> stopping 'app' service (databases stay up)"
	$(COMPOSE) stop app

# =============================================================================
# Java unit tests – surefire only (%test profile -> 'database-test' on 5433)
# =============================================================================
.PHONY: test
test:
	$(MVN) -B -DskipITs clean test
