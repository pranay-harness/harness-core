package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

import java.util.Map;

public class LoggedInUserMetric implements HarnessMetricsEvent {
  private static final String[] loggedInUserMetricLabels =
      new String[] {EventConstants.ACCOUNTID, EventConstants.ACCOUNTNAME};

  @Override
  public String[] getLabelNames() {
    return loggedInUserMetricLabels.clone();
  }

  @Override
  public Type getType() {
    return Type.GAUGE;
  }

  @Override
  public EventType getEventType() {
    return EventType.USERS_LOGGED_IN;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to track the number of users logged in per account ";
  }

  @Override
  public void handleEvent(HarnessMetricRegistry registry, Event event) {
    Map<String, String> properties = event.getEventData().getProperties();
    if (properties.get(EventConstants.USER_LOGGED_IN).equals(Boolean.TRUE.toString())) {
      registry.recordGaugeInc(getEventType().name(), getLabelValues(event.getEventData()));
    } else {
      registry.recordGaugeDec(getEventType().name(), getLabelValues(event.getEventData()));
    }
  }

  @Override
  public void registerMetrics(HarnessMetricRegistry registry) {
    registry.registerGaugeMetric(getEventType().name(), getLabelNames(), getMetricHelpDocument());
  }
}
