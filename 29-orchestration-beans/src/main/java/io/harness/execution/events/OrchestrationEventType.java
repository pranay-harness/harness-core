package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistryKey;

@OwnedBy(HarnessTeam.CDC)
public enum OrchestrationEventType implements RegistryKey {
  ORCHESTRATION_START("ORCHESTRATION_START"),
  ORCHESTRATION_END("ORCHESTRATION_END"),
  NODE_EXECUTION_STATUS_UPDATE("NODE_EXECUTION_STATUS_UPDATE"),
  INTERVENTION_WAIT_START("INTERVENTION_WAIT_START");

  String type;

  OrchestrationEventType(String type) {
    this.type = type;
  }

  @Override
  public String getType() {
    return type;
  }
}
