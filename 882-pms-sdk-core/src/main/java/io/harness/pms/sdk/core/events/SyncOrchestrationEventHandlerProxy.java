package io.harness.pms.sdk.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@OwnedBy(HarnessTeam.CDC)
@Builder
public class SyncOrchestrationEventHandlerProxy implements OrchestrationEventHandlerProxy {
  @Getter @NonNull OrchestrationEventHandler eventHandler;

  public void handleEvent(OrchestrationEvent event) {
    eventHandler.handleEvent(event);
  }
}
