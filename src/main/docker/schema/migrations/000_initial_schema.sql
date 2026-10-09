-- =============================================================================
-- Bracket – dev-stage migrations : 000_initial_schema.sql
-- -----------------------------------------------------------------------------
-- This file (and every other *.sql in this folder) is applied ONCE by
-- src/main/docker/schema/script/schema-init.sh when the Postgres data
-- volume is empty (i.e. after `make docker-refresh`).
--
-- Rules of the road:
--   * Do NOT qualify schema names in DDL. The init script pins
--     `SET search_path TO bracket` per-session, so unqualified CREATE TABLE
--     lands in the app schema, never in `public`.
--   * Keep one file per logical change so the diff stays readable.
--   * Prefix the filename so `sort -V` yields the correct execution order
--     (e.g. 000_, 001_, V1__, V2__…).
--   * Production uses Flyway (see application.yml %prod + classpath:db/migration);
--     keep the equivalent DDL there too.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Baseline table. Flyway uses `flyway_schema_history`; for dev we track
-- which files have been applied by their filename, and Postgres re-runs
-- the whole folder only when the volume is wiped, so a table that
-- documents the bootstrap itself is enough.
-- ---------------------------------------------------------------------------
CREATE SCHEMA bucket;

INSERT INTO schema_bootstrap (filename)
VALUES ('000_initial_schema.sql')
ON CONFLICT (filename) DO NOTHING;

