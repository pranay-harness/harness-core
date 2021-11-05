BEGIN;

CREATE TABLE IF NOT EXISTS public.cg_services (
    id text NOT NULL,
    name text NOT NULL,
    artifact_type text NOT NULL,
    version bigint NOT NULL,
    account_id text NOT NULL,
    app_id text NOT NULL,
    artifact_stream_ids text[],
    created_at bigint,
    last_updated_at bigint,
    created_by text,
    last_updated_by text,
    deployment_type text,

    PRIMARY KEY(id)
);

COMMIT;