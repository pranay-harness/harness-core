package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public enum OrchestrationEventType {
  ORCHESTRATION_START("ORCHESTRATION_START"),
  ORCHESTRATION_END("ORCHESTRATION_END"),
  NODE_EXECUTION_STATUS_UPDATE("NODE_EXECUTION_STATUS_UPDATE"),
  INTERVENTION_WAIT_START("INTERVENTION_WAIT_START"),
  PLAN_EXECUTION_STATUS_UPDATE("PLAN_EXECUTION_STATUS_UPDATE");

  String type;

  OrchestrationEventType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
