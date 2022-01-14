package io.harness.gitsync.gitsyncerror;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(DX) public enum GitSyncErrorStatus { ACTIVE, DISCARDED, EXPIRED, RESOLVED, OVERRIDDEN }
