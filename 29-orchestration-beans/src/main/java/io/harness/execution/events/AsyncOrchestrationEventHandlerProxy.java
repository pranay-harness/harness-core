package io.harness.execution.events;

import com.google.inject.Injector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.AsyncInformObserver;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(HarnessTeam.CDC)
@Builder
public class AsyncOrchestrationEventHandlerProxy implements OrchestrationEventHandlerProxy, AsyncInformObserver {
  private static ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @NonNull Injector injector;
  @Getter @NonNull Class<? extends OrchestrationEventHandler> eventHandlerClass;

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }

  public void handleEvent(OrchestrationEvent event) {
    OrchestrationEventHandler originalEventHandler = injector.getInstance(eventHandlerClass);
    originalEventHandler.handleEvent(event);
  }
}
