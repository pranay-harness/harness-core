package io.harness.cdng.executionplan;

import lombok.Getter;

public enum CDPlanCreatorType {
  STAGE_PLAN_CREATOR("STAGE_PLAN_CREATOR"),
  EXECUTION_PHASES_PLAN_CREATOR("EXECUTION_PHASES_PLAN_CREATOR"),
  PHASE_PLAN_CREATOR("PHASE_PLAN_CREATOR"),
  STAGES_PLAN_CREATOR("STAGES_PLAN_CREATOR");

  @Getter private final String name;

  CDPlanCreatorType(String name) {
    this.name = name;
  }
}
