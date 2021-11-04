BEGIN;

CREATE TABLE IF NOT EXISTS cg_tags (
    id text NOT NULL,
    account_id text NOT NULL,
    app_id text,
    tag_key text,
    tag_value text,
    entity_type text,
    entity_id text,
    created_at bigint,
    Last_updated_at bigint,
    created_by TEXT,
    last_updated_by TEXT,

    PRIMARY KEY(id)
);

COMMIT;