package io.harness.cdng.executionplan;

import lombok.Getter;

public enum CDPlanCreatorType {
  CD_EXECUTION_PLAN_CREATOR("CD_EXECUTION_PLAN_CREATOR"),
  CD_STEP_PLAN_CREATOR("CD_STEP_PLAN_CREATOR"),
  ARTIFACT_FORK_PLAN_CREATOR("ARTIFACT_FORK_PLAN_CREATOR"),
  ARTIFACT_PLAN_CREATOR("ARTIFACT_PLAN_CREATOR"),
  MANIFEST_PLAN_CREATOR("MANIFEST_PLAN_CREATOR"),
  SERVICE_PLAN_CREATOR("SERVICE_PLAN_CREATOR"),
  INFRA_PLAN_CREATOR("INFRA_PLAN_CREATOR"),
  ROLLBACK_PLAN_CREATOR("ROLLBACK_PLAN_CREATOR"),
  STEP_GROUPS_ROLLBACK_PLAN_CREATOR("STEP_GROUPS_ROLLBACK_PLAN_CREATOR"),
  STEP_GROUP_ROLLBACK_PLAN_CREATOR("STEP_GROUP_ROLLBACK_PLAN_CREATOR"),
  EXECUTION_ROLLBACK_PLAN_CREATOR("EXECUTION_ROLLBACK_PLAN_CREATOR"),
  PARALLEL_STEP_GROUP_ROLLBACK_PLAN_CREATOR("PARALLEL_STEP_GROUP_ROLLBACK_PLAN_CREATOR");

  @Getter private final String name;

  CDPlanCreatorType(String name) {
    this.name = name;
  }
}
