package io.harness.metrics.service.api;

public interface MetricService {
  void initializeMetrics();
  void recordMetric(String metricName, double value);
}
