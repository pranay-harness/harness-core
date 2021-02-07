package io.harness.pms.sdk.core.adviser.rollback;

public enum RollbackNodeType {
  STAGE,
  STEP,
  STEP_GROUP,
  STEP_GROUP_COMBINED,
  BOTH_STEP_GROUP_STAGE,
  DIRECT_STAGE // This is used to identifier if step failed and there is no stepGroup as parent of failed step.
}
