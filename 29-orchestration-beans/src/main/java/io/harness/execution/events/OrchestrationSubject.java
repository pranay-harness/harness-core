package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.Subject;

import java.util.Set;
import lombok.NonNull;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationSubject {
  private final Subject<SyncOrchestrationEventHandlerProxy> syncSubject;
  private final Subject<AsyncOrchestrationEventHandlerProxy> asyncSubject;

  public OrchestrationSubject() {
    this.syncSubject = new Subject<>();
    this.asyncSubject = new Subject<>();
  }

  public void register(@NonNull OrchestrationEventHandler eventHandler) {
    if (SyncOrchestrationEventHandler.class.isAssignableFrom(eventHandler.getClass())) {
      registerSyncHandler(SyncOrchestrationEventHandlerProxy.builder().eventHandler(eventHandler).build());
    } else {
      registerAsyncHandler(AsyncOrchestrationEventHandlerProxy.builder().eventHandler(eventHandler).build());
    }
  }

  public void registerAll(@NonNull Set<OrchestrationEventHandler> eventHandlers) {
    for (OrchestrationEventHandler eventHandler : eventHandlers) {
      register(eventHandler);
    }
  }

  public void registerSyncHandler(@NonNull SyncOrchestrationEventHandlerProxy eventHandlerProxy) {
    syncSubject.register(eventHandlerProxy);
  }

  public void registerAsyncHandler(@NonNull AsyncOrchestrationEventHandlerProxy eventHandlerProxy) {
    asyncSubject.register(eventHandlerProxy);
  }

  public void handleEventSync(OrchestrationEvent event) {
    syncSubject.fireInform(OrchestrationEventHandlerProxy::handleEvent, event);
  }

  public void handleEventAsync(OrchestrationEvent event) {
    asyncSubject.fireInform(OrchestrationEventHandlerProxy::handleEvent, event);
  }
}
