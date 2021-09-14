-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

-- enable uuid extension in postgresql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- create user roles
-- harnessti has read/write permission
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles  -- SELECT list can be empty for this
      WHERE  rolname = 'harnessti') THEN
      --  get password from vault
      CREATE ROLE harnessti LOGIN PASSWORD '';
   END IF;
END
$do$;

-- harnesstiread is read-only user
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles  -- SELECT list can be empty for this
      WHERE  rolname = 'harnesstiread') THEN
      --  get password from vault
      CREATE ROLE harnesstiread LOGIN PASSWORD '';
   END IF;
END
$do$;



-- resource_monitor is read-only user with update privileges
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles  -- SELECT list can be empty for this
      WHERE  rolname = 'resource_monitor') THEN
      --  get password from vault
      CREATE ROLE resource_monitor LOGIN PASSWORD '';
   END IF;
END
$do$;


-- grant usage on schema
GRANT USAGE ON SCHEMA public TO harnessti;
GRANT USAGE ON SCHEMA public TO harnesstiread;
GRANT USAGE ON SCHEMA public TO resource_monitor;


-- For multiple tables
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO harnessti;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO harnesstiread;
GRANT SELECT, UPDATE ON ALL TABLES IN SCHEMA public TO resource_monitor;

-- to grant access to the new table in the future automatically, alter default:
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO harnessti;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO harnesstiread;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, UPDATE ON TABLES TO resource_monitor;
