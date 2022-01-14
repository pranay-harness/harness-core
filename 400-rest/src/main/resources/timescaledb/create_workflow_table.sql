BEGIN;

CREATE TABLE IF NOT EXISTS public.cg_workflows (
    id text NOT NULL,
    name text NOT NULL,
    account_id text NOT NULL,
    orchestration_workflow_type text,
    env_id text,
    app_id text NOT NULL,
    service_ids text[],
    deployment_type text[],
    created_at bigint,
    last_updated_at bigint,
    created_by text,
    last_updated_by text,

    PRIMARY KEY(id)
);

COMMIT;