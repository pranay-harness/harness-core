/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
