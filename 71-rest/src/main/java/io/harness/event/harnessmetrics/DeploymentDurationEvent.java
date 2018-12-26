package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

public class DeploymentDurationEvent implements HarnessMetricsEvent {
  private static final String[] deploymentDurationLabelNames =
      new String[] {EventConstants.ACCOUNTID, EventConstants.ACCOUNTNAME};

  @Override
  public String[] getLabelNames() {
    return deploymentDurationLabelNames.clone();
  }

  @Override
  public Type getType() {
    return Type.HISTOGRAM;
  }

  @Override
  public EventType getEventType() {
    return EventType.DEPLOYMENT_DURATION;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to track the deployment duration per account ";
  }

  @Override
  public void handleEvent(HarnessMetricRegistry registry, Event event) {
    registry.recordHistorgram(
        getEventType().name(), getLabelValues(event.getEventData()), event.getEventData().getValue());
  }

  @Override
  public void registerMetrics(HarnessMetricRegistry registry) {
    registry.registerHistogramMetric(getEventType().name(), getLabelNames(), getMetricHelpDocument());
  }
}
