package io.harness.pms.sdk.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.observer.AsyncInformObserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Builder
@Slf4j
public class AsyncOrchestrationEventHandlerProxy implements OrchestrationEventHandlerProxy, AsyncInformObserver {
  private static ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Getter @NonNull OrchestrationEventHandler eventHandler;

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }

  public void handleEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      try {
        log.info("Started executing async event for orchestrationEvent");
        eventHandler.handleEvent(event);
        log.info("Completed event ");
      } catch (Exception ex) {
        log.error("Orchestration Async Event failed with Exception", ex);
      }
    }
  }
}
