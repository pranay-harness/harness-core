package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

public class InstanceMetric implements HarnessMetricsEvent {
  private static final String[] staticMetricLabels =
      new String[] {EventConstants.ACCOUNT_ID, EventConstants.ACCOUNT_NAME, EventConstants.INSTANCE_COUNT_TYPE};

  public String[] getLabelNames() {
    return staticMetricLabels.clone();
  }

  public Type getType() {
    return Type.GAUGE;
  }

  public void handleEvent(HarnessMetricRegistry registry, Event event) {
    registry.recordGaugeValue(
        getEventType().name(), getLabelValues(event.getEventData()), event.getEventData().getValue());
  }

  public void registerMetrics(HarnessMetricRegistry registry) {
    registry.registerGaugeMetric(getEventType().name(), getLabelNames(), getMetricHelpDocument());
  }

  @Override
  public EventType getEventType() {
    return EventType.INSTANCE_COUNT;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to get the instance counts for different accounts";
  }
}
