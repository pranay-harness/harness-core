package io.harness.event.harnessmetrics;

import com.google.common.collect.ImmutableList;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.Collector.Type;

import java.util.List;

public class DeploymentMetadataEvent implements HarnessMetricsEvent {
  private static final List<String> deploymentMetadataLabels =
      ImmutableList.of(EventConstants.ACCOUNT_ID, EventConstants.ACCOUNT_NAME, EventConstants.WORKFLOW_EXECUTION_STATUS,
          EventConstants.WORKFLOW_TYPE, EventConstants.WORKFLOW_ID, EventConstants.WORKFLOW_NAME,
          EventConstants.APPLICATION_ID, EventConstants.APPLICATION_NAME);

  @Override
  public String[] getLabelNames() {
    return deploymentMetadataLabels.toArray(new String[] {});
  }

  @Override
  public Type getType() {
    return Type.COUNTER;
  }

  @Override
  public EventType getEventType() {
    return EventType.DEPLOYMENT_METADATA;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to track the metadata associated with a deployment such as automatic/manual , execution status etc";
  }

  @Override
  public void handleEvent(HarnessMetricRegistry registry, Event event) {
    registry.recordCounterInc(getEventType().name(), getLabelValues(event.getEventData()));
  }

  @Override
  public void registerMetrics(HarnessMetricRegistry registry) {
    registry.registerCounterMetric(getEventType().name(), getLabelNames(), getMetricHelpDocument());
  }
}
