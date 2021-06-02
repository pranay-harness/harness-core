package io.harness.advisers.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.commons.RepairActionCode;

@OwnedBy(CDC)
public enum RollbackStrategy {
  STAGE_ROLLBACK("StageRollback"),
  STEP_GROUP_ROLLBACK("StepGroupRollback"),
  UNKNOWN("Unknown");

  String yamlName;

  RollbackStrategy(String yamlName) {
    this.yamlName = yamlName;
  }

  public static RollbackStrategy fromRepairActionCode(RepairActionCode repairActionCode) {
    for (RollbackStrategy value : RollbackStrategy.values()) {
      if (value.name().equals(repairActionCode.name())) {
        return value;
      }
    }
    return null;
  }

  public static RollbackStrategy fromYamlName(String yamlName) {
    for (RollbackStrategy value : RollbackStrategy.values()) {
      if (value.yamlName.equals(yamlName)) {
        return value;
      }
    }
    return null;
  }
}
