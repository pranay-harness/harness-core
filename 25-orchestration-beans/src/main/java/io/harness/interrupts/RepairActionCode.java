package io.harness.interrupts;

public enum RepairActionCode {
  MANUAL_INTERVENTION,
  ROLLBACK_WORKFLOW,
  ROLLBACK_PHASE,
  IGNORE,
  RETRY,
  END_EXECUTION,
  ABORT_WORKFLOW_EXECUTION;
}
