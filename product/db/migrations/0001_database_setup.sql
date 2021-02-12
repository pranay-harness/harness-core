-- enable uuid extension in postgresql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- create user roles
-- harnessti has read/write permission
DO $$BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles
    WHERE rolname = 'harnessti'
  ) THEN
--  get password from vault
    CREATE USER harnessti WITH PASSWORD '';
  END IF;
END$$

-- harnesstiread is read-only user
DO $$BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles
    WHERE rolname = 'harnesstiread'
  ) THEN
  --  get password from vault
    CREATE USER harnesstiread WITH PASSWORD '';
  END IF;
END$$


-- resource_monitor is read-only user with update privileges
DO $$BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles
    WHERE rolname = 'resource_monitor'
  ) THEN
  --  get password from vault
    CREATE USER resource_monitor WITH PASSWORD '';
  END IF;
END$$


-- grant usage on schema
GRANT USAGE ON SCHEMA public TO harnessti;
GRANT USAGE ON SCHEMA public TO harnesstiread;
GRANT USAGE ON SCHEMA public TO resource_monitor;


-- For multiple tables
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO harnessti;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO harnesstiread;
GRANT SELECT, UPDATE ON ALL TABLES IN SCHEMA public TO resource_monitor;

-- to grant access to the new table in the future automatically, alter default:
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE ON TABLES TO harnessti;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO harnesstiread;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, UPDATE ON TABLES TO resource_monitor;