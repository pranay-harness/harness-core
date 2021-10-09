package io.harness.gitsync.gitsyncerror.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL) public enum GitSyncErrorType { GIT_TO_HARNESS, CONNECTIVITY_ISSUE, FULL_SYNC }
