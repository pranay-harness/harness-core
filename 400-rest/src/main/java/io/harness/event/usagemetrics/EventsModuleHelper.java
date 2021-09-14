/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.usagemetrics;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;

@OwnedBy(PL)
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
