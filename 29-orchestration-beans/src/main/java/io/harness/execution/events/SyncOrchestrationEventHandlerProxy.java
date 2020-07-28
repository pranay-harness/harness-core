package io.harness.execution.events;

import com.google.inject.Injector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@OwnedBy(HarnessTeam.CDC)
@Builder
public class SyncOrchestrationEventHandlerProxy implements OrchestrationEventHandlerProxy {
  @NonNull Injector injector;
  @Getter @NonNull Class<? extends OrchestrationEventHandler> eventHandlerClass;

  public void handleEvent(OrchestrationEvent event) {
    OrchestrationEventHandler originalEventHandler = injector.getInstance(eventHandlerClass);
    originalEventHandler.handleEvent(event);
  }
}
