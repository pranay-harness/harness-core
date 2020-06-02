package io.harness.executionplan.constants;

import lombok.Getter;

public enum PlanCreatorType {
  PIPELINE_PLAN_CREATOR("PIPELINE_PLAN_CREATOR"),
  STEP_PLAN_CREATOR("STEP_PLAN_CREATOR");
  @Getter private final String name;

  PlanCreatorType(String name) {
    this.name = name;
  }
}
