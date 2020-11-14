package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public interface OrchestrationEventHandlerProxy {
  void handleEvent(OrchestrationEvent event);
}
