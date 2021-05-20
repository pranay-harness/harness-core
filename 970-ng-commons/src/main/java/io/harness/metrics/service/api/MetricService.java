package io.harness.metrics.service.api;

import java.util.List;

public interface MetricService {
  void initializeMetrics();

  void initializeMetrics(List<MetricDefinitionInitializer> metricDefinitionInitializers);

  void recordMetric(String metricName, double value);
}
