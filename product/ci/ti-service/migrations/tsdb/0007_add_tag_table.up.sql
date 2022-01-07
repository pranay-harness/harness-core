-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

-- This table gives a snapshot of evaluation stats.

CREATE TABLE IF NOT EXISTS tag(
  created_at        TIMESTAMPTZ DEFAULT now() NOT NULL,
  last_updated_at   timestamp with time zone DEFAULT now() NOT NULL,
--  Identification of a test run
  account_id TEXT NOT NULL,
  org_id TEXT NOT NULL,
  project_id TEXT NOT NULL,
  pipeline_id TEXT NOT NULL,
  build_id TEXT NOT NULL,
  stage_id TEXT NOT NULL,
  step_id TEXT NOT NULL,
--  Test execution overview
  commit_link TEXT
);



comment on column tag.created_at is 'time when the test execution was run';
comment on column tag.last_updated_at is 'Time when this entry was last updated';
--  Identification of a test run
comment on column tag.account_id is 'Account ID';
comment on column tag.org_id is 'Organization ID';
comment on column tag.project_id is 'Project ID';
comment on column tag.pipeline_id is 'Pipeline ID';
comment on column tag.build_id is 'Build ID';
comment on column tag.stage_id is 'Stage ID';
comment on column tag.step_id is 'Step ID';
--  Test execution overview
comment on column tag.commit_link is 'Commit link of the PR';


-- distributed hypertable is supported only in 2.0 with multi-node. for TSDB 1.7, or 2.0 with single-node use create_hypertable
SELECT create_hypertable('tag', 'created_at');

CREATE INDEX IF NOT EXISTS tag_idx1 ON tag(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, created_at DESC);
