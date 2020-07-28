package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;

@OwnedBy(HarnessTeam.CDC)
public interface OrchestrationEventHandlerProxy extends RegistrableEntity {
  Class<? extends OrchestrationEventHandler> getEventHandlerClass();

  void handleEvent(OrchestrationEvent event);
}
