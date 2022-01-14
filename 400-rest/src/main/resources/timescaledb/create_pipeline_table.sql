BEGIN;

CREATE TABLE IF NOT EXISTS public.cg_pipelines (
    id text NOT NULL,
    account_id text NOT NULL,
    app_id text NOT NULL,
    name text,
    env_ids text[],
    workflow_ids text[],
    created_at bigint,
    last_updated_at bigint,
    created_by text,
    last_updated_by text,

    PRIMARY KEY(id)
);

COMMIT;