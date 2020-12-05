package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;

@OwnedBy(HarnessTeam.CDC)
public interface OrchestrationEventHandler {
  void handleEvent(OrchestrationEvent event);
}
