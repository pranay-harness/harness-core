package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineMetricsPublisher implements MetricsPublisher {
  @Inject PlanExecutionService planExecutionService;
  @Override
  public void recordMetrics() {
    planExecutionService.registerActiveExecutionMetrics();
  }
}
