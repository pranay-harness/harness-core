BEGIN;

CREATE TABLE IF NOT EXISTS public.accounts (
    id text NOT NULL,
    name text,
    created_at bigint
);

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);

COMMIT;