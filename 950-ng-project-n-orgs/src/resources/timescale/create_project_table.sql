-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

CREATE TABLE IF NOT EXISTS public.projects (
    id text NOT NULL,
    identifier text,
    name text,
    deleted boolean,
    org_identifier text,
    last_modified_at bigint,
    created_at bigint,
    account_identifier text
);
ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);

COMMIT;
