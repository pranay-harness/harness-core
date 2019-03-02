package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;
import io.prometheus.client.Histogram;

public class DeploymentDurationEvent implements HarnessMetricsEvent {
  private static final String[] deploymentDurationLabelNames =
      new String[] {EventConstants.ACCOUNT_ID, EventConstants.ACCOUNT_NAME, EventConstants.WORKFLOW_ID,
          EventConstants.WORKFLOW_NAME, EventConstants.APPLICATION_ID, EventConstants.APPLICATION_NAME};

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
    registry.recordHistogram(
        getEventType().name(), getLabelValues(event.getEventData()), event.getEventData().getValue());
  }

  @Override
  public void registerMetrics(HarnessMetricRegistry registry) {
    String name = getEventType().name();
    String doc = getMetricHelpDocument();
    Histogram.Builder builder = Histogram.build().name(name).help(doc);
    String[] labels = getLabelNames();
    if (labels != null) {
      builder.labelNames(labels);
    }
    builder.help(doc);
    /**
     * 30 seconds bucket with 120 buckets total -> until 1 hour
     */
    builder.linearBuckets(0, 30, 120);

    registry.registerHistogramMetric(getEventType().name(), builder);
  }
}
