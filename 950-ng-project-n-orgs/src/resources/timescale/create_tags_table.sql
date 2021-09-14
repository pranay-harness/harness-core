-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

CREATE TABLE IF NOT EXISTS public.tags_info (
    id text NOT NULL,
    parent_type text,
    tags text[]
);
ALTER TABLE ONLY public.tags_info
    ADD CONSTRAINT tags_info_pkey PRIMARY KEY (id);

COMMIT;
