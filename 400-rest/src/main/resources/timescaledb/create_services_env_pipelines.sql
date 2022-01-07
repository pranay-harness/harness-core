-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- SERVICES TABLE START ------------
BEGIN;
 CREATE TABLE IF NOT EXISTS services (
     id text  NOT NULL,
     account_id text  NOT NULL,
     org_identifier text  NULL,
     project_identifier text  NULL,
     identifier text  NULL,
     name text  NULL,
     deleted boolean  NULL,
     created_at bigint  NULL,
     last_modified_at bigint  NULL);
COMMIT;

BEGIN;
CREATE INDEX IF NOT EXISTS services_account_id_created_at_idx ON services USING btree (account_id, created_at );
COMMIT;

---------- SERVICES END ------------

---------- ENVIRONMENTS TABLE START ------------
BEGIN;
 CREATE TABLE IF NOT EXISTS environments (
     id text  NOT NULL,
     account_id text  NOT NULL,
     org_identifier text  NULL,
     project_identifier text  NULL,
     identifier text  NULL,
     name text  NULL,
     deleted boolean  NULL,
     created_at bigint  NULL,
     last_modified_at bigint  NULL);
COMMIT;

BEGIN;
CREATE INDEX IF NOT EXISTS environments_account_id_created_at_idx ON environments USING btree (account_id, created_at );
COMMIT;

---------- ENVIRONMENTS END ------------

---------- PIPELINES TABLE START ------------
BEGIN;
 CREATE TABLE IF NOT EXISTS pipelines (
     id text  NOT NULL,
     account_id text  NOT NULL,
     org_identifier text  NULL,
     project_identifier text  NULL,
     identifier text  NULL,
     name text  NULL,
     deleted boolean  NULL,
     created_at bigint  NULL,
     last_updated_at bigint  NULL);
COMMIT;

BEGIN;
CREATE INDEX IF NOT EXISTS pipelines_account_id_created_at_idx ON pipelines USING btree (account_id, created_at );
COMMIT;

---------- PIPELINES END ------------
