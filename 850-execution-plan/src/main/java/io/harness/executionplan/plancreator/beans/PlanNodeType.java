package io.harness.executionplan.plancreator.beans;

public enum PlanNodeType {
  PIPELINE,
  EXECUTION,
  STAGES,
  STAGE,
  STEP,
  STEP_GROUP,
  EXECUTION_ROLLBACK,
  STEP_GROUPS_ROLLBACK,
  STEP_GROUP_ROLLBACK,
  PARALLEL_STEP_GROUP_ROLLBACK
}
