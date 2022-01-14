BEGIN;

CREATE TABLE IF NOT EXISTS public.cg_applications (
    id text NOT NULL,
    name text NOT NULL,
    account_id text NOT NULL,
    created_at bigint,
    last_updated_at bigint,
    created_by text,
    last_updated_by text,
    triggered_by text,

    PRIMARY KEY(id)
);

COMMIT;