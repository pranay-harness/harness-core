package io.harness.event.usagemetrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.EventHandler;
import io.harness.event.harnessmetrics.DeploymentDurationEvent;
import io.harness.event.harnessmetrics.DeploymentMetadataEvent;
import io.harness.event.harnessmetrics.HarnessMetricsEvent;
import io.harness.event.harnessmetrics.LoggedInUserMetric;
import io.harness.event.harnessmetrics.SetupDataMetric;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class HarnessMetricsRegistryHandler implements EventHandler {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  Map<EventType, HarnessMetricsEvent> eventTypeMap = new HashMap<>();

  public void registerEventsWithHarnessRegistry() {
    registerMetrics(new DeploymentDurationEvent());
    registerMetrics(new DeploymentMetadataEvent());
    registerMetrics(new LoggedInUserMetric());
    registerMetrics(new SetupDataMetric());
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
