-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;

CREATE TABLE IF NOT EXISTS cg_tags (
    id text NOT NULL,
    account_id text NOT NULL,
    app_id text,
    tag_key text,
    tag_value text,
    entity_type text,
    entity_id text,
    created_at bigint,
    Last_updated_at bigint,
    created_by TEXT,
    last_updated_by TEXT,

    PRIMARY KEY(id)
);

COMMIT;
