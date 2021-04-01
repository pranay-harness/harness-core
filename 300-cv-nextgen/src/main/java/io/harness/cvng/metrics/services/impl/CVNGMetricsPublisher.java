package io.harness.cvng.metrics.services.impl;

import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;

public class CVNGMetricsPublisher implements MetricsPublisher {
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private OrchestrationService orchestrationService;

  @Override
  public void recordMetrics() {
    learningEngineTaskService.recordMetrics();
    orchestrationService.recordMetrics();
  }
}
