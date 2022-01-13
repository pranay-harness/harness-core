-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS services_pkey ON services USING btree (id);
COMMIT;

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS environments_pkey ON environments USING btree (id);
COMMIT;

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS pipelines_pkey ON pipelines USING btree (id);
COMMIT;
