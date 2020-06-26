package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistryKey;

@OwnedBy(HarnessTeam.CDC)
public enum OrchestrationEventType implements RegistryKey {
  ORCHESTRATION_START("ORCHESTRATION_START"),
  ORCHESTRATION_END("ORCHESTRATION_END");

  String type;

  OrchestrationEventType(String type) {
    this.type = type;
  }

  @Override
  public String getType() {
    return type;
  }
}
