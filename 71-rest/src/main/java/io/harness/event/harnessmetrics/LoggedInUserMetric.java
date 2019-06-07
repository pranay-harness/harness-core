package io.harness.event.harnessmetrics;

import com.google.common.collect.ImmutableList;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

import java.util.List;
import java.util.Map;

public class LoggedInUserMetric implements HarnessMetricsEvent {
  private static final List<String> loggedInUserMetricLabels =
      ImmutableList.of(EventConstants.ACCOUNT_ID, EventConstants.ACCOUNT_NAME);

  @Override
  public String[] getLabelNames() {
    return loggedInUserMetricLabels.toArray(new String[] {});
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
