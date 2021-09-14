-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

DO
$do$
    BEGIN
        BEGIN
            ALTER TABLE selection ADD COLUMN repo TEXT;
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column repo already exists in selection.';
        END;
    END;
$do$;

DO
$do$
    BEGIN
        BEGIN
            ALTER TABLE selection ADD COLUMN source_branch TEXT;
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column source_branch already exists in selection.';
        END;
    END;
$do$;

DO
$do$
    BEGIN
        BEGIN
            ALTER TABLE selection ADD COLUMN target_branch TEXT;
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column target_branch already exists in selection.';
        END;
    END;
$do$;

comment on column selection.repo is 'git repository for selection';
comment on column selection.source_branch is 'source branch that selection is done on';
comment on column selection.target_branch is 'target branch for test selection';
