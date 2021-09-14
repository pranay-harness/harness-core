-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

CREATE TABLE IF NOT EXISTS public.accounts (
    id text NOT NULL,
    name text,
    created_at bigint
);

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);

COMMIT;
