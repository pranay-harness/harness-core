package io.harness.execution.events;

import com.google.inject.Injector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.Subject;
import io.harness.registries.RegistrableEntity;
import lombok.NonNull;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationSubject implements RegistrableEntity {
  private final Injector injector;
  private final Subject<SyncOrchestrationEventHandlerProxy> syncSubject;
  private final Subject<AsyncOrchestrationEventHandlerProxy> asyncSubject;

  public OrchestrationSubject(Injector injector) {
    this.injector = injector;
    this.syncSubject = new Subject<>();
    this.asyncSubject = new Subject<>();
  }

  public void register(@NonNull Class<? extends OrchestrationEventHandler> eventHandler) {
    if (SyncOrchestrationEventHandler.class.isAssignableFrom(eventHandler)) {
      registerSyncHandler(
          SyncOrchestrationEventHandlerProxy.builder().injector(injector).eventHandlerClass(eventHandler).build());
    } else {
      registerAsyncHandler(
          AsyncOrchestrationEventHandlerProxy.builder().injector(injector).eventHandlerClass(eventHandler).build());
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
