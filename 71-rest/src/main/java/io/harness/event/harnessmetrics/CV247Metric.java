package io.harness.event.harnessmetrics;

import com.google.common.collect.ImmutableList;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

import java.util.List;

/*
 * Created by Adam Hancock on 7/25/2019
 */
public class CV247Metric implements HarnessMetricsEvent {
  private static final List<String> staticCVMetricLabels = ImmutableList.of(
      EventConstants.ACCOUNT_ID, EventConstants.VERIFICATION_STATE_TYPE, EventConstants.IS_24X7_ENABLED);

  @Override
  public String[] getLabelNames() {
    return staticCVMetricLabels.toArray(new String[] {});
  }

  @Override
  public Type getType() {
    return Type.GAUGE;
  }

  @Override
  public void handleEvent(HarnessMetricRegistry registry, Event event) {
    registry.recordGaugeValue(
        getEventType().name(), getLabelValues(event.getEventData()), event.getEventData().getValue());
  }

  @Override
  public void registerMetrics(HarnessMetricRegistry registry) {
    registry.registerGaugeMetric(getEventType().name(), getLabelNames(), getMetricHelpDocument());
  }

  @Override
  public EventType getEventType() {
    return EventType.CV_META_DATA;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to get the 24x7 CV metric data for different accounts";
  }
}
