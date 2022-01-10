-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
