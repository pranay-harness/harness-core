package io.harness.event.usagemetrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;

import java.util.Map;

@Singleton
public class EventsModuleHelper {
  @Inject Map<String, EventHandler> eventHandlerMap;
  @Inject @Named("GenericEventListener") EventListener eventListener;

  public void initialize() {
    HarnessMetricsRegistryHandler eventHandler =
        (HarnessMetricsRegistryHandler) eventHandlerMap.get(HarnessMetricsRegistryHandler.class.getSimpleName());
    eventHandler.registerEventsWithHarnessRegistry();
    eventHandler.registerWithEventListener(eventListener);
  }
}
