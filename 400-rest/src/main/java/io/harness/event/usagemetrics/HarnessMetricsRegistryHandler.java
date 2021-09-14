/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.usagemetrics;

import io.harness.event.handler.EventHandler;
import io.harness.event.harnessmetrics.CV247Metric;
import io.harness.event.harnessmetrics.HarnessMetricsEvent;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class HarnessMetricsRegistryHandler implements EventHandler {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  Map<EventType, HarnessMetricsEvent> eventTypeMap = new HashMap<>();

  public void registerEventsWithHarnessRegistry() {
    registerMetrics(new CV247Metric());
  }

  public void registerWithEventListener(EventListener eventListener) {
    eventListener.registerEventHandler(this, eventTypeMap.keySet());
  }

  private void registerMetrics(HarnessMetricsEvent harnessMetricsEvent) {
    harnessMetricsEvent.registerMetrics(harnessMetricRegistry);
    eventTypeMap.put(harnessMetricsEvent.getEventType(), harnessMetricsEvent);
  }

  @Override
  public void handleEvent(Event event) {
    eventTypeMap.get(event.getEventType()).handleEvent(harnessMetricRegistry, event);
  }
}
