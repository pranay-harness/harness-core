package io.harness.perpetualtask;

public enum PerpetualTaskState {
  TASK_UNASSIGNED,
  TASK_TO_REBALANCE,
  TASK_PAUSED,
  TASK_ASSIGNED,

  // Keep just for backward compatibility with the database.
  // Never use the logic is already changed to assume these are not in use.
  @Deprecated NO_DELEGATE_INSTALLED,
  @Deprecated NO_DELEGATE_AVAILABLE,
  @Deprecated NO_ELIGIBLE_DELEGATES,
  @Deprecated TASK_RUN_SUCCEEDED,
  @Deprecated TASK_RUN_FAILED

}
