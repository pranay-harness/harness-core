package io.harness.monitoring;

import io.harness.metrics.service.api.MetricsPublisher;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PipelineServiceMetricsPublisher implements MetricsPublisher {
  @Inject private GraphGenerationService graphGenerationService;
  @Override
  public void recordMetrics() {
    graphGenerationService.recordMetrics();
  }
}
