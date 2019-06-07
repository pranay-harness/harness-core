package io.harness.event.harnessmetrics;

import com.google.common.collect.ImmutableList;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

import java.util.List;

public class SetupDataMetric implements HarnessMetricsEvent {
  private static final List<String> staticMetricLabels =
      ImmutableList.of(EventConstants.ACCOUNT_ID, EventConstants.ACCOUNT_NAME, EventConstants.SETUP_DATA_TYPE);

  public String[] getLabelNames() {
    return staticMetricLabels.toArray(new String[] {});
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
    return EventType.SETUP_DATA;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to get the setup data metric for different accounts";
  }
}
