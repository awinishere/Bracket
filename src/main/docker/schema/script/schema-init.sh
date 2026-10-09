#!/bin/bash
# =============================================================================
# Bracket – PostgreSQL schema bootstrap
# -----------------------------------------------------------------------------
# This file is mounted into `/docker-entrypoint-initdb.d` and runs once,
# the first time the `database` container starts with an empty data volume.
# The same script is executed by CI (`.github/workflows/ci.yml`) against a
# throw-away Postgres service, so dev box and CI always see identical schema.
#
# Folder layout (inside src/main/docker/schema/):
#   schema/
#   ├── script/       <- this shell script (auto-executed by docker)
#   │   └── schema-init.sh
#   └── migrations/   <- *.sql files applied by this script (dev-stage DDL)
#       ├── 000_initial_schema.sql
#       └── V1__whatever.sql   (any *.sql, executed in sort -V order)
#
# Responsibilities
#   1. Create the application-owned schema (`bracket` by default). This is
#      the schema every entity table lands in unless @Table(schema="…") is
#      set on the entity.
#   2. Force the app role's default `search_path` to that schema so that
#      `public` is *not* the implicit default and unqualified DDL from the
#      application never touches `public`.
#   3. Grant the app role full rights on `bracket` and CREATE on the
#      database, so Hibernate (with `hbm2ddl.create_namespaces=true` in
#      dev) can auto-create any additional schema declared on an entity and
#      have it immediately visible in Postgres.
#   4. Revoke ambient write access to `public` so nothing accidental lands
#      there.
#   5. DEV-STAGE MIGRATIONS: apply every `*.sql` file found in
#      `${MIGRATIONS_DIR}` (mounted at `/migrations`) in `sort -V` order.
#      This replaces Flyway in dev — Flyway is disabled at the Quarkus
#      level (%dev: flyway.enabled=false) and the .sql files are the
#      source of truth for the base schema/tables. In prod, Flyway owns
#      DDL (application.yml %prod + classpath:db/migration).
#
# Environment variables recognised:
#   POSTGRES_USER, POSTGRES_DB           – from docker-compose (required)
#   APP_SCHEMA          (default: bracket)         – the non-public schema
#   APP_EXTRA_SCHEMAS   (default: "")              – comma-separated extras
#   APP_MIGRATE_ON_START (default: true)           – set to false to skip
#                                                    the migrations folder
#   MIGRATIONS_DIR      (default: /migrations)     – where to look for .sql
#                                                    (CI can override)
# =============================================================================
set -Eeuo pipefail

APP_SCHEMA="${APP_SCHEMA:-bracket}"
APP_EXTRA_SCHEMAS="${APP_EXTRA_SCHEMAS:-}"
APP_MIGRATE_ON_START="${APP_MIGRATE_ON_START:-true}"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-/migrations}"

# If /migrations is not present (e.g. running from source in CI without the
# docker mount), fall back to the sibling `migrations/` folder next to the
# `script/` folder that contains this file.
if [[ ! -d "$MIGRATIONS_DIR" ]]; then
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    ALT_DIR="$(dirname "$SCRIPT_DIR")/migrations"
    if [[ -d "$ALT_DIR" ]]; then
        MIGRATIONS_DIR="$ALT_DIR"
    fi
fi

run_psql() {
    psql -v ON_ERROR_STOP=1 --no-psqlrc \
         --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "$1"
}

run_psql_file() {
    psql -v ON_ERROR_STOP=1 --no-psqlrc \
         --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$1"
}

echo "[bracket-init] bootstrapping schemas for database=$POSTGRES_DB user=$POSTGRES_USER"

# ---------------------------------------------------------------------------
# 1. application-owned schema (NOT public)
# ---------------------------------------------------------------------------
run_psql "CREATE SCHEMA IF NOT EXISTS ${APP_SCHEMA} AUTHORIZATION ${POSTGRES_USER};"
run_psql "ALTER SCHEMA ${APP_SCHEMA} OWNER TO ${POSTGRES_USER};"
run_psql "GRANT ALL ON SCHEMA ${APP_SCHEMA} TO ${POSTGRES_USER};"

# ---------------------------------------------------------------------------
# 2. optional extra schemas (fixed schema names declared by entities)
# ---------------------------------------------------------------------------
if [[ -n "$APP_EXTRA_SCHEMAS" ]]; then
    IFS=',' read -ra EXTRAS <<< "$APP_EXTRA_SCHEMAS"
    for s in "${EXTRAS[@]}"; do
        s_trim="$(echo -n "${s//[\'\"\ ]/}" | tr '[:upper:]' '[:lower:]')"
        [[ -z "$s_trim" ]] && continue
        echo "[bracket-init]   creating extra schema: $s_trim"
        run_psql "CREATE SCHEMA IF NOT EXISTS ${s_trim} AUTHORIZATION ${POSTGRES_USER};"
        run_psql "GRANT ALL ON SCHEMA ${s_trim} TO ${POSTGRES_USER};"
    done
fi

# ---------------------------------------------------------------------------
# 3. force app role's default search_path (public is NOT the default)
# ---------------------------------------------------------------------------
run_psql "ALTER ROLE ${POSTGRES_USER} IN DATABASE ${POSTGRES_DB} SET search_path = ${APP_SCHEMA};"

# ---------------------------------------------------------------------------
# 4. let the app user CREATE new schemas (needed by hbm2ddl in dev)
# ---------------------------------------------------------------------------
run_psql "GRANT CREATE ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_USER};"

# ---------------------------------------------------------------------------
# 5. lock down `public` so it is not used implicitly
# ---------------------------------------------------------------------------
run_psql "REVOKE ALL ON SCHEMA public FROM PUBLIC;"
run_psql "GRANT ALL ON SCHEMA public TO ${POSTGRES_USER};"

# ---------------------------------------------------------------------------
# 6. DEV-STAGE MIGRATIONS  –  apply migrations/*.sql into APP_SCHEMA
#    Flyway is disabled in %dev, so this is the sole source of the base DDL.
#    Each file is executed in its own psql session with `SET search_path`
#    pinned to APP_SCHEMA so unqualified CREATE TABLE / INDEX / etc. always
#    lands in `bracket` and never in `public`.
# ---------------------------------------------------------------------------
if [[ "$APP_MIGRATE_ON_START" == "true" ]]; then
    if [[ -d "$MIGRATIONS_DIR" ]]; then
        echo "[bracket-init] applying SQL migrations from ${MIGRATIONS_DIR}"
        applied=0
        # sort -V handles natural ordering so V2 comes before V10.
        while IFS= read -r sql; do
            [[ -z "$sql" ]] && continue
            echo "[bracket-init]   -> $(basename "$sql")"
            PGTZ=UTC psql -v ON_ERROR_STOP=1 --no-psqlrc \
                --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
                -c "SET search_path TO ${APP_SCHEMA};" \
                -f "$sql"
            applied=$((applied + 1))
        done < <(find "$MIGRATIONS_DIR" -maxdepth 1 -type f -name '*.sql' | sort -V)
        if [[ $applied -eq 0 ]]; then
            echo "[bracket-init]   (no .sql files found in ${MIGRATIONS_DIR} — nothing to apply)"
        else
            echo "[bracket-init] applied ${applied} migration file(s)."
        fi
    else
        echo "[bracket-init] migrations dir ${MIGRATIONS_DIR} not found — skipping SQL migrations"
    fi
else
    echo "[bracket-init] APP_MIGRATE_ON_START=${APP_MIGRATE_ON_START} — skipping SQL migrations"
fi

# --- Verification -----------------------------------------------------------
echo "[bracket-init] current schemas:"
run_psql "SELECT nspname FROM pg_namespace
          WHERE nspname NOT LIKE 'pg_%' AND nspname <> 'information_schema'
          ORDER BY nspname;"

echo "[bracket-init] tables in ${APP_SCHEMA}:"
run_psql "SELECT schemaname, tablename FROM pg_tables
          WHERE schemaname = '${APP_SCHEMA}' ORDER BY tablename;"

echo "[bracket-init] search_path for ${POSTGRES_USER} on ${POSTGRES_DB}:"
run_psql "SELECT setconfig FROM pg_db_role_setting s
          JOIN pg_roles r ON r.oid = s.setrole
          JOIN pg_database d ON d.oid = s.setdatabase
          WHERE r.rolname = '${POSTGRES_USER}' AND d.datname = '${POSTGRES_DB}';"

echo "[bracket-init] done."
