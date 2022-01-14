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