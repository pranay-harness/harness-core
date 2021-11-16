BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS services_pkey ON services USING btree (id);
COMMIT;

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS environments_pkey ON environments USING btree (id);
COMMIT;

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS pipelines_pkey ON pipelines USING btree (id);
COMMIT;