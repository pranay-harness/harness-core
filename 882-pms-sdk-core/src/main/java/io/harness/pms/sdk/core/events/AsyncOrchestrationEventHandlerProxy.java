package io.harness.pms.sdk.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.AsyncInformObserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@OwnedBy(HarnessTeam.CDC)
@Builder
public class AsyncOrchestrationEventHandlerProxy implements OrchestrationEventHandlerProxy, AsyncInformObserver {
  private static ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Getter @NonNull OrchestrationEventHandler eventHandler;

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }

  public void handleEvent(OrchestrationEvent event) {
    eventHandler.handleEvent(event);
  }
}
