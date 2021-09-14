/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
