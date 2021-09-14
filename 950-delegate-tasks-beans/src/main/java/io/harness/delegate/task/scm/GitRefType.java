/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum GitRefType {
  COMMIT,
  BRANCH,
  PULL_REQUEST_COMMITS,
  PULL_REQUEST_WITH_COMMITS,
  LATEST_COMMIT_ID,
  COMPARE_COMMITS,
}
