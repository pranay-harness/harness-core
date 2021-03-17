package io.harness.pms.sdk.core.steps.io;

public enum RollbackStrategy {
  STAGE_ROLLBACK("StageRollback"),
  STEP_GROUP_ROLLBACK("StepGroupRollback");

  String yamlName;

  RollbackStrategy(String yamlName) {
    this.yamlName = yamlName;
  }
}
