-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;

CREATE TABLE IF NOT EXISTS public.organizations (
    id text NOT NULL,
    identifier text NOT NULL,
    name text,
    deleted boolean,
    harness_managed boolean,
    account_identifier text,
    last_modified_at bigint,
    created_at bigint
);
ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);

COMMIT;
