package io.harness.gitsync.common.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX) public enum GitToHarnessProcessingStepStatus { TO_DO, QUEUED, DONE, ERROR }
