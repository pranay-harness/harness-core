-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

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
