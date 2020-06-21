package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum RepairActionCode {
  MANUAL_INTERVENTION,
  ROLLBACK_WORKFLOW,
  ROLLBACK_PHASE,
  IGNORE,
  RETRY,
  END_EXECUTION,
  ABORT_WORKFLOW_EXECUTION;
}
