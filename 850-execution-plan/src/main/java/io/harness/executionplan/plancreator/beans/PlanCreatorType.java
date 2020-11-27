package io.harness.executionplan.plancreator.beans;

import lombok.Getter;

public enum PlanCreatorType {
  PIPELINE_PLAN_CREATOR("PIPELINE_PLAN_CREATOR"),
  STEP_PLAN_CREATOR("STEP_PLAN_CREATOR"),
  STAGE_PLAN_CREATOR("STAGE_PLAN_CREATOR"),
  STAGES_PLAN_CREATOR("STAGES_PLAN_CREATOR");
  @Getter private final String name;

  PlanCreatorType(String name) {
    this.name = name;
  }
}
