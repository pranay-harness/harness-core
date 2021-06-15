package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX) public enum GitRefType { COMMIT, BRANCH, PULL_REQUEST, COMPARE_COMMITS }
