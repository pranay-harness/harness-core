package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

/**
 * Created by Pranjal on 05/15/2019
 */
public class LicenseDataMetric implements HarnessMetricsEvent {
  private static final String[] staticLicenseMetricLabels =
      new String[] {EventConstants.ACCOUNT_ID, EventConstants.ACCOUNT_NAME, EventConstants.COMPANY_NAME,
          EventConstants.ACCOUNT_TYPE, EventConstants.ACCOUNT_STATUS, EventConstants.ACCOUNT_CREATED_AT};

  public String[] getLabelNames() {
    return staticLicenseMetricLabels.clone();
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
    return EventType.LICENSE_UNITS;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to get the license data metric for different accounts";
  }
}
