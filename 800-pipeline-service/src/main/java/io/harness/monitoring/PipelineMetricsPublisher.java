package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionMonitorService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineMetricsPublisher implements MetricsPublisher {
  @Inject PlanExecutionMonitorService planExecutionMonitorService;
  @Override
  public void recordMetrics() {
    planExecutionMonitorService.registerActiveExecutionMetrics();
  }
}
